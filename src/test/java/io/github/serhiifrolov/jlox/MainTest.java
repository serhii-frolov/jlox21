package io.github.serhiifrolov.jlox;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exit codes only; output formatting is not pinned yet. */
class MainTest {

    @Test
    void validFileExitsOk(@TempDir Path dir) throws IOException {
        Path f = Files.writeString(dir.resolve("ok.lox"), "var x = 1;");
        assertThat(Main.runFile(f.toString())).isEqualTo(Main.EX_OK);
    }

    @Test
    void fileWithLexicalErrorExitsDataErr(@TempDir Path dir) throws IOException {
        Path f = Files.writeString(dir.resolve("bad.lox"), "var x = @;");
        assertThat(Main.runFile(f.toString())).isEqualTo(Main.EX_DATAERR);
    }

    @Test
    void missingFileExitsNoInput(@TempDir Path dir) {
        assertThat(Main.runFile(dir.resolve("nope.lox").toString())).isEqualTo(Main.EX_NOINPUT);
    }

    @Test
    void nonUtf8FileExitsIoErr(@TempDir Path dir) throws IOException {
        // "é" in Latin-1 is 0xE9, which is not valid UTF-8
        Path f = Files.write(dir.resolve("latin1.lox"), "\"café\"".getBytes(StandardCharsets.ISO_8859_1));
        assertThat(Main.runFile(f.toString())).isEqualTo(Main.EX_IOERR);
    }

    @Test
    void directoryExitsIoErr(@TempDir Path dir) {
        assertThat(Main.runFile(dir.toString())).isEqualTo(Main.EX_IOERR);
    }

    @Test
    void nulInPathExitsIoErr() {
        // NUL is invalid in a path on every platform; Path.of throws InvalidPathException
        assertThat(Main.runFile("a\0b")).isEqualTo(Main.EX_IOERR);
    }
}
