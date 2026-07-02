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
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Orchestrates a single declutter run: load patterns, strip prefixes, filter the log, and write the
 * result.
 *
 * <p>Each line first has any configured leading prefix stripped. Removal (blank line or pattern
 * match) is then decided against that stripped content, so patterns written against the raw log
 * (e.g. JSON fields) match regardless of later reformatting. Surviving lines are optionally
 * unwrapped from JSON into a classic-style line before being written, preserving original order.
 */
@Singleton
public class LogDeclutterer {

    private final DeclutterConfig config;
    private final PatternLoader patternLoader;
    private final LineFilter lineFilter;
    private final PrefixStripper prefixStripper;
    private final JsonLogConverter jsonLogConverter;

    @Inject
    public LogDeclutterer(
            DeclutterConfig config,
            PatternLoader patternLoader,
            LineFilter lineFilter,
            PrefixStripper prefixStripper,
            JsonLogConverter jsonLogConverter) {
        this.config = config;
        this.patternLoader = patternLoader;
        this.lineFilter = lineFilter;
        this.prefixStripper = prefixStripper;
        this.jsonLogConverter = jsonLogConverter;
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
        int convertedJson = 0;
        for (String rawLine : lines) {
            String line = prefixStripper.strip(rawLine, config.stripPrefixes());
            if (!line.equals(rawLine)) {
                strippedPrefixes++;
            }

            // Decide removal against the stripped, pre-unwrap content so patterns written against
            // the raw log (JSON fields and all) still match.
            if (line.isBlank()) {
                removedBlank++;
                continue;
            }
            if (lineFilter.matches(line, patterns)) {
                removedMatching++;
                continue;
            }

            // Surviving line: optionally unwrap JSON for the output.
            if (config.unwrapJson()) {
                Optional<String> converted = jsonLogConverter.convert(line);
                if (converted.isPresent()) {
                    line = converted.get();
                    convertedJson++;
                }
            }
            kept.add(line);
        }

        Files.write(config.outputFile(), kept, StandardCharsets.UTF_8);
        return new DeclutterResult(
                lines.size(), removedMatching, removedBlank, kept.size(), strippedPrefixes,
                convertedJson);
    }
}
