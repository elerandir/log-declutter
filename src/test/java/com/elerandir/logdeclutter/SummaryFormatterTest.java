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
        DeclutterResult result = new DeclutterResult(10, 4, 2, 4);

        String expected =
                String.format(
                        "Decluttered logs/app.log%n"
                                + "  total lines read:        10%n"
                                + "  removed (matched):       4%n"
                                + "  removed (blank lines):   2%n"
                                + "  kept:                    4%n"
                                + "  written to:              logs/app.decluttered.log");

        assertEquals(expected, SummaryFormatter.format(config, result));
    }
}
