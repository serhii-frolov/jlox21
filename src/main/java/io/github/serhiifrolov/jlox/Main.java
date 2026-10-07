package io.github.serhiifrolov.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * CLI entry point: owns args, I/O and the process exit code. All language logic lives in {@link Lox}.
 *
 * <p>Never lets an exception escape on bad input: every failure becomes one line on stderr and an
 * exit code. Stack traces are for bugs in this program, not for the user's files.
 */
public final class Main {
    // Exit codes from <sysexits.h>, same values as the book
    static final int EX_OK = 0;
    static final int EX_USAGE = 64;
    static final int EX_DATAERR = 65;
    static final int EX_NOINPUT = 66;
    static final int EX_IOERR = 74;

    private Main() {}

    public static void main(String[] args) {
        int code = switch (args.length) {
            case 0 -> runPrompt();
            case 1 -> runFile(args[0]);
            default -> {
                System.err.println("Usage: jlox [script]");
                yield EX_USAGE;
            }
        };
        if (code != EX_OK) {
            System.exit(code);
        }
    }

    /** Package-private so tests can call it without spawning a JVM. */
    static int runFile(String pathArg) {
        String source;
        try {
            source = Files.readString(Path.of(pathArg));
        } catch (NoSuchFileException e) {
            System.err.println("File not found: " + pathArg);
            return EX_NOINPUT;
        } catch (IOException | InvalidPathException e) {
            System.err.println("Cannot read " + pathArg + ": " + e.getMessage());
            return EX_IOERR;
        }
        RunResult result = Lox.run(source);
        report(result);
        return result.hasErrors() ? EX_DATAERR : EX_OK;
    }

    private static int runPrompt() {
        var in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (true) {
            System.out.print("> ");
            System.out.flush();
            String line;
            try {
                line = in.readLine();
            } catch (IOException e) {
                System.err.println("Cannot read input: " + e.getMessage());
                return EX_IOERR;
            }
            if (line == null) {
                System.out.println();
                return EX_OK;
            }
            report(Lox.run(line));
        }
    }

    private static void report(RunResult result) {
        result.tokens().forEach(System.out::println);
        result.diagnostics().forEach(System.err::println);
    }
}
