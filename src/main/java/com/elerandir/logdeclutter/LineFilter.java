package com.elerandir.logdeclutter;

import com.elerandir.logdeclutter.model.FilterPatterns;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Decides whether a single log line matches any of the configured filter patterns. */
@Singleton
public class LineFilter {

    @Inject
    public LineFilter() {
        // Dagger-managed; no collaborators required.
    }

    /**
     * Whether the line contains a match for at least one pattern.
     *
     * <p>Uses {@link java.util.regex.Matcher#find()} so a partial-string pattern matches any whole
     * line that contains it.
     *
     * @param line     the log line to test
     * @param patterns the patterns to test against
     * @return {@code true} if the line should be removed
     */
    public boolean matches(String line, FilterPatterns patterns) {
        for (Pattern pattern : patterns.patterns()) {
            if (pattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }
}
