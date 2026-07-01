package com.elerandir.logdeclutter;

import com.elerandir.logdeclutter.model.FilterPatterns;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Loads partial-string patterns from a file and compiles them into regular expressions.
 *
 * <p>Blank lines and lines beginning with {@link LogDeclutterConstants#COMMENT_PREFIX} are ignored;
 * every other line is treated as a regex fragment matched against whole log lines.
 */
@Singleton
public class PatternLoader {

    @Inject
    public PatternLoader() {
        // Dagger-managed; no collaborators required.
    }

    /**
     * Reads and compiles the patterns from the given file.
     *
     * @param patternsFile the file to read
     * @return the compiled patterns
     * @throws NoSuchFileException if the patterns file does not exist
     * @throws IOException if the file cannot be read
     * @throws java.util.regex.PatternSyntaxException if a line is not a valid regular expression
     */
    public FilterPatterns load(Path patternsFile) throws IOException {
        List<String> rawLines = Files.readAllLines(patternsFile, StandardCharsets.UTF_8);
        List<Pattern> patterns = new ArrayList<>();
        for (String rawLine : rawLines) {
            String pattern = rawLine.strip();
            if (pattern.isEmpty() || pattern.startsWith(LogDeclutterConstants.COMMENT_PREFIX)) {
                continue;
            }
            patterns.add(Pattern.compile(pattern));
        }
        return FilterPatterns.of(patterns);
    }
}
