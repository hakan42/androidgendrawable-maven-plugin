package fr.avianey.modjo.androidgendrawable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import fr.avianey.modjo.androidgendrawable.Qualifier.Type;

@RunWith(Parameterized.class)
public class QualifiedResourceTest {

    private final String name;
    private final Map<Type, Qualifier> typedQualifiers;
    private final boolean successExpected;
    
    public QualifiedResourceTest(String name, Object[][] qualifiers, boolean successExpected) {
        this.name = name;
        this.successExpected = successExpected;
        this.typedQualifiers = new EnumMap<>(Type.class);
        if (qualifiers != null) {
            for (Object[] o : qualifiers) {
                typedQualifiers.put((Type) o[0], new Qualifier((Type) o[0], (String) o[1]));
            }
        }
    }
    
    @Parameters
    public static Collection<Object[]> data() {
//        Object[][] configs = new Object[][] {
//                {Type.mcc_mnc, new Object[] {"mcc310", "mcc310-mnc004", "mcc208-mnc00"}},
//                {Type.locale, new Object[] {"en", "fr", "en-rUS", "fr-rFR", "fr-rCA"}},
//                {Type.layoutDirection, new Object[] {"ldrtl", "ldltr"}},
//                {Type.smallestWidth, new Object[] {"sw320dp", "sw600dp", "sw720dp"}},
//                {Type.availableWidth, new Object[] {"w720dp", "w1024dp"}},
//                {Type.availableHeight, new Object[] {"h720dp", "h1024dp"}},
//                {Type.screenSize, new Object[] {"small", "normal", "large", "xlarge"}},
//                {Type.aspect, new Object[] {"long", "notlong"}},
//                {Type.orientation, new Object[] {"port", "land"}},
//                {Type.uiMode, new Object[] {"car", "desk", "television", "appliance"}},
//                {Type.nightMode, new Object[] {"night", "notnight"}},
//                {Type.density, new Object[] {"ldpi", "mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi", "nodpi", "tvdpi"}},
//                {Type.touchScreen, new Object[] {"notouch", "finger"}},
//                {Type.keyboard, new Object[] {"keysexposed", "keyshidden", "keyssoft"}},
//                {Type.textInputMethod, new Object[] {"nokeys", "qwerty", "12key"}},
//                {Type.navigationKey, new Object[] {"navexposed", "navhidden"}},
//                {Type.nonTouchNavigationMethod, new Object[] {"nonav", "dpad", "trackball", "wheel"}},
//                {Type.plateformVersion, new Object[] {"v3", "v4", "v15", "v19"}}
//        };
//        // all possibilities
//        long n = 0;
//        Map<String, Collection<Qualifier>> names = new HashMap<>();
//        names.put("", Collections.EMPTY_LIST);
//        for (int i = 0; i < configs.length; i++) {
//            Object[] o = configs[i];
//            Type t = (Type) o[0];
//            System.out.println(t.name() + " " + n);
//            Object[] sample = (Object[]) o[1];
//            Set<String> existingKeys = new HashSet<>(names.keySet());
//            for (int j = 0; j < sample.length; j++) {
//                for (String name : existingKeys) {
//                    Collection<Qualifier> _old = names.get(name);
//                    Collection<Qualifier> _new = new ArrayList<>(_old);
//                    _new.add(new Qualifier(t, (String) sample[j]));
//                    String newKey = name + "-" + (String) sample[j];
//                    names.put(newKey, _new);
//                    n++;
//                }
//            }
//        }
        return Arrays.asList(
                new Object[][] { 
                        // passing cases
                        {"name-ldpi.svg", new Object[][] {{Type.density, "ldpi"}}, true},
                        {"name-mdpi.svg", new Object[][] {{Type.density, "mdpi"}}, true},
                        {"name-hdpi.svg", new Object[][] {{Type.density, "hdpi"}}, true},
                        {"name-xhdpi.svg", new Object[][] {{Type.density, "xhdpi"}}, true},
                        {"name-xxhdpi.svg", new Object[][] {{Type.density, "xxhdpi"}}, true},
                        {"name-xxxhdpi.svg", new Object[][] {{Type.density, "xxxhdpi"}}, true},
                        {"name-nohdpi.svg", new Object[][] {{Type.density, "nodpi"}}, true},
                        {"name-tvhdpi.svg", new Object[][] {{Type.density, "tvdpi"}}, true},
                        {"name-mcc310-mnc004-en-rUS-xxxhdpi-land.svg", new Object[][] {
                                {Type.density, "xxxhdpi"}, 
                                {Type.locale, "en-rUS"}, 
                                {Type.orientation, "land"}, 
                                {Type.mcc_mnc, "mcc310-mnc004"}
                        }, true},
                        {"name-en-rUS-mcc310-mnc004-xxhdpi-land.svg", new Object[][] {
                                {Type.density, "xxhdpi"}, 
                                {Type.locale, "en-rUS"}, 
                                {Type.orientation, "land"}, 
                                {Type.mcc_mnc, "mcc310-mnc004"}
                        }, true},
                        {"name-ldpi-sw100dp-h400dp-w731dp-v19-port-xlarge.svg", new Object[][] {
                                {Type.density, "ldpi"}, 
                                {Type.smallestWidth, "sw100dp"}, 
                                {Type.availableHeight, "h400dp"}, 
                                {Type.availableWidth, "w731dp"}, 
                                {Type.plateformVersion, "v19"}, 
                                {Type.orientation, "port"}, 
                                {Type.screenSize, "xlarge"}
                        }, true},
                        {"name-mdpi-fr-land.svg", new Object[][] {
                                {Type.density, "mdpi"}, 
                                {Type.locale, "fr"}, 
                                {Type.orientation, "land"}
                        }, true},
                        // error cases
                        {"mdpi-fr-land.svg", null, false},
                        {"name-mdpi-fr-land", null, false},
                        {"", null, false},
                        {null, null, false},
                        {"name.svg", null, false}
                });
    }
    
    @Test
    public void test() {
        File file = Mockito.mock(File.class);
        Mockito.when(file.getAbsolutePath()).thenReturn(name);
        
        QualifiedResource qr = null;
        try {
            qr = QualifiedResource.fromSvgFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        Assert.assertEquals(successExpected, qr != null);
        if (qr != null) {
            Assert.assertTrue(qr.getTypedQualifiers().keySet().containsAll(typedQualifiers.keySet())
                    && typedQualifiers.keySet().containsAll(qr.getTypedQualifiers().keySet()));
            for (Type t : typedQualifiers.keySet()) {
                Assert.assertEquals(typedQualifiers.get(t).getValue(), 
                        qr.getTypedQualifiers().get(t).getValue());
            }
        }
    }
    
}
