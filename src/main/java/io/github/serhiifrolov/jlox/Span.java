package io.github.serhiifrolov.jlox;

/**
 * Source location of a token or diagnostic.
 *
 * <p>{@code offset} and {@code length} are in UTF-16 code units from the start of the source.
 * {@code line} and {@code column} are 1-based; a column counts code units, so a tab is one column
 * and a character outside the BMP is two. Only {@code '\n'} ends a line: {@code "\r\n"} counts as
 * one newline, a lone {@code '\r'} does not start a new line.
 */
public record Span(int offset, int length, int line, int column) {
    public Span {
        if (offset < 0 || length < 0 || line < 1 || column < 1) {
            throw new IllegalArgumentException(
                    "invalid span: offset=%d length=%d line=%d column=%d".formatted(offset, length, line, column));
        }
    }

    @Override
    public String toString() {
        return line + ":" + column;
    }
}
