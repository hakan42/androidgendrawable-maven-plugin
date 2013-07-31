package fr.avianey.modjo.androidgendrawable;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.svg.SVGDocument;

/**
 * Goal which generates drawable from Scalable Vector Graphics (SVG) files.
 * 
 * @goal gen
 */
// TODO : delete PNG with the same name as target PNG
// TODO : handle multiple output directories with no density classifier
public class Gen extends AbstractMojo {
        
    private static final Set<String> densityClassifiers = new HashSet<String>();
    // TODO : matcher les pattern Android
//    private static Pattern svgPattern = null;
    private static Pattern resPattern = null;
    static {
        StringBuilder sb = new StringBuilder("\\w+-");
        StringBuilder tb = new StringBuilder("drawable.*-");
        StringBuilder db = new StringBuilder("(");
        boolean first = true;
        for (Density density : Density.values()) {
            if (!first) {
                db.append("|");
            } else {
                first = false;
            }
            db.append(density.name());
            densityClassifiers.add(density.name().toLowerCase());
        }
        db.append(")");
        tb.append(db.toString());
        sb.append(db.toString());
        tb.append(".*");
        sb.append("\\.svg");
//        svgPattern = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
        resPattern = Pattern.compile(tb.toString(), Pattern.CASE_INSENSITIVE);
    }
    // TODO : matcher le pattern android
    private static final Pattern classifiersPattern = Pattern.compile("[^-]+((-[^-]+)+)", Pattern.CASE_INSENSITIVE);
    
    
    /**
     * Directory of the svg resources to generate drawable from.
     * 
     * @parameter
     * @required
     */
    private File from;
    
    /**
     * Location of the Android "./res/drawable(...)" directories :
     * - drawable
     * - drawable-hdpi
     * - drawable-ldpi
     * - drawable-mdpi
     * - drawable-xhdpi
     * 
     * @parameter default-value="${project.basedir}/res"
     */
    private File to;
    
    /**
     * Create a drawable-density directory when no directory exists for a targeted densities
     * 
     * @parameter default-value="false"
     */
    private boolean createMissingDirectories;
    
    /**
     * If set to false, will trascode to NODPI directories using the fallback density
     * 
     * @parameter default-value="true"
     */
    private boolean skipNoDpi;

    /**
     * Enumeration of desired target densities.
     * If no density specified, PNG are only generated to existing directories.
     * If at least one density is specified, PNG are only generated in matching directories.
     * 
     * @parameter 
     */
    private Set<String> targetedDensities;

    /**
     * Use alternatives names for PNG resources
     * Key = original svg name (without density prefix)
     * Value = target name
     * 
     * @parameter 
     */
    private Map<String, String> rename;

    /**
     * Density for drawable directories without density qualifier
     * 
     * @parameter default-value="mdpi"
     */
    private String fallbackDensity;
    
    /**
     * Density for drawable directories without density qualifier
     * 
     * @parameter default-value=""
     */
    private String highResIcon;
    
