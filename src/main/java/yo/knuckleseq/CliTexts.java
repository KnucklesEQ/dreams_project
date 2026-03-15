package yo.knuckleseq;

final class CliTexts {

    static final String VERSION_USAGE = "Uso: sonolog version\n";
    static final String HELP_USAGE = "Uso: sonolog help [command]\nPrueba \"sonolog help\" para ver los comandos disponibles\n";
    static final String GENERAL_USAGE = "Uso: sonolog <command>\nPrueba \"sonolog help\" para ver los comandos disponibles\n";
    static final String UNKNOWN_COMMAND_HELP_MESSAGE = "Comando desconocido. Prueba \"sonolog help\" para descubrir comandos.\n";
    static final String INIT_USAGE = "Uso: sonolog init [--home <dir>] [--force]\n";
    static final String INIT_OVERWRITE_BLOCK_MESSAGE = "Configuracion existente detectada. Usa --force para forzar la reescritura.\n";
    static final String DOCTOR_USAGE = "Uso: sonolog doctor [--online] [--config <path>]\n";
    static final String SCAN_USAGE = "Uso: sonolog scan [--config <path>]\n";
    static final String STATUS_USAGE = "Uso: sonolog status [<dreamId>] [--failed] [--protected] [--config <path>]\n";
    static final String TRANSCRIBE_USAGE = "Uso: sonolog transcribe <dreamId> [--force] [--config <path>]\n";
    static final String CLEAN_USAGE = "Uso: sonolog clean <dreamId> [--force] [--config <path>]\n";
    static final String ANALYZE_USAGE = "Uso: sonolog analyze <dreamId> [--force] [--config <path>]\n";
    static final String BUILD_NOTE_USAGE = "Uso: sonolog build-note <dreamId> [--force] [--config <path>]\n";
    static final String PROCESS_USAGE = "Uso: sonolog process <dreamId> [--config <path>]\n";
    static String versionOutput(ApplicationMetadata metadata) {
        return metadata.displayName() + " " + metadata.version() + "\n";
    }

    static String generalHelpOutput(ApplicationMetadata metadata) {
        return """
%s - Diario de suenos en CLI
Comandos principales:
  init
  doctor
  scan
  process [<dreamId|path>]
  transcribe <dreamId>
  clean <dreamId>
  analyze <dreamId>
  build-note <dreamId>
  status [<dreamId>]
  help [command]
  version
Flags globales:
  --config <path>
  --json
  --verbose
  --dry-run
  --force
Ejemplos:
  sonolog help
  sonolog process
""".formatted(metadata.displayName());
    }

    static final String PROCESS_HELP_OUTPUT = """
Uso: sonolog process [<dreamId|path>]

Opciones:
  --from scan|transcribe|clean|analyze|build-note
  --to transcribe|clean|analyze|build-note
  --retry-failed
  --force

Seguridad:
  Usa --force unicamente cuando quieras reejecutar etapas frescas o sobrescribir artefactos protegidos.
""";
    static final String DOCTOR_LOCAL_OK = "Doctor local OK\n";
    static final String DOCTOR_ONLINE_OK = "Doctor online OK\n";
    static final String CONFIG_INVALID = "Configuracion invalida o inaccesible.\n";
    static final String LOCAL_ENV_INVALID = "Entorno local no valido.\n";
    static final String MISSING_API_KEY = "API key ausente.\n";
    static final String REMOTE_UNAVAILABLE = "Servicio remoto inaccesible.\n";
    static final String UNSUPPORTED_MODELS = "Configuracion de modelos no soportada.\n";
    static final String SCAN_FAILED = "No se pudo ejecutar scan.\n";
    static final String NO_NEW_AUDIO = "No hay audio nuevo.\n";
    static final String SCAN_OK = "Scan OK\n";
    static final String UNSUPPORTED_AUDIO_NAME = "Nombre de audio no soportado.\n";
    static final String STATUS_FAILED = "No se pudo ejecutar status.\n";
    static final String UNKNOWN_DREAM = "Dream no encontrado.\n";
    static final String NO_STATUS_ITEMS = "Sin items.\n";
    static final String TRANSCRIBE_FAILED = "No se pudo ejecutar transcribe.\n";
    static final String EMPTY_TRANSCRIPT = "La transcripcion esta vacia o es invalida.\n";
    static final String CLEAN_FAILED = "No se pudo ejecutar clean.\n";
    static final String INVALID_CLEAN_OUTPUT = "La salida de clean es invalida.\n";
    static final String ANALYZE_FAILED = "No se pudo ejecutar analyze.\n";
    static final String INVALID_ANALYSIS_OUTPUT = "La salida de analyze es invalida.\n";
    static final String BUILD_NOTE_FAILED = "No se pudo ejecutar build-note.\n";
    static final String NOTE_OVERWRITE_PROTECTED = "note_overwrite_protected\n";
    static final String PROCESS_FAILED = "No se pudo ejecutar process.\n";

    private CliTexts() {
    }
}
