package com.elerandir.logdeclutter;

import com.elerandir.logdeclutter.model.DeclutterConfig;
import com.elerandir.logdeclutter.model.DeclutterResult;
import com.elerandir.logdeclutter.model.FilterPatterns;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Orchestrates a single declutter run: load patterns, filter the log, and write the result.
 *
 * <p>Lines are removed when they are blank (so the output never contains empty lines) or when they
 * match a filter pattern. Everything else is preserved in its original order.
 */
@Singleton
public class LogDeclutterer {

    private final DeclutterConfig config;
    private final PatternLoader patternLoader;
    private final LineFilter lineFilter;

    @Inject
    public LogDeclutterer(DeclutterConfig config, PatternLoader patternLoader, LineFilter lineFilter) {
        this.config = config;
        this.patternLoader = patternLoader;
        this.lineFilter = lineFilter;
    }

    /**
     * Runs the declutter operation described by the injected {@link DeclutterConfig}.
     *
     * @return a summary of the run
     * @throws NoSuchFileException if the log or patterns file does not exist
     * @throws IOException if reading or writing fails
     * @throws java.util.regex.PatternSyntaxException if the patterns file contains an invalid regex
     */
    public DeclutterResult declutter() throws IOException {
        FilterPatterns patterns = patternLoader.load(config.patternsFile());
        List<String> lines = Files.readAllLines(config.logFile(), StandardCharsets.UTF_8);

        List<String> kept = new ArrayList<>(lines.size());
        int removedMatching = 0;
        int removedBlank = 0;
        for (String line : lines) {
            if (line.isBlank()) {
                removedBlank++;
            } else if (lineFilter.matches(line, patterns)) {
                removedMatching++;
            } else {
                kept.add(line);
            }
        }

        Files.write(config.outputFile(), kept, StandardCharsets.UTF_8);
        return new DeclutterResult(lines.size(), removedMatching, removedBlank, kept.size());
    }
}
