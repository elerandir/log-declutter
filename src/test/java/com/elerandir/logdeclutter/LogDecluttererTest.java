package com.elerandir.logdeclutter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.elerandir.logdeclutter.model.DeclutterConfig;
import com.elerandir.logdeclutter.model.DeclutterResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Behaviour tests that drive the real Dagger object graph end to end with realistic inputs and
 * assert the observable output file and result summary.
 */
@DisplayName("LogDeclutterer (via the real Dagger graph)")
class LogDecluttererTest {

    @TempDir
    Path workDir;

    /** Builds the real object graph and runs a declutter with the given files. */
    private DeclutterResult declutter(Path logFile, Path patternsFile, Path outputFile)
            throws IOException {
        DeclutterConfig config = DeclutterConfig.of(logFile, patternsFile, outputFile);
        return DaggerDeclutterComponent.factory().create(config).declutterer().declutter();
    }

    /** Builds the real object graph and runs a declutter, also stripping the given prefixes. */
    private DeclutterResult declutter(
            Path logFile, Path patternsFile, Path outputFile, List<Pattern> stripPrefixes)
            throws IOException {
        DeclutterConfig config =
                DeclutterConfig.of(logFile, patternsFile, outputFile, stripPrefixes);
        return DaggerDeclutterComponent.factory().create(config).declutterer().declutter();
    }

    private Path writeFile(String name, String... lines) throws IOException {
        Path file = workDir.resolve(name);
        Files.write(file, List.of(lines), StandardCharsets.UTF_8);
        return file;
    }

    private List<String> readLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("given matching lines, when decluttering, then they are removed and order preserved")
        void removesMatchingLinesAndPreservesOrder() throws IOException {
            Path log =
                    writeFile(
                            "app.log",
                            "INFO start",
                            "DEBUG noisy detail",
                            "ERROR real problem",
                            "DEBUG another noise",
                            "WARN keep me");
            Path patterns = writeFile("patterns.txt", "DEBUG");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, patterns, output);

