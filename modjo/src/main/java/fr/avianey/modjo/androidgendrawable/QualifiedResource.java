package fr.avianey.modjo.androidgendrawable;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import com.google.common.base.Preconditions;

import fr.avianey.modjo.androidgendrawable.Qualifier.Acceptor;
import fr.avianey.modjo.androidgendrawable.Qualifier.Type;

public class QualifiedResource extends File {
    
    private static final long serialVersionUID = 1L;
    
    public Density density;
    public String targetName;
    public Set<String> qualifiers;

    public QualifiedResource(File file, Density density) throws IOException {
        super(file.getCanonicalPath());
        this.density = density;
        this.targetName = FilenameUtils.getBaseName(getName()).toLowerCase()
                .replaceAll("-" + density.name().toLowerCase(), "");
    }

    public QualifiedResource(File file, Density density, Set<String> classifiers) throws IOException {
        this(file, density);
        this.qualifiers = classifiers;
        if (classifiers != null) {
            for (String classifier : classifiers) {
                this.targetName = this.targetName.replaceAll("-" + classifier, "");
            }
        }
    }
    
    /**/
    
    private String name;
    private Map<Type, Qualifier> typedQualifiers;
    
    private QualifiedResource(final File file, final String name, Map<Type, Qualifier> qualifiers) {
        super(file.getAbsolutePath());
        this.name = name;
        this.typedQualifiers = qualifiers;
        // TODO : pre-calculate output : drawable-fr-{density}-land
    }
    
    public File getOutputFor(Density density) {
        return null;
    }
    
    /**
     * Create a {@link QualifiedResource} from an input SVG file.
     * @param file
     * @return
     */
    public static final QualifiedResource fromSvgFile(final File file) {
        
        Preconditions.checkNotNull(file);
        final String extension = FilenameUtils.getExtension(file.getAbsolutePath());
        final String fileName = FilenameUtils.getBaseName(file.getAbsolutePath());
        Preconditions.checkArgument(extension.toLowerCase().equals("svg"));
        Preconditions.checkArgument(fileName.length() > 0);
        
        // unqualified name
        final String unqualifiedName = fileName.substring(0, fileName.indexOf("-"));
        Preconditions.checkArgument(unqualifiedName != null && unqualifiedName.matches("\\w+"));
        
        // qualifiers
        final Map<Type, Qualifier> typedQualifiers = new EnumMap<>(Type.class);
        String qualifiers = fileName.substring(fileName.indexOf("-") + 1);
        Preconditions.checkArgument(qualifiers.length() > 0);
        
        while (qualifiers.length() > 0) {
            // remove leading "-"
            int i = -1;
            while (qualifiers.indexOf("-", i) == i + 1) {
                i++;
            }
            if (i >= 0) {
                qualifiers = qualifiers.substring(i + 1);
            }
            
            Qualifier q = null;
            for (Type type : EnumSet.allOf(Type.class)) {
                Acceptor a = new Acceptor(type);
                q = a.accept(qualifiers);
                if (q != null) {
                    qualifiers = qualifiers.substring(q.getValue().length());
                    typedQualifiers.put(type, q);
                    break;
                }
            }
            
            if (q == null) {
                if (qualifiers.indexOf("-") < 0) {
                    break;
                } else {
                    qualifiers = qualifiers.substring(qualifiers.indexOf("-") + 1);
                }
            }
            
        }
        
        // a density qualifier must be provided
        Preconditions.checkNotNull(typedQualifiers.get(Type.density));
        
        return new QualifiedResource(file, unqualifiedName, typedQualifiers);
    }

    public Map<Type, Qualifier> getTypedQualifiers() {
        return typedQualifiers;
    }
    
}