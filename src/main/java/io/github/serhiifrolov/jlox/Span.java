package io.github.serhiifrolov.jlox;

/**
 * Source location. {@code offset}/{@code length} in chars, {@code line}/{@code column} 1-based.
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
