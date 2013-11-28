package fr.avianey.modjo.androidgendrawable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.Test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class NinePatchTest {

    @Test
    public void testFromJson() throws URISyntaxException, JsonIOException, JsonSyntaxException, IOException {
        try (final Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("9patch.json"))) {
            Type t = new TypeToken<Map<String, NinePatch>>() {}.getType();
            Map<String, NinePatch> ninePatchMap = new GsonBuilder().create().fromJson(reader, t);
            System.out.println("");
        }
    }
    
}
