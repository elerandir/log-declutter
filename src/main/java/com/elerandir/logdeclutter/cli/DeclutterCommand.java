package com.elerandir.logdeclutter.cli;

import com.elerandir.logdeclutter.DaggerDeclutterComponent;
import com.elerandir.logdeclutter.DeclutterComponent;
import com.elerandir.logdeclutter.LogDeclutterConstants;
import com.elerandir.logdeclutter.SummaryFormatter;
import com.elerandir.logdeclutter.model.DeclutterConfig;
import com.elerandir.logdeclutter.model.DeclutterResult;
import java.nio.file.Path;
import java.util.concurrent.Callable;
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
                "Removes log lines containing any of the given partial strings, dropping empty lines.")
public class DeclutterCommand implements Callable<Integer> {

    @Parameters(
            index = "0",
            paramLabel = "LOG_FILE",
            description = "The log file to declutter.")
    private Path logFile;

    @Parameters(
            index = "1",
            paramLabel = "PATTERNS_FILE",
            description =
                    "File of partial strings (one per line); log lines containing any are removed.")
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

    @Override
    public Integer call() throws Exception {
        DeclutterConfig config = DeclutterConfig.of(logFile, patternsFile, resolveOutputFile());
        DeclutterComponent component = DaggerDeclutterComponent.factory().create(config);
        DeclutterResult result = component.declutterer().declutter();
        System.out.println(SummaryFormatter.format(config, result));
        return LogDeclutterConstants.EXIT_SUCCESS;
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
