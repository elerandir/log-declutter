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

    /** Process exit code signalling a successful run. */
    public final int EXIT_SUCCESS = 0;
}
