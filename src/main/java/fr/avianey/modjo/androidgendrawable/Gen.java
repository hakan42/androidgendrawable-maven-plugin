package fr.avianey.modjo.androidgendrawable;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.svg.SVGDocument;

/**
 * Goal which touches a timestamp file.
 * 
 * @goal gen
 * @phase generate-resources
 */
// TODO : extract supported density from manifest : http://wiki.apache.org/xmlgraphics-batik/BootSvgAndCssDom : http://batik.2283329.n4.nabble.com/getBBox-returns-null-td4655273.html
public class Gen extends AbstractMojo {
    
    private static final int LDPI   = 120;
    private static final int MDPI   = 160;
    private static final int HDPI   = 240;
    private static final int XHDPI  = 320;
    private static final int TVDPI  = 213;
    
    /**
     * Location of the svg resources to generate drawable from.
     * 
     * @parameter
     * @required
     */
    private String from;
    
    /**
     * Location of the root directory for :
     * <ul>
     * <li>drawable</li>
     * <li>drawable-hdpi</li>
     * <li>drawable-ldpi</li>
     * <li>drawable-mdpi</li>
     * <li>drawable-xhdpi</li>
     * </ul>
     * 
     * @parameter
     * @required
     */
    private String to;

    public void execute() throws MojoExecutionException {
        // list destinations
        
        
        // list input svg to convert
        
        File dir = new File(from);
        if (dir.isDirectory()) {
            for (File f : dir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isFile();
                }
            })) {
                getLog().info(f.getAbsolutePath());
            }
        }
        File f = new File(to + "/drawable/" + "toto.png");
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(f.getAbsolutePath());
    }

    public static void main(String[] args) throws TranscoderException, IOException {
        
        String svgURI = new File("Level.svg").toURL().toString();
        
        // load svg bounds
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
        SVGDocument doc = (SVGDocument) f.createDocument(svgURI);
        UserAgent userAgent = new UserAgentAdapter();
        DocumentLoader loader = new DocumentLoader(userAgent);
        BridgeContext ctx = new BridgeContext(userAgent, loader);
        ctx.setDynamicState(BridgeContext.DYNAMIC);
        GVTBuilder builder = new GVTBuilder();
        GraphicsNode rootGN = builder.build(ctx, doc);
        Rectangle2D svgBounds = rootGN.getBounds();
        
        PNGTranscoder t = new PNGTranscoder();
//        if (parameter != null) {
//            t.addTranscodingHint(PNGTranscoder.KEY_WIDTH, new Float(parameter.desiredWidth));
//            t.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, new Float(parameter.desiredHeight));
//        }

        // Set the transcoding hints.
//        t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(.8));

        // Create the transcoder input.
        TranscoderInput input = new TranscoderInput(svgURI);

        // Create the transcoder output.
        OutputStream ostream = new FileOutputStream("out.png");
        TranscoderOutput output = new TranscoderOutput(ostream);

        // Save the image.
        t.transcode(input, output);

        // Flush and close the stream.
        ostream.flush();
        ostream.close();
        System.exit(0);
    }

}
