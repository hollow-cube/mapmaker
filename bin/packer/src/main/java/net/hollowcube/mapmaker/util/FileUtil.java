package net.hollowcube.mapmaker.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.hollowcube.mapmaker.SpriteTransform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class FileUtil {

    private static final Gson GSON = new Gson();

    private FileUtil() {
    }

    public static JsonElement getJson(Path path) {
        try (var reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, JsonElement.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void walkResourcesDirectory(String directory, ThrowingLambdas.BiConsumer<String, InputStream> consumer) {
        try (var stream = FileUtil.class.getResourceAsStream(directory)) {
            Objects.requireNonNull(stream);

            var reader = new BufferedReader(new InputStreamReader(stream));
            String file;
            while ((file = reader.readLine()) != null) {
                try (var fileStream = SpriteTransform.class.getResourceAsStream(directory + file)) {
                    consumer.accept(file, Objects.requireNonNull(fileStream));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
