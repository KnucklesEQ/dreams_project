# Sonolog

## Estado
- Version: v0.1
- Estado: borrador base aprobado para arrancar implementacion
- Objetivo: servir como fuente de verdad para futuras sesiones

## Objetivo
Construir una herramienta personal para Debian Linux que convierta audios de suenos ya grabados en notas Markdown estructuradas para un diario personal de suenos.

## Alcance v1
- CLI local para procesar uno o varios audios desde una carpeta de entrada.
- Transcripcion remota con OpenAI.
- Limpieza y estructuracion remota con un LLM de OpenAI.
- Extraccion de tags, personas, lugares, emociones y motivos.
- Generacion de una nota Markdown por audio.
- Conservacion del audio original, artefactos intermedios y resultado final.
- Revision manual posterior en Obsidian, fuera de la aplicacion.

## No objetivos v1
- App movil.
- Backend multiusuario.
- Cuentas, sync o cloud propio.
- Grabacion de audio desde la app.
- UI de revision o edicion integrada.
- Fusion de varios audios en una sola nota.

## Principios
- Local-first en almacenamiento y control operativo.
- Pipeline simple, reproducible e idempotente.
- Separacion estricta entre transcripcion, limpieza y analisis.
- Trazabilidad completa por etapa.
- Nunca inventar detalles del sueno.
- Guardar siempre versiones intermedias.

## Decisiones cerradas
- Nombre de la aplicacion: Sonolog.
- Lenguaje: Java.
- Runtime objetivo: Java 21 LTS.
- Build: Gradle.
- Interfaz inicial: CLI.
- Idioma v1: espanol.
- Preparar estructura para multiidioma mas adelante.
- Modelos v1: remotos, usando OpenAI para STT y LLM.
- Entrada v1: audios `.wav` de grabadora externa, con patron esperado `A-YYYYMMDD-HHMMSS.wav`.
- Una nota Markdown por audio.
- Rutas configurables para `inputDir`, `archiveDir` y `notesDir`.
- La app tiene una carpeta propia configurable (`appHomeDir`).

## Arquitectura de alto nivel
- `config`: carga y valida configuracion.
- `scan/import`: descubre audios, calcula hash, crea `dreamId`, extrae `recorded_at` y mueve el original a `archiveDir`.
- `transcribe`: envia audio a OpenAI y guarda respuesta cruda + salida normalizada.
- `clean`: limpia muletillas/ruido y mejora legibilidad sin anadir hechos.
- `analyze`: extrae metadatos estructurados.
- `export`: genera la nota Markdown final para Obsidian.
- `manifest`: registra estado, hashes, modelos, prompts y artefactos por archivo.

## Configuracion
Resumen:
- `config.json` debe ser pequeno, humano y declarativo.
- `manifest.json` debe ser canonico por audio y escrito por la app.
- Contrato detallado: `specs/002-config-manifest/spec.md`.

Config de referencia:

```json
{
  "schemaVersion": 1,
  "appHomeDir": "~/Documents/sonolog",
  "paths": {
    "inputDir": "~/Documents/sonolog/input",
    "archiveDir": "~/Documents/sonolog/archive",
    "notesDir": "~/Documents/sonolog/notes",
    "workspaceDir": "~/Documents/sonolog/workspace",
    "runsDir": "~/Documents/sonolog/runs",
    "logsDir": "~/Documents/sonolog/logs",
    "tmpDir": "~/Documents/sonolog/tmp"
  },
  "openai": {
    "apiKeyRef": "env:OPENAI_API_KEY",
    "baseUrl": "https://api.openai.com/v1",
    "transcriptionModel": "whisper-1",
    "cleanupModel": "gpt-4.1-mini",
    "analysisModel": "gpt-4.1-mini",
    "timeoutSeconds": 120,
    "maxRetries": 3
  },
  "pipeline": {
    "locale": "es",
    "promptSet": "es-v1",
    "skipIfUnchanged": true
  },
  "execution": {
    "maxParallelFiles": 2,
    "continueOnError": true
  }
}
```

Notas:
- `appHomeDir` es la raiz operativa de la app.
- `inputDir`, `archiveDir` y `notesDir` deben poder cambiarse independientemente.
- `inputDir` funciona como bandeja de entrada temporal.
- `archiveDir` es la ubicacion canonica del audio original una vez importado.
- `workspaceDir` guarda artefactos tecnicos y no tiene por que estar dentro del vault de Obsidian.
- `promptSet` identifica el bundle de prompts, no el prompt literal.
- La forma detallada de `manifest.json` vive en `specs/002-config-manifest/spec.md`.
- Contrato detallado de prompts y etapas: `specs/005-prompt-stage-contracts/spec.md`.

## Layout recomendado
```text
appHomeDir/
  config/
    config.json
  input/
  archive/
  notes/
  workspace/
    <dreamId>/
      stt/
      llm/
      analysis/
      manifest.json
  runs/
    <runId>/run.json
  logs/
  tmp/
```

## Artefactos por audio
```text
archive/
  A-20250108-071440.wav

workspace/<dreamId>/
  stt/
    raw-response.json
    transcript.txt
    segments.json
  llm/
    raw-response.json
    cleaned.json
  analysis/
    analysis.json
  manifest.json
```

Nota final:
- `notes/<dreamId>.md`
- Contrato detallado de `analysis.json` y `run.json`: `specs/003-analysis-run/spec.md`.
- Contrato detallado de la nota: `specs/004-note-frontmatter/spec.md`.

