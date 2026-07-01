package com.elerandir.logdeclutter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Removes a leading prefix from a log line when it matches one of the configured patterns. */
@Singleton
public class PrefixStripper {

    @Inject
    public PrefixStripper() {
        // Dagger-managed; no collaborators required.
    }

    /**
     * Strips a leading match for each prefix pattern, in order.
     *
     * <p>Each pattern is anchored at the start of the line via {@link Matcher#lookingAt()}, so the
     * pattern need not begin with {@code ^}. Patterns are applied in sequence, allowing several
     * layers of prefix to be peeled off.
     *
     * @param line     the line to strip
     * @param prefixes the prefix patterns to apply, in order
     * @return the line with any leading prefix matches removed
     */
    public String strip(String line, List<Pattern> prefixes) {
        String result = line;
        for (Pattern prefix : prefixes) {
            Matcher matcher = prefix.matcher(result);
            if (matcher.lookingAt()) {
                result = result.substring(matcher.end());
            }
        }
        return result;
    }
}
