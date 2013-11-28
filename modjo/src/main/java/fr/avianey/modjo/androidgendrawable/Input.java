package fr.avianey.modjo.androidgendrawable;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

public class Input extends File {
    
    private static final long serialVersionUID = 1L;
    
    public Density density;
    public String targetName;
    public Set<String> qualifiers;

    public Input(File file, Density density) throws IOException {
        super(file.getCanonicalPath());
        this.density = density;
        this.targetName = FilenameUtils.getBaseName(getName()).toLowerCase()
                .replaceAll("-" + density.name().toLowerCase(), "");
    }

    public Input(File file, Density density, Set<String> classifiers) throws IOException {
        this(file, density);
        this.qualifiers = classifiers;
        if (classifiers != null) {
            for (String classifier : classifiers) {
                this.targetName = this.targetName.replaceAll("-" + classifier, "");
            }
        }
    }
}