## Registro de ejecuciones
- `workspace/<dreamId>/manifest.json` guarda el estado canonico de un audio.
- `runs/<runId>/run.json` guarda el contexto de una ejecucion concreta del CLI.
- `run.json` es util para lotes, errores parciales, reintentos, tiempos y resumen final.
- Debe incluir al menos `runId`, inicio/fin, config efectiva, lista de `dreamId`, etapas ejecutadas y errores.
- Contrato detallado: `specs/003-analysis-run/spec.md`.

## Pipeline
Pipeline logico:

```text
scan -> import -> transcribe -> clean -> analyze -> build-note
```

Reglas:
- Cada etapa debe poder reejecutarse por separado.
- El audio original puede moverse de `inputDir` a `archiveDir`, pero no debe alterarse su contenido.
- Si el nombre sigue el patron `A-YYYYMMDD-HHMMSS.wav`, Sonolog debe extraer `recorded_at` desde el nombre.
- `scan` debe asignar y congelar el ordinal diario en el momento de importacion.
- Si una etapa posterior falla, los artefactos validos ya generados deben conservarse.
- Reprocesar solo si cambian hash de audio, modelo, prompt o config relevante.
- Cada salida debe ser determinista a partir de sus entradas normalizadas.

## Modelo de la nota Markdown
Resumen:
- `note.md` es un artefacto derivado, orientado a Obsidian.
- Contrato detallado: `specs/004-note-frontmatter/spec.md`.
- Titulo final con formato `Sue\u00f1o YYYY_MM_DD(<ordinal romano>) - <titulo>`; los ejemplos del repo usan `Sueno` por consistencia ASCII.
- La nota debe conservar `Version depurada` y `Transcripcion cruda`.
- Si la nota fue editada fuera de Sonolog, no debe sobrescribirse sin `--force`.

Frontmatter de referencia:

```yaml
---
type: dream
dream_id: 2026-03-14_001
title: "Sueno 2025_01_08(I) - Doctor excentrico"
recorded_at: 2025-01-08T07:14:40
created_at: 2026-03-14T09:15:02Z
generated_at: 2026-03-14T09:18:11Z
language: es
source_audio_name: A-20250108-071440.wav
needs_review: true
tags: []
people: []
places: []
emotions: []
motifs: []
sonolog:
  note_schema_version: 1
  pipeline_version: "0.1"
  run_id: "2026-03-14T09-15-02Z"
  dream_day_index: 1
  dream_day_ordinal: "I"
  title_candidate: "Doctor excentrico"
  source_hash: "sha256:..."
  transcript_hash: "sha256:..."
  cleaned_hash: "sha256:..."
  analysis_hash: "sha256:..."
  models:
    transcription: "whisper-1"
    cleanup: "gpt-4.1-mini"
    analysis: "gpt-4.1-mini"
---
```

Cuerpo de referencia:

```md
# Sueno 2025_01_08(I) - Doctor excentrico

## Version depurada
...

## Elementos extraidos

### Personas
- madre

### Lugares
- casa

### Emociones
- miedo

### Motivos
- persecucion

### Tags
- familia

## Secuencia
1. Primero aparece en la casa de infancia.

## Ambiguedades
- [inaudible] en segmento 7

## Transcripcion cruda
...

## Trazabilidad
- Dream ID: 2026-03-14_001
- Run ID: 2026-03-14T09-15-02Z
- Grabado: 2025-01-08T07:14:40
- Fuente: A-20250108-071440.wav
- Hash fuente: sha256:...
- Modelos: whisper-1 / gpt-4.1-mini / gpt-4.1-mini
```

## Guardrails anti alucinacion
- Prompt estricto: aclarar y reordenar, nunca completar huecos.
- Si algo no esta claro, usar `dudoso`, `inaudible` o `no determinado`.
- Guardar siempre transcripcion cruda junto a la version depurada.
- Guardar respuestas raw de API siempre que sea posible.
- El analisis debe basarse solo en texto existente.
- Marcar para revision manual cualquier entidad claramente nueva respecto al texto fuente.
- Contrato detallado de prompts e I/O por etapa: `specs/005-prompt-stage-contracts/spec.md`.

## CLI prevista
- `sonolog init`
- `sonolog scan`
- `sonolog process [<ruta|dreamId>]`
- `sonolog transcribe <dreamId>`
- `sonolog clean <dreamId>`
- `sonolog analyze <dreamId>`
- `sonolog build-note <dreamId>`
- `sonolog status <dreamId>`
- `sonolog doctor`
- UX CLI detallada: `specs/006-cli-ux/spec.md`.

## Stack recomendado
- Java 21 LTS.
- Gradle.
- `picocli` para CLI.
- `OkHttp` para HTTP.
- `Jackson` para JSON.
- `SnakeYAML` para frontmatter YAML.
- `ffmpeg` opcional para normalizacion interna de audio si hiciera falta, sin tocar el `.wav` original archivado.
- `JUnit 5` para tests.

## Criterios de aceptacion MVP
- Procesa uno o varios audios desde `inputDir`.
- Mueve cada audio importado a `archiveDir` y extrae `recorded_at` desde el nombre cuando siga el patron esperado.
- Congela el ordinal diario al importar por primera vez y lo usa para el titulo final.
- Genera una nota `.md` por cada audio en `notesDir`.
- Conserva artefactos intermedios y final; el borrado posterior del audio archivado queda bajo control manual del usuario.
- Permite reejecucion segura y trazable.
- No inventa detalles del sueno.
- Funciona en Debian Linux.
- La revision manual se hace despues en Obsidian.

## Plan
- Plan de implementacion: `specs/001-sonolog-foundation/plan.md`.

## Estado actual
- Esta especificacion recoge las decisiones ya tomadas en la conversacion actual.
- Puede usarse como punto de arranque para implementacion en sesiones futuras.
