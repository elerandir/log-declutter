package com.elerandir.logdeclutter.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable runtime configuration for a single declutter run.
 *
 * <p>Bound into the Dagger object graph via {@code @BindsInstance} so services can depend on it
 * directly.
 *
 * @param logFile      the log file to declutter
 * @param patternsFile the file of partial-string patterns whose matching lines are removed
 * @param outputFile   the destination for the decluttered log
 */
public record DeclutterConfig(Path logFile, Path patternsFile, Path outputFile) {

    public DeclutterConfig {
        Objects.requireNonNull(logFile, "logFile");
        Objects.requireNonNull(patternsFile, "patternsFile");
        Objects.requireNonNull(outputFile, "outputFile");
    }

    /** Intention-revealing factory for a declutter configuration. */
    public static DeclutterConfig of(Path logFile, Path patternsFile, Path outputFile) {
        return new DeclutterConfig(logFile, patternsFile, outputFile);
    }
}
