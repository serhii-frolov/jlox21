package io.github.serhiifrolov.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/** CLI entry point: owns args, I/O and the process exit code. All language logic lives in {@link Lox}. */
public final class Main {
    // Exit codes from <sysexits.h>, same values as the book
    private static final int EX_OK = 0;
    private static final int EX_USAGE = 64;
    private static final int EX_DATAERR = 65;
    private static final int EX_NOINPUT = 66;

    private Main() {}

    public static void main(String[] args) throws IOException {
        int code = switch (args.length) {
            case 0 -> runPrompt();
            case 1 -> runFile(Path.of(args[0]));
            default -> {
                System.err.println("Usage: jlox [script]");
                yield EX_USAGE;
            }
        };
        if (code != EX_OK) {
            System.exit(code);
        }
    }

    private static int runFile(Path path) throws IOException {
        String source;
        try {
            source = Files.readString(path); // UTF-8
        } catch (NoSuchFileException e) {
            System.err.println("File not found: " + path);
            return EX_NOINPUT;
        }
        RunResult result = Lox.run(source);
        report(result);
        return result.hasErrors() ? EX_DATAERR : EX_OK;
    }

    private static int runPrompt() throws IOException {
        var in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (true) {
            System.out.print("> ");
            System.out.flush();
            String line = in.readLine();
            if (line == null) { // Ctrl+D
                System.out.println();
                return EX_OK;
            }
            report(Lox.run(line)); // errors don't end the session
        }
    }

    private static void report(RunResult result) {
        result.tokens().forEach(System.out::println); // temporary: until the parser exists
        result.diagnostics().forEach(System.err::println);
    }
}
