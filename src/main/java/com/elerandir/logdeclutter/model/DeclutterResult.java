package com.elerandir.logdeclutter.model;

/**
 * Immutable summary of a completed declutter run.
 *
 * @param totalLines      number of lines read from the source log
 * @param removedMatching lines dropped because they matched a pattern
 * @param removedBlank    blank lines dropped so the output contains no empty lines
 * @param keptLines       lines written to the decluttered output
 */
public record DeclutterResult(int totalLines, int removedMatching, int removedBlank, int keptLines) {

    public DeclutterResult {
        if (totalLines < 0 || removedMatching < 0 || removedBlank < 0 || keptLines < 0) {
            throw new IllegalArgumentException("line counts must be non-negative");
        }
    }

    /** Total number of lines removed (pattern matches plus blank lines). */
    public int removedTotal() {
        return removedMatching + removedBlank;
    }
}
