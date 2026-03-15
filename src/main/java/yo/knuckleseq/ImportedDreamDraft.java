package yo.knuckleseq;

record ImportedDreamDraft(
    String dreamId,
    String detectedInputPath,
    String archivedPath,
    String originalFileName,
    String sourceHash,
    long sizeBytes,
    String recordedAt,
    int dreamDayIndex,
    String dreamDayOrdinal,
    String titleFinal
) {
}
