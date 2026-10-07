package io.github.serhiifrolov.jlox;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.serhiifrolov.jlox.lexer.Token;
import io.github.serhiifrolov.jlox.lexer.TokenType;
import org.junit.jupiter.api.Test;

class LoxTest {

    @Test
    void emptySourceProducesOnlyEof() {
        RunResult result = Lox.run("");

        assertThat(result.tokens()).extracting(Token::type).containsExactly(TokenType.EOF);
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void resultIsImmutable() {
        RunResult result = Lox.run("");

        assertThat(result.tokens()).isUnmodifiable();
        assertThat(result.diagnostics()).isUnmodifiable();
    }
}
