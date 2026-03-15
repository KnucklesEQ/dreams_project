# Testing Flow Inventory

- F01 - `sonolog version` / `sonolog --version` muestra la version de la aplicacion
  Specs: `specs/007-help-and-version/spec.md`
  Flujo: el usuario consulta la version y recibe solo el nombre de la aplicacion y su version, sin depender de config, red ni archivos
  Clase propuesta: `DisplayApplicationVersionTest`

- F02 - `sonolog help` / `sonolog --help` muestra una ayuda general de la CLI
  Specs: `specs/007-help-and-version/spec.md`, `specs/006-cli-ux/spec.md`
  Flujo: el usuario pide ayuda general y recibe un resumen estable de comandos, flags globales y ejemplos de uso
  Clase propuesta: `DisplayApplicationHelpMessageTest`

- F03 - `sonolog help <command>` muestra la ayuda de un comando concreto
  Specs: `specs/007-help-and-version/spec.md`, `specs/006-cli-ux/spec.md`
  Flujo: el usuario pide ayuda de un comando concreto y recibe su uso, argumentos, flags y notas de seguridad relevantes
  Clase propuesta: `DisplayCommandHelpMessageTest`

- F04 - `sonolog init --home <dir>` inicializa el espacio de trabajo de Sonolog
  Specs: `specs/006-cli-ux/spec.md`, `specs/001-sonolog-foundation/spec.md`, `specs/002-config-manifest/spec.md`
  Flujo: el usuario inicializa Sonolog en una ruta y la aplicacion crea la estructura base de carpetas y el `config/config.json`
  Clase propuesta: `InitializeApplicationHomeDirectoryTest`

- F05 - `sonolog doctor` valida el entorno local de la aplicacion
  Specs: `specs/006-cli-ux/spec.md`, `specs/002-config-manifest/spec.md`
  Flujo: el usuario valida la configuracion y las rutas locales antes de ejecutar el pipeline, sin depender de red
  Clase propuesta: `ValidateLocalApplicationEnvironmentTest`

- F06 - `sonolog doctor --online` valida la disponibilidad remota de la aplicacion
  Specs: `specs/006-cli-ux/spec.md`
  Flujo: el usuario extiende la validacion local con la comprobacion de reachability remota y de la configuracion de modelos
  Clase propuesta: `ValidateOnlineApplicationEnvironmentTest`

- F07 - `sonolog scan` descubre e importa nuevos audios de suenos
  Specs: `specs/006-cli-ux/spec.md`, `specs/005-prompt-stage-contracts/spec.md`, `specs/002-config-manifest/spec.md`, `specs/001-sonolog-foundation/spec.md`
  Flujo: el usuario escanea `inputDir` y la aplicacion detecta `.wav`, calcula su hash, asigna identidad estable, archiva el original y crea su manifest
  Clase propuesta: `ScanAndImportDreamAudioFilesTest`

- F08 - `sonolog status <dreamId>` muestra el estado actual de un sueno
  Specs: `specs/006-cli-ux/spec.md`, `specs/002-config-manifest/spec.md`
  Flujo: el usuario consulta un `dreamId` conocido y obtiene una vista concisa de su estado actual, incluyendo señales de error, stale o nota protegida
  Clase propuesta: `DisplayDreamItemStatusTest`

- F09 - `sonolog status` muestra el estado de varios suenos con filtros opcionales
  Specs: `specs/006-cli-ux/spec.md`, `specs/002-config-manifest/spec.md`
  Flujo: el usuario revisa el estado global de los items conocidos y puede filtrar por condiciones como failed, stale o protected
  Clase propuesta: `DisplayDreamBatchStatusTest`

- F10 - `sonolog transcribe <dreamId>` genera la transcripcion de un audio importado
  Specs: `specs/006-cli-ux/spec.md`, `specs/005-prompt-stage-contracts/spec.md`, `specs/002-config-manifest/spec.md`
  Flujo: el usuario lanza la transcripcion de un audio archivado y la aplicacion produce los artefactos canonicos de transcript y actualiza el manifest
  Clase propuesta: `TranscribeDreamAudioFileTest`

- F11 - `sonolog clean <dreamId>` genera la version depurada de la transcripcion
  Specs: `specs/006-cli-ux/spec.md`, `specs/005-prompt-stage-contracts/spec.md`, `specs/002-config-manifest/spec.md`
  Flujo: el usuario depura la transcripcion de un sueno y la aplicacion valida y persiste el `cleaned.json` como salida canonica
  Clase propuesta: `CleanDreamTranscriptTest`

- F12 - `sonolog analyze <dreamId>` genera el analisis estructurado del sueno
  Specs: `specs/006-cli-ux/spec.md`, `specs/005-prompt-stage-contracts/spec.md`, `specs/003-analysis-run/spec.md`, `specs/002-config-manifest/spec.md`
  Flujo: el usuario analiza un sueno ya depurado y la aplicacion produce un `analysis.json` con evidencia y metadatos estructurados
  Clase propuesta: `AnalyzeCleanedDreamNarrativeTest`

- F13 - `sonolog build-note <dreamId>` construye la nota Markdown final
  Specs: `specs/006-cli-ux/spec.md`, `specs/004-note-frontmatter/spec.md`, `specs/005-prompt-stage-contracts/spec.md`, `specs/002-config-manifest/spec.md`, `specs/003-analysis-run/spec.md`
  Flujo: el usuario genera la nota final y la aplicacion renderiza frontmatter y cuerpo Markdown de forma determinista a partir de los artefactos previos
  Clase propuesta: `BuildDreamMarkdownNoteTest`

- F14 - `sonolog build-note <dreamId>` protege una nota editada fuera de Sonolog
  Specs: `specs/004-note-frontmatter/spec.md`, `specs/006-cli-ux/spec.md`, `specs/002-config-manifest/spec.md`
  Flujo: el usuario intenta regenerar una nota modificada manualmente y la aplicacion detecta la proteccion por hash y evita sobreescribirla sin `--force`
  Clase propuesta: `ProtectExternallyEditedDreamNoteTest`

- F15 - `sonolog process <dreamId>` procesa un sueno conocido de extremo a extremo
  Specs: `specs/006-cli-ux/spec.md`, `specs/001-sonolog-foundation/spec.md`, `specs/002-config-manifest/spec.md`, `specs/003-analysis-run/spec.md`, `specs/004-note-frontmatter/spec.md`, `specs/005-prompt-stage-contracts/spec.md`
  Flujo: el usuario procesa un item concreto y la aplicacion ejecuta las etapas necesarias en orden, conserva artefactos parciales y registra el resultado del run
  Clase propuesta: `ProcessKnownDreamItemTest`

- F16 - `sonolog process` procesa todos los audios elegibles de extremo a extremo
  Specs: `specs/006-cli-ux/spec.md`, `specs/001-sonolog-foundation/spec.md`, `specs/002-config-manifest/spec.md`, `specs/003-analysis-run/spec.md`, `specs/004-note-frontmatter/spec.md`, `specs/005-prompt-stage-contracts/spec.md`
  Flujo: el usuario ejecuta el happy path principal y la aplicacion escanea, selecciona y procesa todos los items elegibles, dejando trazabilidad completa del lote
  Clase propuesta: `ProcessAllEligibleDreamAudioFilesTest`
