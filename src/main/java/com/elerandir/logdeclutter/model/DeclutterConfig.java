package com.elerandir.logdeclutter.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable runtime configuration for a single declutter run.
 *
 * <p>Bound into the Dagger object graph via {@code @BindsInstance} so services can depend on it
 * directly.
 *
 * @param logFile      the log file to declutter
 * @param patternsFile the file of partial strings whose matching lines are removed, or {@code null}
 *                     to remove no lines (blank-line and prefix cleanup still apply)
 * @param outputFile   the destination for the decluttered log
 * @param stripPrefixes ordered prefix patterns; a leading match is stripped from each line
 */
public record DeclutterConfig(
        Path logFile, Path patternsFile, Path outputFile, List<Pattern> stripPrefixes) {

    public DeclutterConfig {
        Objects.requireNonNull(logFile, "logFile");
        Objects.requireNonNull(outputFile, "outputFile");
        // patternsFile is optional (null => no removal patterns).
        stripPrefixes = List.copyOf(stripPrefixes);
    }

    /** Factory for a run with no prefix stripping. */
    public static DeclutterConfig of(Path logFile, Path patternsFile, Path outputFile) {
        return new DeclutterConfig(logFile, patternsFile, outputFile, List.of());
    }

    /** Factory for a run that also strips the given leading prefixes from each line. */
    public static DeclutterConfig of(
            Path logFile, Path patternsFile, Path outputFile, List<Pattern> stripPrefixes) {
        return new DeclutterConfig(logFile, patternsFile, outputFile, stripPrefixes);
    }
}
