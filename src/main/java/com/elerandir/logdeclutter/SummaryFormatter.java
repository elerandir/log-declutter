package com.elerandir.logdeclutter;

import com.elerandir.logdeclutter.model.DeclutterConfig;
import com.elerandir.logdeclutter.model.DeclutterResult;
import lombok.experimental.UtilityClass;

/** Formats a {@link DeclutterResult} into the human-readable summary printed by the CLI. */
@UtilityClass
public class SummaryFormatter {

    /**
     * Builds the multi-line run summary.
     *
     * @param config the configuration that was run
     * @param result the outcome of the run
     * @return the formatted summary (no trailing newline)
     */
    public String format(DeclutterConfig config, DeclutterResult result) {
        return String.format(
                "Decluttered %s%n"
                        + "  total lines read:        %d%n"
                        + "  removed (matched):       %d%n"
                        + "  removed (blank lines):   %d%n"
                        + "  kept:                    %d%n"
                        + "  written to:              %s",
                config.logFile(),
                result.totalLines(),
                result.removedMatching(),
                result.removedBlank(),
                result.keptLines(),
                config.outputFile());
    }
}