            assertEquals(List.of("INFO start", "ERROR real problem", "WARN keep me"), readLines(output));
            assertEquals(5, result.totalLines());
            assertEquals(2, result.removedMatching());
            assertEquals(0, result.removedBlank());
            assertEquals(3, result.keptLines());
        }

        @Test
        @DisplayName("given multiple patterns, when decluttering, then a line matching any is removed")
        void removesLinesMatchingAnyPattern() throws IOException {
            Path log =
                    writeFile(
                            "app.log",
                            "INFO keep",
                            "DEBUG drop",
                            "TRACE drop",
                            "WARN keep");
            Path patterns = writeFile("patterns.txt", "DEBUG", "TRACE");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, patterns, output);

            assertEquals(List.of("INFO keep", "WARN keep"), readLines(output));
            assertEquals(2, result.removedMatching());
        }

        @Test
        @DisplayName("given a partial string, when decluttering, then any line containing it is removed")
        void removesLinesContainingPartialString() throws IOException {
            Path log =
                    writeFile(
                            "app.log",
                            "2026-07-01 request id=42 completed",
                            "2026-07-01 healthcheck ping",
                            "2026-07-01 request id=7 failed");
            // Partial substrings matched anywhere in a line.
            Path patterns = writeFile("patterns.txt", "healthcheck", "id=42");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, patterns, output);

            assertEquals(List.of("2026-07-01 request id=7 failed"), readLines(output));
            assertEquals(2, result.removedMatching());
        }

        @Test
        @DisplayName("given regex metacharacters, when decluttering, then they are matched literally, not as regex")
        void treatsRegexMetacharactersLiterally() throws IOException {
            Path log =
                    writeFile(
                            "app.log",
                            "level=[INFO] service started",
                            "cost was $5.00 (approx.)",
                            "regex a.c should not match abc",
                            "abc is kept");
            // These lines contain '[', ']', '$', '.', '(', ')', '*' which are regex-special.
            Path patterns =
                    writeFile("patterns.txt", "[INFO]", "$5.00 (approx.)", "a.c");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, patterns, output);

            // 'a.c' matches the literal "a.c" line but NOT "abc" (which regex '.' would have matched).
            assertEquals(List.of("abc is kept"), readLines(output));
            assertEquals(3, result.removedMatching());
        }
    }

    @Nested
    @DisplayName("blank-line handling")
    class BlankLines {

        @Test
        @DisplayName("given blank lines, when decluttering, then none are left in the output")
        void dropsBlankLines() throws IOException {
            Path log =
                    writeFile(
                            "app.log",
                            "INFO one",
                            "",
                            "   ",
                            "\t",
                            "INFO two");
            Path patterns = writeFile("patterns.txt");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, patterns, output);

            assertEquals(List.of("INFO one", "INFO two"), readLines(output));
            assertEquals(3, result.removedBlank());
            assertEquals(0, result.removedMatching());
        }

        @Test
        @DisplayName("given a blank line created by removing a match, when decluttering, then no gap remains")
        void leavesNoGapWhereMatchesWereRemoved() throws IOException {
            Path log = writeFile("app.log", "keep A", "DROP", "keep B");
            Path patterns = writeFile("patterns.txt", "DROP");
            Path output = workDir.resolve("out.log");

            declutter(log, patterns, output);

            assertEquals(List.of("keep A", "keep B"), readLines(output));
        }

        @ParameterizedTest(name = "whitespace-only line [{0}] counts as blank")
        @ValueSource(strings = {" ", "   ", "\t", "\t \t"})
        @DisplayName("given whitespace-only lines, when decluttering, then they are treated as blank")
        void treatsWhitespaceOnlyLinesAsBlank(String blank) throws IOException {
            Path log = writeFile("app.log", "INFO keep", blank);
            Path patterns = writeFile("patterns.txt");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, patterns, output);

            assertEquals(List.of("INFO keep"), readLines(output));
            assertEquals(1, result.removedBlank());
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        @DisplayName("given an empty log, when decluttering, then the output is empty")
        void handlesEmptyLog() throws IOException {
            Path log = writeFile("app.log");
            Path patterns = writeFile("patterns.txt", "DEBUG");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, patterns, output);

            assertTrue(readLines(output).isEmpty());
            assertEquals(0, result.totalLines());
            assertEquals(0, result.keptLines());
        }

        @Test
        @DisplayName("given only comment and blank lines in the patterns file, when decluttering, then no patterns apply")
        void ignoresCommentAndBlankPatternLines() throws IOException {
            Path log = writeFile("app.log", "INFO keep", "DEBUG also kept now");
            Path patterns = writeFile("patterns.txt", "# this is a comment", "", "   ");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, patterns, output);

            assertEquals(List.of("INFO keep", "DEBUG also kept now"), readLines(output));
            assertEquals(0, result.removedMatching());
        }

        @Test
        @DisplayName("given a pattern matching every line, when decluttering, then the output is empty")
        void handlesAllLinesRemoved() throws IOException {
            Path log = writeFile("app.log", "DEBUG a", "DEBUG b", "DEBUG c");
            Path patterns = writeFile("patterns.txt", "DEBUG");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, patterns, output);

            assertTrue(readLines(output).isEmpty());
            assertEquals(0, Files.size(output), "output file should be empty, not a blank line");
            assertEquals(3, result.removedMatching());
            assertEquals(0, result.keptLines());
        }
    }

    @Nested
    @DisplayName("error paths")
    class ErrorPaths {

        @Test
        @DisplayName("given a missing log file, when decluttering, then NoSuchFileException is thrown")
        void throwsWhenLogFileMissing() throws IOException {
            Path patterns = writeFile("patterns.txt", "DEBUG");
            Path missingLog = workDir.resolve("does-not-exist.log");
            Path output = workDir.resolve("out.log");

            assertThrows(NoSuchFileException.class, () -> declutter(missingLog, patterns, output));
        }

        @Test
        @DisplayName("given a missing patterns file, when decluttering, then NoSuchFileException is thrown")
        void throwsWhenPatternsFileMissing() throws IOException {
            Path log = writeFile("app.log", "INFO");
            Path missingPatterns = workDir.resolve("nope.txt");
            Path output = workDir.resolve("out.log");

            assertThrows(NoSuchFileException.class, () -> declutter(log, missingPatterns, output));
        }

        @Test
        @DisplayName("given a would-be-invalid regex, when decluttering, then it is matched literally without error")
        void treatsInvalidRegexStringAsLiteral() throws IOException {
            Path log = writeFile("app.log", "INFO [unclosed bracket here", "INFO clean line");
            // '[unclosed' is not a valid regex, but as a literal pattern it must not throw.
            Path patterns = writeFile("patterns.txt", "[unclosed");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, patterns, output);

            assertEquals(List.of("INFO clean line"), readLines(output));
            assertEquals(1, result.removedMatching());
        }
    }

    @Nested
    @DisplayName("prefix stripping")
    class PrefixStripping {

        private final Pattern cri = Pattern.compile(LogDeclutterConstants.CRI_PREFIX_REGEX);

        @Test
        @DisplayName("given the CRI prefix, when stripping, then only the payload remains and it is counted")
        void stripsCriPrefix() throws IOException {
            Path log =
                    writeFile(
                            "app.log",
                            "2026-07-01T12:12:58.4378384Z stdout F {\"level\":\"INFO\",\"msg\":\"a\"}",
                            "2026-07-01T12:12:59.0000000Z stderr F {\"level\":\"WARN\",\"msg\":\"b\"}");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, null, output, List.of(cri));

            assertEquals(
                    List.of("{\"level\":\"INFO\",\"msg\":\"a\"}", "{\"level\":\"WARN\",\"msg\":\"b\"}"),
                    readLines(output));
            assertEquals(2, result.strippedPrefixes());
            assertEquals(2, result.keptLines());
        }

        @Test
        @DisplayName("given a line without the prefix, when stripping, then it is unchanged and not counted")
        void leavesUnprefixedLinesUntouched() throws IOException {
            Path log =
                    writeFile(
                            "app.log",
                            "2026-07-01T12:12:58.4378384Z stdout F wrapped line",
                            "already clean line");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, null, output, List.of(cri));

            assertEquals(List.of("wrapped line", "already clean line"), readLines(output));
            assertEquals(1, result.strippedPrefixes());
        }

        @Test
        @DisplayName("given stripping and removal patterns, when running, then patterns match the stripped content")
        void appliesRemovalPatternsToStrippedContent() throws IOException {
            Path log =
                    writeFile(
                            "app.log",
                            "2026-07-01T12:12:58.4378384Z stdout F DEBUG noisy",
                            "2026-07-01T12:12:59.0000000Z stdout F INFO keep");
            Path patterns = writeFile("patterns.txt", "DEBUG");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, patterns, output, List.of(cri));

            assertEquals(List.of("INFO keep"), readLines(output));
            assertEquals(2, result.strippedPrefixes());
            assertEquals(1, result.removedMatching());
        }

        @Test
        @DisplayName("given a custom prefix regex, when stripping, then its leading match is removed")
        void stripsCustomPrefix() throws IOException {
            Path log = writeFile("app.log", "[worker-7] task done", "[worker-7] task queued");
            // Anchored automatically at line start; no leading '^' needed.
            Path patterns = null;
            Path output = workDir.resolve("out.log");

            DeclutterResult result =
                    declutter(log, patterns, output, List.of(Pattern.compile("\\[worker-\\d+\\]\\s+")));

            assertEquals(List.of("task done", "task queued"), readLines(output));
            assertEquals(2, result.strippedPrefixes());
        }

        @Test
        @DisplayName("given a line that becomes blank after stripping, when running, then it is dropped")
        void dropsLinesThatBecomeBlankAfterStripping() throws IOException {
            Path log =
                    writeFile(
                            "app.log",
                            "2026-07-01T12:12:58.4378384Z stdout F ",
                            "2026-07-01T12:12:59.0000000Z stdout F kept");
            Path output = workDir.resolve("out.log");

            DeclutterResult result = declutter(log, null, output, List.of(cri));

            assertEquals(List.of("kept"), readLines(output));
            assertEquals(1, result.removedBlank());
            assertEquals(1, result.keptLines());
        }
    }
}
