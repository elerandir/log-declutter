package com.elerandir.logdeclutter.cli;

import com.elerandir.logdeclutter.DaggerDeclutterComponent;
import com.elerandir.logdeclutter.DeclutterComponent;
import com.elerandir.logdeclutter.LogDeclutterConstants;
import com.elerandir.logdeclutter.SummaryFormatter;
import com.elerandir.logdeclutter.model.DeclutterConfig;
import com.elerandir.logdeclutter.model.DeclutterResult;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * picocli entry command and composition root: parses arguments, builds the Dagger object graph with
 * the runtime configuration, and runs the declutter operation.
 */
@Command(
        name = LogDeclutterConstants.APP_NAME,
        mixinStandardHelpOptions = true,
        version = LogDeclutterConstants.APP_NAME + " " + LogDeclutterConstants.VERSION,
        description =
                "Cleans up logs: strips leading prefixes, unwraps JSON logs, removes lines "
                        + "containing any given partial string, and drops empty lines.")
public class DeclutterCommand implements Callable<Integer> {

    @Parameters(
            index = "0",
            paramLabel = "LOG_FILE",
            description = "The log file to declutter.")
    private Path logFile;

    @Parameters(
            index = "1",
            arity = "0..1",
            paramLabel = "PATTERNS_FILE",
            description =
                    "Optional file of partial strings (one per line); log lines containing any are "
                            + "removed. Omit to only strip prefixes and drop blank lines.")
    private Path patternsFile;

    @Option(
            names = {"-o", "--output"},
            paramLabel = "OUTPUT_FILE",
            description =
                    "Destination for the decluttered log."
                            + " Defaults to the input file name with a '"
                            + LogDeclutterConstants.DEFAULT_OUTPUT_SUFFIX
                            + "' suffix.")
    private Path outputFile;

    @Option(
            names = "--strip-cri",
            description =
                    "Strip the Kubernetes CRI runtime prefix "
                            + "(e.g. '2026-07-01T12:12:58.4378384Z stdout F ') from each line.")
    private boolean stripCri;

    @Option(
            names = {"-s", "--strip-prefix"},
            paramLabel = "REGEX",
            description =
                    "Regex whose leading match is stripped from each line (anchored at line start; "
                            + "no '^' needed). Repeatable; applied in order.")
    private List<String> stripPrefixRegexes = new ArrayList<>();

    @Option(
            names = {"-j", "--unwrap-json"},
            description =
                    "Convert JSON-object log lines into classic '<timestamp> <LEVEL> [<thread>] "
                            + "<logger> - <message>' lines. Non-JSON lines pass through unchanged.")
    private boolean unwrapJson;

    @Override
    public Integer call() throws Exception {
        DeclutterConfig config =
                new DeclutterConfig(
                        logFile, patternsFile, resolveOutputFile(), stripPrefixes(), unwrapJson);
        DeclutterComponent component = DaggerDeclutterComponent.factory().create(config);
        DeclutterResult result = component.declutterer().declutter();
        System.out.println(SummaryFormatter.format(config, result));
        return LogDeclutterConstants.EXIT_SUCCESS;
    }

    /** Compiles the configured prefixes: the built-in CRI prefix (if requested) then custom ones. */
    private List<Pattern> stripPrefixes() {
        List<Pattern> prefixes = new ArrayList<>();
        if (stripCri) {
            prefixes.add(Pattern.compile(LogDeclutterConstants.CRI_PREFIX_REGEX));
        }
        for (String regex : stripPrefixRegexes) {
            prefixes.add(Pattern.compile(regex));
        }
        return prefixes;
    }

    /** Resolves the output path, defaulting alongside the input log when {@code --output} is absent. */
    private Path resolveOutputFile() {
        if (outputFile != null) {
            return outputFile;
        }
        String defaultName = logFile.getFileName() + LogDeclutterConstants.DEFAULT_OUTPUT_SUFFIX;
        Path parent = logFile.getParent();
        return parent == null ? Path.of(defaultName) : parent.resolve(defaultName);
    }
}
