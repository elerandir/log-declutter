package com.elerandir.logdeclutter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.elerandir.logdeclutter.model.DeclutterConfig;
import com.elerandir.logdeclutter.model.DeclutterResult;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SummaryFormatter")
class SummaryFormatterTest {

    @Test
    @DisplayName("given a result, when formatting, then it produces the exact multi-line summary")
    void formatsExactSummary() {
        DeclutterConfig config =
                DeclutterConfig.of(
                        Path.of("logs/app.log"),
                        Path.of("logs/patterns.txt"),
                        Path.of("logs/app.decluttered.log"));
        DeclutterResult result = new DeclutterResult(10, 4, 2, 4, 8, 6);

        // Paths are rendered via Path.toString(), whose separator is OS-specific, so build the
        // expected path segments from the same Path objects to keep the assertion cross-platform.
        String expected =
                String.format(
                        "Decluttered %s%n"
                                + "  total lines read:        10%n"
                                + "  prefixes stripped:       8%n"
                                + "  json lines converted:    6%n"
                                + "  removed (matched):       4%n"
                                + "  removed (blank lines):   2%n"
                                + "  kept:                    4%n"
                                + "  written to:              %s",
                        config.logFile(),
                        config.outputFile());

        assertEquals(expected, SummaryFormatter.format(config, result));
    }
}
