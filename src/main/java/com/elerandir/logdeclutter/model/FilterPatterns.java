package com.elerandir.logdeclutter.model;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Immutable set of compiled regular expressions used to identify log lines to remove.
 *
 * @param patterns the compiled patterns; defensively copied into an unmodifiable list
 */
public record FilterPatterns(List<Pattern> patterns) {

    public FilterPatterns {
        // Defensive copy: List.copyOf rejects nulls and yields an unmodifiable list.
        patterns = List.copyOf(patterns);
    }

    /** Intention-revealing factory for a set of filter patterns. */
    public static FilterPatterns of(List<Pattern> patterns) {
        return new FilterPatterns(patterns);
    }

    /** Whether there are no patterns (every non-blank line is kept). */
    public boolean isEmpty() {
        return patterns.isEmpty();
    }
}
