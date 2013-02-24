package fr.avianey.modjo.androidgendrawable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which touches a timestamp file.
 * 
 * @goal gen
 * @phase generate-resources
 */
public class Gen extends AbstractMojo {
    
    /**
     * Location of the svg resources to generate drawable from.
     * @parameter
     * @required
     */
    private String generateFrom;

    /**
     * @parameter default-value="./res"
     */
    private String androidResPath;

    /**
     * @parameter
     */
    private String extension;

    public void execute() throws MojoExecutionException {
        File dir = new File(generateFrom);
        if (dir.isDirectory()) {
            for (File f : dir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isFile();
                }
            })) {
                getLog().info(f.getAbsolutePath());
            }
        }
        File f = new File(androidResPath + "/drawable/" + "toto.png");
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(f.getAbsolutePath());
    }
}
