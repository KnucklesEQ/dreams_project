package yo.knuckleseq;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

record ApplicationMetadata(String displayName, String version) {

    private static final String RESOURCE_PATH = "/yo/knuckleseq/build-info.properties";

    static ApplicationMetadata load() {
        var properties = new Properties();

        try (InputStream inputStream = ApplicationMetadata.class.getResourceAsStream(RESOURCE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Build metadata resource not found: " + RESOURCE_PATH);
            }

            properties.load(inputStream);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read build metadata resource", exception);
        }

        return new ApplicationMetadata(required(properties, "app.name"), required(properties, "app.version"));
    }

    private static String required(Properties properties, String key) {
        var value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing build metadata property: " + key);
        }

        return value;
    }
}
