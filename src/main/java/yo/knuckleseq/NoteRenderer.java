package yo.knuckleseq;

import java.util.List;
import java.util.stream.Collectors;

final class NoteRenderer {

    String render(
        ManifestDocument manifest,
        CleanedDocument cleanedDocument,
        AnalysisDocument analysisDocument,
        String transcriptText,
        AppConfig config,
        String generatedAt,
        String runId,
        String titleFinal
    ) throws java.io.IOException {
        var titleCandidate = analysisDocument.titleCandidate() == null ? null : analysisDocument.titleCandidate().text();
        var frontmatter = new StringBuilder();
        frontmatter.append("---\n");
        frontmatter.append("type: dream\n");
        frontmatter.append("dream_id: ").append(manifest.dreamId()).append("\n");
        frontmatter.append("title: \"").append(titleFinal).append("\"\n");
        frontmatter.append("recorded_at: ").append(manifest.source().recordedAt()).append("\n");
        frontmatter.append("created_at: ").append(manifest.createdAt()).append("\n");
        frontmatter.append("generated_at: ").append(generatedAt).append("\n");
        frontmatter.append("language: ").append(manifest.locale()).append("\n");
        frontmatter.append("needs_review: ").append(manifest.needsReview()).append("\n");
        frontmatter.append("tags: ").append(frontmatterList(values(analysisDocument.entities().tags()))).append("\n");
        frontmatter.append("people: ").append(frontmatterList(values(analysisDocument.entities().people()))).append("\n");
        frontmatter.append("places: ").append(frontmatterList(values(analysisDocument.entities().places()))).append("\n");
        frontmatter.append("emotions: ").append(frontmatterList(values(analysisDocument.entities().emotions()))).append("\n");
        frontmatter.append("motifs: ").append(frontmatterList(values(analysisDocument.entities().motifs()))).append("\n");
        frontmatter.append("source_audio_name: ").append(manifest.source().originalFileName()).append("\n");
        frontmatter.append("sonolog:\n");
        frontmatter.append("  note_schema_version: 1\n");
        frontmatter.append("  pipeline_version: \"").append(manifest.pipelineVersion()).append("\"\n");
        frontmatter.append("  run_id: \"").append(runId).append("\"\n");
        frontmatter.append("  dream_day_index: ").append(manifest.naming().dreamDayIndex()).append("\n");
        frontmatter.append("  dream_day_ordinal: \"").append(manifest.naming().dreamDayOrdinal()).append("\"\n");
        frontmatter.append("  title_candidate: ").append(titleCandidate == null ? "null" : "\"" + titleCandidate + "\"").append("\n");
        frontmatter.append("  source_hash: \"").append(manifest.source().sha256()).append("\"\n");
        frontmatter.append("  transcript_hash: \"").append(HashingSupport.sha256(transcriptText)).append("\"\n");
        frontmatter.append("  cleaned_hash: \"").append(HashingSupport.sha256(JsonSupport.toStableJson(cleanedDocument))).append("\"\n");
        frontmatter.append("  analysis_hash: \"").append(HashingSupport.sha256(JsonSupport.toStableJson(analysisDocument))).append("\"\n");
        frontmatter.append("  models:\n");
        frontmatter.append("    transcription: \"").append(config.openai().transcriptionModel()).append("\"\n");
        frontmatter.append("    cleanup: \"").append(config.openai().cleanupModel()).append("\"\n");
        frontmatter.append("    analysis: \"").append(config.openai().analysisModel()).append("\"\n");
        frontmatter.append("---\n\n");

        var body = new StringBuilder();
        body.append("# ").append(titleFinal).append("\n\n");
        body.append("## Version depurada\n").append(cleanedDocument.cleanText()).append("\n\n");
        body.append("## Elementos extraidos\n\n");
        appendEntitySection(body, "Personas", values(analysisDocument.entities().people()));
        appendEntitySection(body, "Lugares", values(analysisDocument.entities().places()));
        appendEntitySection(body, "Emociones", values(analysisDocument.entities().emotions()));
        appendEntitySection(body, "Motivos", values(analysisDocument.entities().motifs()));
        appendEntitySection(body, "Tags", values(analysisDocument.entities().tags()));

        if (analysisDocument.timeline() != null && !analysisDocument.timeline().isEmpty()) {
            body.append("## Secuencia\n");
            for (var item : analysisDocument.timeline()) {
                body.append(item.order()).append(". ").append(item.text()).append("\n");
            }
            body.append("\n");
        }

        body.append("## Ambiguedades\n");
        if (analysisDocument.uncertainties().isEmpty()) {
            body.append("- Ninguna detectada\n\n");
        } else {
            for (var uncertainty : analysisDocument.uncertainties()) {
                body.append("- ").append(uncertainty.text()).append("\n");
            }
            body.append("\n");
        }

        body.append("## Transcripcion cruda\n").append(transcriptText.stripTrailing()).append("\n\n");
        body.append("## Trazabilidad\n");
        body.append("- Dream ID: ").append(manifest.dreamId()).append("\n");
        body.append("- Run ID: ").append(runId).append("\n");
        body.append("- Grabado: ").append(manifest.source().recordedAt()).append("\n");
        body.append("- Fuente: ").append(manifest.source().originalFileName()).append("\n");
        body.append("- Hash fuente: ").append(manifest.source().sha256()).append("\n");
        body.append("- Modelos: ").append(config.openai().transcriptionModel())
            .append(" / ").append(config.openai().cleanupModel())
            .append(" / ").append(config.openai().analysisModel()).append("\n");

        return frontmatter.append(body).toString();
    }

    private void appendEntitySection(StringBuilder body, String title, List<String> values) {
        body.append("### ").append(title).append("\n");
        if (values.isEmpty()) {
            body.append("- Ninguno\n\n");
            return;
        }

        for (var value : values) {
            body.append("- ").append(value).append("\n");
        }
        body.append("\n");
    }

    private List<String> values(List<AnalysisDocument.AnalysisEntity> entities) {
        return entities.stream().map(AnalysisDocument.AnalysisEntity::normalized).collect(Collectors.toList());
    }

    private String frontmatterList(List<String> values) {
        if (values.isEmpty()) {
            return "[]";
        }

        return "[" + values.stream().collect(Collectors.joining(", ")) + "]";
    }
}
