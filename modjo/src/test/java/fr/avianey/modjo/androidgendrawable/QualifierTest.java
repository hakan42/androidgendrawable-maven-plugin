package fr.avianey.modjo.androidgendrawable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import fr.avianey.modjo.androidgendrawable.Qualifier.InvalidResourceDirectoryName;
import fr.avianey.modjo.androidgendrawable.Qualifier.InvalidSVGName;
import fr.avianey.modjo.androidgendrawable.Qualifier.Type;

@RunWith(Parameterized.class)
public class QualifierTest {

    private String svgFileName;
    private final boolean successExpected;
    private final Set<String> errorsExpected;
    
    public QualifierTest(String svgFileName, boolean successExpected, String[] errorsExpected) {
        this.svgFileName = svgFileName;
        this.successExpected = successExpected;
        this.errorsExpected = errorsExpected == null ? null : new HashSet<String>(Arrays.asList(errorsExpected));
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] { 
                        {"sample_name-mdpi", true, null}
                });
    }
    @Test
    public void parseQualifiedString() {
//            try {
//                Map<Type, Qualifier> qualifiers = Qualifier.parseQualifiedString(svgFileName);
//                Assert.assertTrue(successExpected);
//            } catch (InvalidSVGName e) {
//                Assert.assertFalse(successExpected);
//            } catch (InvalidResourceDirectoryName e) {
//                Assert.assertFalse(successExpected);
//            }
    }
    
}
