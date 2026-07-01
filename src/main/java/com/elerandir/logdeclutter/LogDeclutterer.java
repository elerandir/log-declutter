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
 * Orchestrates a single declutter run: load patterns, strip prefixes, filter the log, and write the
 * result.
 *
 * <p>Each line first has any configured leading prefix stripped. It is then removed when it is blank
 * (so the output never contains empty lines) or when it matches a filter pattern. Everything else is
 * preserved, with its prefix removed, in its original order.
 */
@Singleton
public class LogDeclutterer {

    private final DeclutterConfig config;
    private final PatternLoader patternLoader;
    private final LineFilter lineFilter;
    private final PrefixStripper prefixStripper;

    @Inject
    public LogDeclutterer(
            DeclutterConfig config,
            PatternLoader patternLoader,
            LineFilter lineFilter,
            PrefixStripper prefixStripper) {
        this.config = config;
        this.patternLoader = patternLoader;
        this.lineFilter = lineFilter;
        this.prefixStripper = prefixStripper;
    }

    /**
     * Runs the declutter operation described by the injected {@link DeclutterConfig}.
     *
     * @return a summary of the run
     * @throws NoSuchFileException if the log or patterns file does not exist
     * @throws IOException if reading or writing fails
     */
    public DeclutterResult declutter() throws IOException {
        FilterPatterns patterns =
                config.patternsFile() == null
                        ? FilterPatterns.of(List.of())
                        : patternLoader.load(config.patternsFile());
        List<String> lines = Files.readAllLines(config.logFile(), StandardCharsets.UTF_8);

        List<String> kept = new ArrayList<>(lines.size());
        int removedMatching = 0;
        int removedBlank = 0;
        int strippedPrefixes = 0;
        for (String rawLine : lines) {
            String line = prefixStripper.strip(rawLine, config.stripPrefixes());
            if (!line.equals(rawLine)) {
                strippedPrefixes++;
            }

            if (line.isBlank()) {
                removedBlank++;
            } else if (lineFilter.matches(line, patterns)) {
                removedMatching++;
            } else {
                kept.add(line);
            }
        }

        Files.write(config.outputFile(), kept, StandardCharsets.UTF_8);
        return new DeclutterResult(
                lines.size(), removedMatching, removedBlank, kept.size(), strippedPrefixes);
    }
}
