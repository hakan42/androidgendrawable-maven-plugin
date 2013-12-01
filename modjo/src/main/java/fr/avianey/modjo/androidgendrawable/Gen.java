package fr.avianey.modjo.androidgendrawable;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

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

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import fr.avianey.modjo.androidgendrawable.NinePatch.Zone;

/**
 * Goal which generates drawable from Scalable Vector Graphics (SVG) files.
 * 
 * @goal gen
 */
// TODO : delete PNG with the same name as target PNG
// TODO : JPEG or PNG
// TODO : 9-Patch
// TODO : handle multiple output directories with no density qualifier
// TODO : ordered qualifiers (http://developer.android.com/guide/topics/resources/providing-resources.html#QualifierRules)
public class Gen extends AbstractMojo {
        
    private static final Set<String> densityQualifiers = new HashSet<String>();
    // TODO : matcher les pattern Android
    private static Pattern resPattern = null;
    static {
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
            densityQualifiers.add(density.name().toLowerCase());
        }
        db.append(")");
        tb.append(db.toString());
        tb.append(".*");
        resPattern = Pattern.compile(tb.toString(), Pattern.CASE_INSENSITIVE);
    }
    // TODO : matcher le pattern android
    private static final Pattern qualifiersPattern = Pattern.compile("[^-]+((-[^-]+)+)", Pattern.CASE_INSENSITIVE);
    
    
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
     * - drawable-xxhdpi
     * 
     * @parameter default-value="${project.basedir}/res"
     */
    private File to;
    
    /**
     * Create a drawable-density directory when no directory exists for the given qualifiers.
     * If set to false, the plugin will generate the drawable in the best matching directory :
     * <ul>
     * <li>match all of the qualifiers</li>
     * <li>no other matching directory with less qualifiers</li>
     * </ul>
     * 
     * @parameter default-value="true"
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
    private Set<Density> targetedDensities;

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
    private Density fallbackDensity;
    
    /**
     * Name of the input file to use to generate a 512x512 high resolution Google Play icon
     * 
     * @parameter default-value=""
     */
    private String highResIcon;
    
    /**
     * Path to the 9-patch drawable configuration file.
     * 
     * @parameter
     * @parameter default-value=null
     */
    private String ninePatchConfig;
    
    @SuppressWarnings("unchecked")
    public void execute() throws MojoExecutionException {
        
        // validating target densities specified in pom.xml
        // untargetted densities will be ignored 
        // except for the fallback density if specified
        final Set<Density> _targetDensities = targetedDensities;
        final Density _fallbackDensity = fallbackDensity;
        _targetDensities.add(_fallbackDensity);
        getLog().debug("Fallback density set to : " + fallbackDensity.toString());
        
        /********************************
         * Load NinePatch configuration *
         ********************************/
        
        Map<String, NinePatch> ninePatchMap = new HashMap<>();
        if (ninePatchConfig != null) {
            try (final Reader reader = new FileReader(ninePatchConfig)) {
                Type t = new TypeToken<Map<String, NinePatch>>(){}.getType();
                ninePatchMap.putAll((Map<String, NinePatch>) (new GsonBuilder().create().fromJson(reader, t)));
            } catch (IOException e) {
                getLog().error(e);
            }
        }
        
        /*********************************************
         * List existing output drawable directories *
         *********************************************/
        final Map<Density, List<QualifiedResource>> destinations = new EnumMap<Density, List<QualifiedResource>>(Density.class);
        for (Density d : _targetDensities) {
            destinations.put(d, new ArrayList<QualifiedResource>());
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
                        Matcher m1 = qualifiersPattern.matcher(name.toLowerCase());
                        if (m1.matches()) {
                            // extract qualifiers list
                            final Set<String> qualifiers = new HashSet<String>();
                            for (String qualifier : m1.group(1).substring(1).toLowerCase().split("-", -1)) {
                                qualifiers.add(qualifier);
                            }
                            qualifiers.removeAll(densityQualifiers);
                            // catalog output
                            Matcher m2 = resPattern.matcher(name.toLowerCase());
                            if (m2.matches()) {
                                // density classified directory
                                try {
                                    Density density = Density.valueOf(m2.group(1).toLowerCase());
                                    if (_targetDensities.isEmpty() 
                                            || _targetDensities.contains(density)) {
                                        destinations.get(density).add(new QualifiedResource(file, density, qualifiers));
                                        foundDensities.add(density);
                                    }
                                    return true;
                                } catch (IOException e) {
                                    getLog().error(e);
                                }
                            } else {
                                // drawable resources directory with no density qualifier
                                try {
                                    destinations.get(_fallbackDensity).add(new QualifiedResource(file, _fallbackDensity, qualifiers));
                                    foundDensities.add(_fallbackDensity);
                                } catch (IOException e) {
                                    getLog().error(e);
                                }
                            }
                        }
                    }
                    return false;
                }
            })) {
                getLog().debug("Found output destination : " + f.getAbsolutePath());
            }
        } else {
            throw new MojoExecutionException(to.getAbsolutePath() + " is not a valid output directory");
        }
        
        /*****************************
         * List input svg to convert *
         *****************************/
        final List<QualifiedResource> svgToConvert = new ArrayList<QualifiedResource>();
        if (from.isDirectory()) {
            for (File f : from.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    if (file.isFile()) {
                        String name = FilenameUtils.getName(file.getPath());
                        Matcher m1 = qualifiersPattern.matcher(name.toLowerCase().replace(".svg", ""));
                        if (m1.matches()) {
                            // extract qualifiers list
                            final Set<String> qualifiers = new HashSet<String>();
                            for (String qualifier : m1.group(1).substring(1).toLowerCase().split("-", -1)) {
                                qualifiers.add(qualifier);
                            }
                            Set<String> _qualifiers = new HashSet<String>(qualifiers);
                            if (qualifiers.removeAll(densityQualifiers)) {
                                _qualifiers.retainAll(densityQualifiers);
                                if (_qualifiers.size() == 1) {
                                    try {
                                        svgToConvert.add(new QualifiedResource(file, Density.valueOf(new ArrayList<String>(_qualifiers).get(0).toLowerCase()), qualifiers));
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
                getLog().debug("Found svg file to convert : " + f.getAbsolutePath());
            }
        } else {
            throw new MojoExecutionException(from.getAbsolutePath() + " is not a valid input directory");
        }

        QualifiedResource _highResIcon = null;
        Rectangle2D _highResIconBounds = null;
        
        /*********************************
         * Create svg in res/* folder(s) *
         *********************************/
        for (QualifiedResource svg : svgToConvert) {
            try {
                Rectangle2D bounds = extractSVGBounds(svg);
                if (highResIcon != null && highResIcon.equals(svg.targetName)) {
                    _highResIcon = svg;
                    _highResIconBounds = bounds;
                }
                getLog().debug("Generating drawable from " + svg.getName());
                // for each target density :
                // - find matching destinations :
                //   - matches all extra qualifiers
                //   - no other output with a qualifiers set that is a subset of this output
                // - if no match, create required directories
                for (Density d : _targetDensities) {
                    getLog().debug("Generating drawable for target density " + d.toString());
                    final Collection<QualifiedResource> filteredDestinations = filterDestinations(destinations.get(d), svg.qualifiers, createMissingDirectories);
                    if (filteredDestinations.isEmpty() && createMissingDirectories) {
                        // no matching directory - creating one
                        final String dirName = getInputClassifierDir(d, _fallbackDensity, svg.qualifiers);
                        getLog().debug("No matching directory found for qualifiers " + svg.qualifiers.toString() + " ... creating " + dirName);
                        File dir = new File(to, dirName);
                        dir.mkdir();
                        QualifiedResource in = new QualifiedResource(dir, d, svg.qualifiers);
                        filteredDestinations.add(in);
                        destinations.get(d).add(in);
                    } else if (filteredDestinations.isEmpty()) {
                        getLog().debug("No matching directory found for qualifiers " + svg.qualifiers.toString() + " ... set createMissingDirectores to true to generate it automatically");
                    }
                    for (QualifiedResource destination : filteredDestinations) {
                        getLog().info("Transcoding " + svg.getName() + " to " + destination.getName());
                        transcode(svg, bounds, destination, ninePatchMap.get(svg.targetName));
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
                // TODO : add a garbage density (NO_DENSITY) for the highResIcon
                transcode(_highResIcon, _highResIconBounds, new QualifiedResource(new File("."), Density.mdpi), 512, 512, null);
            } catch (IOException e) {
                getLog().error(e);
            } catch (TranscoderException e) {
                getLog().error(e);
            }
        }
    }
    
    /**
     * Return the shortest drawable directory name matching the input qualifiers
     * @param d
     * @param qualifiers
     * @return
     */
    private String getInputClassifierDir(Density d, Density fallback, Collection<String> qualifiers) {
        final StringBuilder sb = new StringBuilder("drawable");
        if (!d.equals(fallback)) {
            sb.append("-");
            sb.append(d.name().toLowerCase());
        }
        for (String qualifier : qualifiers) {
            sb.append("-");
            sb.append(qualifier);
        }
        return sb.toString();
    }

    /**
     * Filters directory with the svg constraints
     * @param list
     *          existing directories
     * @param qualifiers
     *          qualifiers targeted by the svg resource
     * @param createMissingDirectories
     *          create a directory if no matching directory found
     * @return
     */
    private Collection<QualifiedResource> filterDestinations(final List<QualifiedResource> directories, final Set<String> qualifiers, final boolean createMissingDirectories) {
        Collection<QualifiedResource> filteredDirectories = new ArrayList<QualifiedResource>();
        for (QualifiedResource in : directories) {
            // input match requirements
            if (in.qualifiers.containsAll(qualifiers)) {
                if (createMissingDirectories && !qualifiers.containsAll(in.qualifiers)) {
                    // skip input as it doesn't match exactly the requested qualifiers
                    continue;
                }
                // otherwise, we keep the best matching existing input
                // verify that no other matching input already covers current input qualifiers
                boolean retain = true;
                for (QualifiedResource filtered : filteredDirectories) {
                    if (filtered.qualifiers.containsAll(in.qualifiers)) {
                        // filtered contains current
                        // retain current and skip filtered
                        filteredDirectories.remove(filtered);
                        break;
                    } else if (in.qualifiers.containsAll(filtered.qualifiers)) {
                        // current contains filtered
                        // skip current and retain filtered
                        retain = false;
                        break;
                    } else {
                        // disjunction, retain both
                        // ex : sample-mdpi-fr.svg
                        // >> "drawable-fr-mdpi-land/"
                        // >> "drawable-fr-mdpi-port/"
                        // and no "drawable-fr-mdpi/"
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
    private Rectangle2D extractSVGBounds(QualifiedResource svg) throws MalformedURLException, IOException {
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
    private void transcode(QualifiedResource svg, Rectangle2D bounds, QualifiedResource dest, NinePatch ninePatch) throws IOException, TranscoderException {
        transcode(svg, bounds, dest, 
                new Float(bounds.getWidth() * svg.density.ratio(dest.density)), 
                new Float(bounds.getHeight() * svg.density.ratio(dest.density)),
                ninePatch);
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
    private void transcode(QualifiedResource svg, Rectangle2D bounds, QualifiedResource dest, float targetWidth, float targetHeight, NinePatch ninePatch) throws IOException, TranscoderException {
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
        final String finalName = new StringBuilder(dest.getAbsolutePath())
            .append(System.getProperty("file.separator"))
            .append(outputName)
            .append(ninePatch != null ? ".9" : "")
            .append(".png")
            .toString();
        if (ninePatch == null) {
            // write file directly
            OutputStream ostream = new FileOutputStream(finalName);
            TranscoderOutput output = new TranscoderOutput(ostream);
            t.transcode(input, output);
            ostream.flush();
            ostream.close();
        } else {
            // write in memory
            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(ostream);
            t.transcode(input, output);
            // fill the patch
            ostream.flush();
            InputStream istream = new ByteArrayInputStream(ostream.toByteArray());
            ostream.close();
            ostream = null;
            toNinePatch(istream, finalName, ninePatch, svg.density.ratio(dest.density));
        }
    }
    
    /**
     * Draw the stretch and content area defined by the {@link NinePatch} around the given image
     * @param is
     * @param finalName
     * @param ninePatch
     * @param ratio
     * @throws IOException
     */
    private void toNinePatch(final InputStream is, final String finalName, final NinePatch ninePatch, final double ratio) throws IOException {
        BufferedImage image = ImageIO.read(is);
        final int w = image.getWidth();
        final int h = image.getHeight();
        BufferedImage ninePatchImage = new BufferedImage(
                w + 2, 
                h + 2, 
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = ninePatchImage.getGraphics();
        g.drawImage(image, 1, 1, null);
        
        // draw patch
        g.setColor(Color.BLACK);
        Zone stretch = ninePatch.getStretch();
        for (int[] seg : stretch.getX()) {
            final int start = (int) Math.floor(seg[0] * ratio);
            final int stop = (int) Math.ceil(seg[1] * ratio);
            g.fillRect(start, 0, stop, 1);
        }
        for (int[] seg : stretch.getY()) {
            final int start = (int) Math.floor(seg[0] * ratio);
            final int stop = (int) Math.ceil(seg[1] * ratio);
            g.fillRect(0, start, 1, stop);
        }
        Zone content = ninePatch.getContent();
        for (int[] seg : content.getX()) {
            final int start = (int) Math.floor(seg[0] * ratio);
            final int stop = (int) Math.ceil(seg[1] * ratio);
            g.fillRect(start, h + 1, stop, 1);
        }
        for (int[] seg : content.getY()) {
            final int start = (int) Math.floor(seg[0] * ratio);
            final int stop = (int) Math.ceil(seg[1] * ratio);
            g.fillRect(w + 1, start, 1, stop);
        }
        
        ImageIO.write(ninePatchImage, "png", new File(finalName));
    }

}
