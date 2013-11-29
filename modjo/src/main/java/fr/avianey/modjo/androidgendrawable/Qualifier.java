package fr.avianey.modjo.androidgendrawable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Qualifier {
    
    /**
     * Exception thrown when the parsed input is not a valid resource directory name.
     */
    public static class InvalidResourceDirectoryName extends Exception {
        private static final long serialVersionUID = 1L;

        public InvalidResourceDirectoryName() {
            super();
        }
    }
    
    /**
     * Exception thrown when the parsed input is not a valid svg name.
     * <ul>
     * <li>has no {@link Qualifier}</li>
     * <li>doesn't start with the density {@link Qualifier}</li>
     * </ul>
     */
    public static class InvalidSVGName extends Exception {
        private static final long serialVersionUID = 1L;

        public InvalidSVGName(String msg) {
            super(msg);
        }
    }
    
    /**
     * Common interface for qualified input parsing.
     */
    public static interface Acceptor {
        /**
         * Return the {@link Qualifier} if found at the <u>beginning</u> of the input {@link String}.
         * If the {@link Qualifier} exists but is not at the beginning of the input {@link String}, 
         * then the input is not a valid resource directory name... 
         * @param input
         * @return
         *      The {@link Qualifier} or null if no {@link Qualifier} of the desired {@link Type} is found
         *      at the <u>beginning</u> of the input {@link String}. 
         */
        Qualifier accept(String input);
        
        /**
         * Return the {@link Pattern} {@link String} for the desired {@link Qualifier.Type}
         * @return
         *      the Regexp
         */
        String regexp();
    }
    
    private static class BaseAcceptor implements Acceptor {

        private final Type type;
        private final String regexp;
        
        public BaseAcceptor(Type type, String regexp) {
            this.type = type;
            // (capturingregexp)(-.*)*
            this.regexp = new StringBuilder("(")
                    .append(regexp)
                    .append(")")
                    .append("(-.*)?")
                    .toString();
        }
        
        @Override
        public Qualifier accept(String input) {
            Pattern p = Pattern.compile(regexp());
            Matcher m = p.matcher(input);
            Qualifier q = null;
            if (m.matches() && m.groupCount() > 0) {
                q = new Qualifier(type, m.group(1));
            }
            return q;
        }

        @Override
        public String regexp() {
            return regexp;
        }
        
    }
    
    /**
     * Qualifier types in order of precedence.<br/>
     * <a href="http://developer.android.com/guide/topics/resources/providing-resources.html">Providing Resources</a>
     */
    public enum Type {
        mcc_mnc("mcc\\d+(-mnc\\d+)?"),
        locale("[a-zA-Z]{2}(-r[a-zA-Z]{2})?"), // TODO : verify from Locale class
        layoutDirection("ldrtl|ldltr"),
        smallestWidth("sw\\d+dp"),
        availableWidth("w\\d+dp"),
        availableHeight("h\\d+dp"),
        screenSize("small|normal|large|xlarge"),
        aspect("(not)?long"),
        orientation("port|land"),
        uiMode("car|desk|television|appliance"),
        nightMode("(not)?night"),
        density("(l|m|x{0,3}h|tv|no)dpi"),
        touchScreen("notouch|finger"),
        keyboard("keysexposed|keyshidden|keyssoft"),
        textInputMethod("nokeys|qwerty|12key"),
        navigationKey("nav(exposed|hidden)"),
        nonTouchNavigationMethod("nonav|dpad|trackball|wheel"),
        plateformVersion("v\\d+"); // TODO : verify validity version code numbers
        
        private final Acceptor acceptor;

        private Type(String regexp) {
            this.acceptor = new BaseAcceptor(this, regexp);
        }

        /**
         * @return the acceptor for the {@link Qualifier.Type}
         */
        public Acceptor getAcceptor() {
            return acceptor;
        }
    }
    
    private final Type type;
    private final String value;
    
    public Qualifier(Type type, String value) {
        this.type = type;
        this.value = value;
    }
    
    /**
     * Extract the qualifiers from the svg name.<br/>
     * SVG names must be first qualified by the {@link Type#density} and then followed by
     * the other {@link Qualifier.Type} in the order of precedence...
     * @param svgFileName with no extension
     * @return
     * @throws InvalidSVGName 
     * @throws InvalidResourceDirectoryName 
     */
    public static final Map<Type, Qualifier> parseQualifiedString(final String svgFileName) throws InvalidSVGName, InvalidResourceDirectoryName {
        Map<Type, Qualifier> typedQualifiers = new EnumMap<>(Type.class);
        
        if (svgFileName.indexOf("-") <= 0) {
            throw new InvalidSVGName("SVG file has no qualifier");
        }
        
        // extract the name
        final String svnName = svgFileName.substring(0, svgFileName.indexOf("-"));
        String qualifiers = null;
        try {
            qualifiers = svgFileName.substring(svgFileName.indexOf("-") + 1);
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidSVGName("SVG file has no density qualifier");
        }
        
        Set<Type> types = EnumSet.allOf(Type.class);
        types.remove(Type.density);
        
        // extract the density first
        Qualifier density = Type.density.getAcceptor().accept(qualifiers);
        if (density == null) {
            throw new InvalidSVGName("SVG file has no density qualifier");
        }
        typedQualifiers.put(Type.density, density);
        qualifiers = qualifiers.substring(density.value.length());
        boolean found = true;
        
        // extract other qualifiers
        Iterator<Type> it = types.iterator();
        while (it.hasNext() && qualifiers.length() > 0) {
            // remove any leading "-"
            if (found && qualifiers.startsWith("-")) {
                qualifiers = qualifiers.substring(1);
            }
            Type type = it.next();
            Qualifier q = type.getAcceptor().accept(qualifiers);
            if (q != null) {
                typedQualifiers.put(type, q);
                found = true;
            } else {
                found = false;
            }
            qualifiers = qualifiers.substring(q.value.length());
        }
        
        if (qualifiers.length() != 0) {
            throw new InvalidResourceDirectoryName();
        }
        
        return typedQualifiers;
    }

}
