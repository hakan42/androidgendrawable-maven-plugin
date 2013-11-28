package fr.avianey.modjo.androidgendrawable;

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

    private Zone stretch;
    private Zone content;
    
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

}
