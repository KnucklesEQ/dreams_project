package yo.knuckleseq;

final class EnvironmentSupport {

    String resolveEnvReference(String reference) {
        if (reference == null || !reference.startsWith("env:") || reference.length() <= 4) {
            return null;
        }

        var variableName = reference.substring("env:".length());
        var value = System.getenv(variableName);
        return value == null || value.isBlank() ? null : value;
    }
}
