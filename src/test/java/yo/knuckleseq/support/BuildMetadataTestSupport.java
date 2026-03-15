package yo.knuckleseq.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public final class BuildMetadataTestSupport {

    private static final String RESOURCE_PATH = "/yo/knuckleseq/build-info.properties";

    private BuildMetadataTestSupport() {
    }

    public static BuildMetadata load() {
        var properties = new Properties();

        try (InputStream inputStream = BuildMetadataTestSupport.class.getResourceAsStream(RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Build metadata resource not found: " + RESOURCE_PATH);
            }

            properties.load(inputStream);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read build metadata resource", exception);
        }

        return new BuildMetadata(required(properties, "app.name"), required(properties, "app.version"));
    }

    private static String required(Properties properties, String key) {
        var value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing build metadata property: " + key);
        }

        return value;
    }

    public record BuildMetadata(String displayName, String version) {
    }
}
