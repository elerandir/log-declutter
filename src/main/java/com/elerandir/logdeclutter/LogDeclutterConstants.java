package com.elerandir.logdeclutter;

import lombok.experimental.UtilityClass;

/** Central holder for the application's magic strings and numeric constants. */
@UtilityClass
public class LogDeclutterConstants {

    /** Canonical application / command name. */
    public final String APP_NAME = "log-declutter";

    /** Application version, surfaced by picocli's {@code --version}. */
    public final String VERSION = "0.1.0";

    /** Lines in the patterns file starting with this prefix are treated as comments and ignored. */
    public final String COMMENT_PREFIX = "#";

    /** Suffix appended to the source log file name when no explicit output path is given. */
    public final String DEFAULT_OUTPUT_SUFFIX = ".decluttered";

    /**
     * Prefix added by the Kubernetes CRI log format: an RFC3339Nano timestamp, the stream name
     * ({@code stdout}/{@code stderr}), and the full/partial tag ({@code F}/{@code P}), e.g.
     * {@code 2026-07-01T12:12:58.4378384Z stdout F }. Anchored at the start of a line by the
     * prefix stripper, so no leading {@code ^} is needed.
     */
    public final String CRI_PREFIX_REGEX = "\\S+\\s+(?:stdout|stderr)\\s+[FP]\\s+";

    /** Separator between the header and the message in a converted classic-style log line. */
    public final String CLASSIC_MESSAGE_SEPARATOR = " - ";

    /** Minimum width the level is right-padded to in a converted classic-style log line. */
    public final int CLASSIC_LEVEL_WIDTH = 5;

    /** Process exit code signalling a successful run. */
    public final int EXIT_SUCCESS = 0;
}