    public void execute() throws MojoExecutionException {
        
        // validating target densities specified in pom.xml
        // untargetted densities will be ignored 
        // except for the fallback density if specified
        final Set<Density> targetDensities_ = EnumSet.noneOf(Density.class);
        for (String density : targetedDensities) {
            try {
                targetDensities_.add(Density.valueOf(density.toUpperCase()));
            } catch (Exception e) {
                throw new MojoExecutionException("Invalid target density : " + density, e);
            }
        }
        Density fd_ = Density.MDPI;
        try {
            fd_ = Density.valueOf(fallbackDensity.toUpperCase());
        } catch (Exception e) {
            throw new MojoExecutionException("Invalid fallback density : " + fallbackDensity, e);
        }
        final Density fallbackDensity_ = fd_;
        targetDensities_.add(fallbackDensity_);
        
        /*********************************************
         * List existing output drawable directories *
         *********************************************/
        final Map<Density, List<Input>> destinations = new EnumMap<Density, List<Input>>(Density.class);
        for (Density d : targetDensities_) {
            destinations.put(d, new ArrayList<Input>());
        }
        final Set<Density> foundDensities = EnumSet.noneOf(Density.class);
        if (to.isDirectory()) {
            for (File f : to.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    String name = FilenameUtils.getName(file.getPath());
                    if (file.isDirectory() && name.startsWith("drawable")) {
                        if (skipNoDpi && name.toLowerCase().matches("drawable.*-nodpi(-.+){0,1}")) {
                            // skip noDpiDirectory
                            return false;
                        }
                        Matcher m1 = classifiersPattern.matcher(name.toLowerCase());
                        if (m1.matches()) {
                            // extract classifiers list
                            final Set<String> classifiers = new HashSet<String>();
                            for (String classifier : m1.group(1).substring(1).toLowerCase().split("-", -1)) {
                                classifiers.add(classifier);
                            }
                            classifiers.removeAll(densityClassifiers);
                            // catalog output
                            Matcher m2 = resPattern.matcher(name.toLowerCase());
                            if (m2.matches()) {
                                // density classified directory
                                try {
                                    Density density = Density.valueOf(m2.group(1).toUpperCase());
                                    if (targetDensities_.isEmpty() 
                                            || targetDensities_.contains(density)) {
                                        destinations.get(density).add(new Input(file, density, classifiers));
                                        foundDensities.add(density);
                                    }
                                    return true;
                                } catch (IOException e) {
                                    getLog().error(e);
                                }
                            } else {
                                // drawable resources directory with no density qualifier
                                try {
                                    destinations.get(fallbackDensity_).add(new Input(file, fallbackDensity_, classifiers));
                                    foundDensities.add(fallbackDensity_);
                                } catch (IOException e) {
                                    getLog().error(e);
                                }
                            }
                        }
                    }
                    return false;
                }
            })) {
                getLog().debug("found output destination : " + f.getAbsolutePath());
            }
        } else {
            throw new MojoExecutionException(to.getAbsolutePath() + " is not a valid output directory");
        }
        
        /*****************************
         * List input svg to convert *
         *****************************/
        final List<Input> svgToConvert = new ArrayList<Input>();
        if (from.isDirectory()) {
            for (File f : from.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    if (file.isFile()) {
                        String name = FilenameUtils.getName(file.getPath());
                        Matcher m1 = classifiersPattern.matcher(name.toLowerCase().replace(".svg", ""));
                        if (m1.matches()) {
                            // extract classifiers list
                            final Set<String> classifiers = new HashSet<String>();
                            for (String classifier : m1.group(1).substring(1).toLowerCase().split("-", -1)) {
                                classifiers.add(classifier);
                            }
                            Set<String> classifiers_ = new HashSet<String>(classifiers);
                            if (classifiers.removeAll(densityClassifiers)) {
                                classifiers_.retainAll(densityClassifiers);
                                if (classifiers_.size() == 1) {
                                    try {
                                        svgToConvert.add(new Input(file, Density.valueOf(new ArrayList<String>(classifiers_).get(0).toUpperCase()), classifiers));
                                        return true;
                                    } catch (IOException e) {
                                        getLog().error(e);
                                    }
                                }
                            }
                        }
                    }
                    getLog().warn("Invalid svg input : " + file.getAbsolutePath());
                    return false;
                }
            })) {
                // log matching svg inputs
                getLog().debug("found svg file to convert : " + f.getAbsolutePath());
            }
        } else {
            throw new MojoExecutionException(from.getAbsolutePath() + " is not a valid input directory");
        }

        Input _highResIcon = null;
        Rectangle2D _highResIconBounds = null;
        
        /*********************************
         * Create svg in res/* folder(s) *
         *********************************/
        for (Input svg : svgToConvert) {
            try {
                Rectangle2D bounds = extractSVGBounds(svg);
                if (highResIcon != null && highResIcon.equals(svg.targetName)) {
                    _highResIcon = svg;
                    _highResIconBounds = bounds;
                }
                // for each target density :
                // - find matching destinations :
                //   - matches all extra classifiers
                //   - no other output with a classifiers set that is a subset of this output
                // - if no match, create required directories
                for (Density d : targetDensities_) {
                    final Collection<Input> filteredDestinations = filterDestinations(destinations.get(d), svg.classifiers, createMissingDirectories);
                    if (filteredDestinations.isEmpty()) {
                        // no matching directory - creating one
                        final String dirName = getInputClassifierDir(d, fallbackDensity_, svg.classifiers);
                        getLog().info("Creating required directory " + dirName);
                        File dir = new File(to, dirName);
                        dir.mkdir();
                        Input in = new Input(dir, d, svg.classifiers);
                        filteredDestinations.add(in);
                        destinations.get(d).add(in);
                    }
                    for (Input destination : filteredDestinations) {
                        getLog().info("Transcoding " + svg.getName() + " to " + destination.getName());
                        transcode(svg, bounds, destination);
                    }
                }
            } catch (MalformedURLException e) {
                getLog().error(e);
            } catch (IOException e) {
                getLog().error(e);
            } catch (TranscoderException e) {
                getLog().error(e);
            }
        }
        
        /******************************************
         * Generates the play store high res icon *
         ******************************************/
        if (_highResIcon != null) {
            try {
                _highResIcon.targetName = "highResIcon";
                // TODO : add a garbage density (NO_DENSITY)
                transcode(_highResIcon, _highResIconBounds, new Input(new File("."), Density.MDPI), 512, 512);
            } catch (IOException e) {
                getLog().error(e);
            } catch (TranscoderException e) {
                getLog().error(e);
            }
        }
    }
    
    /**
     * Return the shortest drawable directory name matching the input classifiers
     * @param d
     * @param classifiers
     * @return
     */
    private String getInputClassifierDir(Density d, Density fallback, Collection<String> classifiers) {
        final StringBuilder sb = new StringBuilder("drawable");
        if (!d.equals(fallback)) {
            sb.append("-");
            sb.append(d.name().toLowerCase());
        }
        for (String classifier : classifiers) {
            sb.append("-");
            sb.append(classifier);
        }
        return sb.toString();
    }

    /**
     * Filters directory with the svg constraints
     * @param list
     *          existing directories
     * @param classifiers
     *          classifiers targeted by the svg resource
     * @param createMissingDirectories
     *          create a directory if no matching directory found
     * @return
     */
    private Collection<Input> filterDestinations(final List<Input> directories, final Set<String> classifiers, final boolean createMissingDirectories) {
        Collection<Input> filteredDirectories = new ArrayList<Input>();
        for (Input in : directories) {
            // input match requirements
            if (in.classifiers.containsAll(classifiers)) {
                // verify that no other matching input
                // already covers current input requirements
                boolean retain = true;
                for (Input filtered : filteredDirectories) {
                    if (filtered.classifiers.containsAll(in.classifiers)) {
                        // filtered contains current
                        // retain current and skip filtered
                        filteredDirectories.remove(filtered);
                        break;
                    } else if (in.classifiers.containsAll(filtered.classifiers)) {
                        // current contains filtered
                        // skip current and retain filtered
                        retain = false;
                        break;
                    } else {
                        // disjunction
                        // retain both
                    }
                }
                if (retain) {
                    filteredDirectories.add(in);
                }
            }
        }
        return filteredDirectories;
    }

    /**
     * Extract the viewbox of the input SVG
     * @param svg
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    private Rectangle2D extractSVGBounds(Input svg) throws MalformedURLException, IOException {
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
        SVGDocument doc = (SVGDocument) f.createDocument(svg.toURI().toURL().toString());
        UserAgent userAgent = new UserAgentAdapter();
        DocumentLoader loader = new DocumentLoader(userAgent);
        BridgeContext ctx = new BridgeContext(userAgent, loader);
        ctx.setDynamicState(BridgeContext.DYNAMIC);
        GVTBuilder builder = new GVTBuilder();
        GraphicsNode rootGN = builder.build(ctx, doc);
        return rootGN.getGeometryBounds();
    }
    
    /**
     * Given it's bounds, transcodes a svg file to a PNG for the desired density
     * @param svg
     * @param bounds
     * @param dest
     * @throws IOException
     * @throws TranscoderException
     */
    private void transcode(Input svg, Rectangle2D bounds, Input dest) throws IOException, TranscoderException {
        transcode(svg, bounds, dest, 
                new Float(bounds.getWidth() * svg.density.ratio(dest.density)), 
                new Float(bounds.getHeight() * svg.density.ratio(dest.density)));
    }
    
    /**
     * Given a desired width and height, transcodes a svg file to a PNG for the desired density
     * @param svg
     * @param bounds
     * @param dest
     * @param targetWidth
     * @param targetHeight
     * @throws IOException
     * @throws TranscoderException
     */
    // TODO : center inside option
    // TODO : preserve aspect ratio
    private void transcode(Input svg, Rectangle2D bounds, Input dest, float targetWidth, float targetHeight) throws IOException, TranscoderException {
        PNGTranscoder t = new PNGTranscoder();
        t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(targetWidth));
        t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, new Float(targetHeight));
        TranscoderInput input = new TranscoderInput(svg.toURI().toURL().toString());
        String outputName = svg.targetName;
        if (rename.containsKey(outputName)) {
            if (rename.get(outputName) != null && rename.get(outputName).matches("\\w+")) {
                outputName = rename.get(outputName);
            } else {
                getLog().warn(rename.get(outputName) + " is not a valid replacment name for " + outputName);
            }
        }
        OutputStream ostream = new FileOutputStream(dest.getAbsolutePath() + System.getProperty("file.separator") + outputName + ".png");
        TranscoderOutput output = new TranscoderOutput(ostream);
        t.transcode(input, output);
        ostream.flush();
        ostream.close();
    }

}
