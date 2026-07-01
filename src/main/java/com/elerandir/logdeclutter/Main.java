package com.elerandir.logdeclutter;

import com.elerandir.logdeclutter.cli.DeclutterCommand;
import picocli.CommandLine;

/** Application entry point. Kept static: it wires picocli to the CLI command and exits. */
public final class Main {

    private Main() {
        // Not instantiable.
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new DeclutterCommand()).execute(args);
        System.exit(exitCode);
    }
}
