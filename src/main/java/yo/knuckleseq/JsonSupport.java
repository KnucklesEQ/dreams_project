package yo.knuckleseq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;

final class JsonSupport {

    static final ObjectMapper STRICT_MAPPER = JsonMapper.builder()
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build();

    static final ObjectMapper LENIENT_MAPPER = STRICT_MAPPER.copy()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    static String toStableJson(Object value) throws IOException {
        return STRICT_MAPPER.writeValueAsString(value).replace(" : ", ": ");
    }

    private JsonSupport() {
    }
}
