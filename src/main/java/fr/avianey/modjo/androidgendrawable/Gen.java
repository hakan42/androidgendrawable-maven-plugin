package fr.avianey.modjo.androidgendrawable;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.EnumSet;
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
    
    /**
     * Supported screen densities
     */
    private static enum Density {
        LDPI(120),
        MDPI(160),
        HDPI(240),
        XHDPI(320),
        TVDPI(213);
        private int dpi;
        private Density(int dpi) {
            this.dpi = dpi;
        }
        public double ratio(Density target) {
            return (double) target.dpi / (double) this.dpi;
        }
    }
    
    private static class Input extends File {
        private static final long serialVersionUID = 1L;
        private Density density;
        private String targetName;
        public Input(File file, Density density) throws IOException {
            super(file.getCanonicalPath());
            this.density = density;
            this.targetName = FilenameUtils.getBaseName(getName()).toLowerCase().replaceAll("-" + density.name().toLowerCase(), "");
        }
    }
    
    private static Pattern svgPattern = null;
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
        }
        db.append(")");
        tb.append(db.toString());
        sb.append(db.toString());
        tb.append(".*");
        sb.append("\\.svg");
        svgPattern = Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
        resPattern = Pattern.compile(tb.toString(), Pattern.CASE_INSENSITIVE);
    }
    
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
        // validating
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
        
        // list destinations
        final List<Input> destinations = new ArrayList<Input>();
        final Set<Density> foundDensities = EnumSet.noneOf(Density.class);
        if (to.isDirectory()) {
            for (File f : to.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    String name = FilenameUtils.getName(file.getPath());
                    if (file.isDirectory() && name.startsWith("drawable")) {
                        if (skipNoDpi && name.toLowerCase().matches("drawable.*-nodpi(-.+){0,1}")) {
                            return false;
                        }
                        Matcher m = resPattern.matcher(name.toLowerCase());
                        if (m.matches()) {
                            try {
                                Density density = Density.valueOf(m.group(1).toUpperCase());
                                if (targetDensities_.isEmpty() 
                                        || targetDensities_.contains(density)) {
                                    destinations.add(new Input(file, density));
                                    foundDensities.add(density);
                                }
                                return true;
                            } catch (IOException e) {
                                getLog().error(e);
                            }
                        } else {
                            // drawable resources directory with no density qualifier
                            try {
                                destinations.add(new Input(file, fallbackDensity_));
                                foundDensities.add(fallbackDensity_);
                            } catch (IOException e) {
                                getLog().error(e);
                            }
                        }
                    }
                    return false;
                }
            })) {
                // log matching output directories
                getLog().debug("found output destination : " + f.getAbsolutePath());
            }
        } else {
            throw new MojoExecutionException(to.getAbsolutePath() + " is not a valid output directory");
        }
        if (createMissingDirectories && !targetDensities_.isEmpty()) {
            for (Density density : targetDensities_) {
                if (!foundDensities.contains(density)) {
                    File f = new File(
                            to.getAbsolutePath() + 
                            System.getProperty("file.separator") + 
                            "drawable" + (fallbackDensity_.equals(density) ? "" : "-" + density.name().toLowerCase()));
                    if (!f.exists()) {
                        f.mkdir();
                    }
                    try {
                        destinations.add(new Input(f, density));
                    } catch (IOException e) {
                        throw new MojoExecutionException("Error accessing file " + f.getAbsolutePath(), e);
                    }
                }
            }
        }
        
        // list input svg to convert
        final List<Input> svgToConvert = new ArrayList<Input>();
        if (from.isDirectory()) {
            for (File f : from.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    if (file.isFile()) {
                        String name = FilenameUtils.getName(file.getPath());
                        Matcher m = svgPattern.matcher(name.toLowerCase());
                        if (m.matches()) {
                            try {
                                svgToConvert.add(new Input(file, Density.valueOf(m.group(1).toUpperCase())));
                                return true;
                            } catch (IOException e) {
                                getLog().error(e);
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
        
        // create svg in res/* folder(s)
        for (Input svg : svgToConvert) {
            try {
                Rectangle2D bounds = extractSVGBounds(svg);
                if (highResIcon != null && highResIcon.equals(svg.targetName)) {
                    _highResIcon = svg;
                    _highResIconBounds = bounds;
                }
                for (Input destination : destinations) {
                    getLog().info("Transcoding " + svg.getName() + " to " + destination.getName());
                    transcode(svg, bounds, destination);
                }
            } catch (MalformedURLException e) {
                getLog().error(e);
            } catch (IOException e) {
                getLog().error(e);
            } catch (TranscoderException e) {
                getLog().error(e);
            }
        }
        
        // generates the play store high res icon
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
