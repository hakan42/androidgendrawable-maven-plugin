package fr.avianey.modjo.androidgendrawable;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

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
    private final Density density;
    private final File outputDirectory;
    
    public QualifiedResourceTest(String name, Object[][] qualifiers, Density density, String outputDirectoryName, boolean successExpected) {
        this.name = name;
        this.successExpected = successExpected;
        this.typedQualifiers = new EnumMap<>(Type.class);
        if (qualifiers != null) {
            for (Object[] o : qualifiers) {
                typedQualifiers.put((Type) o[0], new Qualifier((Type) o[0], (String) o[1]));
            }
        }
        this.density = density;
        this.outputDirectory = new File(".", "drawable-" + outputDirectoryName);
    }
    
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] { 
                        // passing cases
                        {"name-ldpi.svg", new Object[][] {{Type.density, "ldpi"}}, Density.hdpi, "hdpi", true},
                        {"name-mdpi.svg", new Object[][] {{Type.density, "mdpi"}}, Density.hdpi, "hdpi", true},
                        {"name-hdpi.svg", new Object[][] {{Type.density, "hdpi"}}, Density.hdpi, "hdpi", true},
                        {"name-xhdpi.svg", new Object[][] {{Type.density, "xhdpi"}}, Density.hdpi, "hdpi", true},
                        {"name-xxhdpi.svg", new Object[][] {{Type.density, "xxhdpi"}}, Density.hdpi, "hdpi", true},
                        {"name-xxxhdpi.svg", new Object[][] {{Type.density, "xxxhdpi"}}, Density.hdpi, "hdpi", true},
                        {"name-nodpi.svg", new Object[][] {{Type.density, "nodpi"}}, Density.hdpi, "hdpi", true},
                        {"name-tvdpi.svg", new Object[][] {{Type.density, "tvdpi"}}, Density.hdpi, "hdpi", true},
                        {"name-mcc310-mnc004-en-rUS-xxxhdpi-land.svg", new Object[][] {
                                {Type.density, "xxxhdpi"}, 
                                {Type.locale, "en-rUS"}, 
                                {Type.orientation, "land"}, 
                                {Type.mcc_mnc, "mcc310-mnc004"}
                        }, Density.ldpi, "mcc310-mnc004-en-rUS-land-ldpi", true},
                        {"name-en-rUS-mcc310-mnc004-xxhdpi-land.svg", new Object[][] {
                                {Type.density, "xxhdpi"}, 
                                {Type.locale, "en-rUS"}, 
                                {Type.orientation, "land"}, 
                                {Type.mcc_mnc, "mcc310-mnc004"}
                        }, Density.ldpi, "mcc310-mnc004-en-rUS-land-ldpi", true},
                        {"name-ldpi-sw100dp-h400dp-w731dp-v19-port-xlarge.svg", new Object[][] {
                                {Type.density, "ldpi"}, 
                                {Type.smallestWidth, "sw100dp"}, 
                                {Type.availableHeight, "h400dp"}, 
                                {Type.availableWidth, "w731dp"}, 
                                {Type.plateformVersion, "v19"}, 
                                {Type.orientation, "port"}, 
                                {Type.screenSize, "xlarge"}
                        }, Density.tvdpi, "sw100dp-w731dp-h400dp-xlarge-port-tvdpi-v19", true},
                        {"name-mdpi-fr-land.svg", new Object[][] {
                                {Type.density, "mdpi"}, 
                                {Type.locale, "fr"}, 
                                {Type.orientation, "land"}
                        }, Density.xxhdpi, "fr-land-xxhdpi", true},
                        // error cases
                        {"mdpi-fr-land.svg", null, null, null, false},
                        {"name-mdpi-fr-land", null, null, null, false},
                        {"", null, null, null, false},
                        {null, null, null, null, false},
                        {"name.svg", null, null, null, false}
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
            // verify name
            Assert.assertEquals(outputDirectory.getAbsolutePath(), qr.getOutputFor(density, new File("."), null).getAbsolutePath());
        }
    }
    
}
