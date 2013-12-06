package fr.avianey.modjo.androidgendrawable;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.avianey.modjo.androidgendrawable.Qualifier.Acceptor;
import fr.avianey.modjo.androidgendrawable.Qualifier.Type;

/**
 * Describe the configuration for a 9-Patch drawable:
 * <dl>
 * <dt>Stretchable area</dt>
 * <dd>coordinates of start & stop points for segments along the x-axis</dd>
 * <dd>coordinates of start & stop points for segments along the y-axis</dd>
 * <dt>Content area</dt>
 * <dd>coordinates of start & stop points for segments along the x-axis</dd>
 * <dd>coordinates of start & stop points for segments along the y-axis</dd>
 * </dl>
 * If no segment defined for an area along an axis, the whole axis is used as a segment.
 * Coordinates must be include within the svg bounds (width and height).
 * 
 * @version 1
 * @author avianey
 */
public class NinePatch {

    private String name;
    private Zone stretch = new Zone();
    private Zone content = new Zone();
    
    // for applying nine-patch config only for some qualified inputs
    private Collection<String> qualifiers;
    private transient Map<Type, String> typedQualifiers;
    
    public static class Zone {
        
        private int[][] x;
        private int[][] y;
        
        /**
         * @return the x
         */
        public int[][] getX() {
            return x;
        }
        /**
         * @return the y
         */
        public int[][] getY() {
            return y;
        }
        
    }

    /**
     * @return the stretch
     */
    public Zone getStretch() {
        return stretch;
    }

    /**
     * @return the content
     */
    public Zone getContent() {
        return content;
    }


    /**
     * @return the typedQualifiers
     */
    public Map<Type, String> getTypedQualifiers() {
        return typedQualifiers;
    }
    
    @SuppressWarnings("unchecked")
    public static final NinePatchMap init(Set<NinePatch> ninePatchSet) {
        NinePatchMap map = new NinePatchMap();
        for (NinePatch ninePatch : ninePatchSet) {
            // classify by name
            Set<NinePatch> set = map.get(ninePatch.name);
            if (set == null) {
                set = new HashSet<NinePatch>();
                map.put(ninePatch.name, set);
            }
            set.add(ninePatch);
            // extract qualifiers
            if (ninePatch.qualifiers != null) {
                ninePatch.typedQualifiers = new EnumMap<>(Type.class);
                for (String qualifier : ninePatch.qualifiers) {
                    for (Type t : EnumSet.allOf(Type.class)) {
                        String value = new Acceptor(t).accept(qualifier);
                        if (value != null) {
                            ninePatch.typedQualifiers.put(t, value);
                            break;
                        }
                    }
                }
            } else {
                ninePatch.typedQualifiers = Collections.EMPTY_MAP;
            }
        }
        return map;
    }

}
