package io.github.serhiifrolov.jlox.lexer;

import static io.github.serhiifrolov.jlox.lexer.TokenType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.serhiifrolov.jlox.Diagnostic;
import io.github.serhiifrolov.jlox.Span;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Spec for the scanner, Crafting Interpreters ch. 4 + challenge 4. Make these green. */
class ScannerTest {

    private static List<Token> tokens(String source) {
        return new Scanner(source).scanTokens();
    }

    private static List<TokenType> types(String source) {
        return tokens(source).stream().map(Token::type).toList();
    }

    private static List<Diagnostic> diagnostics(String source) {
        var scanner = new Scanner(source);
        scanner.scanTokens();
        return scanner.diagnostics();
    }

    @Nested
    class Basics {
        @Test
        void emptySourceGivesOnlyEof() {
            assertThat(tokens("")).containsExactly(new Token(EOF, "", null, new Span(0, 0, 1, 1)));
        }

        @Test
        void singleCharacterTokens() {
            assertThat(types("(){},.-+;*/")).containsExactly(
                    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
                    COMMA, DOT, MINUS, PLUS, SEMICOLON, STAR, SLASH, EOF);
        }

        @Test
        void oneOrTwoCharacterOperators() {
            assertThat(types("! != = == < <= > >=")).containsExactly(
                    BANG, BANG_EQUAL, EQUAL, EQUAL_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, EOF);
        }

        @Test
        void maximalMunchOnOperators() {
            assertThat(types("!==")).containsExactly(BANG_EQUAL, EQUAL, EOF);
            assertThat(types("<==")).containsExactly(LESS_EQUAL, EQUAL, EOF);
        }

        @Test
        void whitespaceIsIgnored() {
            assertThat(types(" \t\r\n ( \n )  ")).containsExactly(LEFT_PAREN, RIGHT_PAREN, EOF);
        }
    }

    @Nested
    class Comments {
        @Test
        void lineCommentRunsToEndOfLine() {
            assertThat(types("( // comment ) }\n)")).containsExactly(LEFT_PAREN, RIGHT_PAREN, EOF);
        }

        @Test
        void lineCommentAtEndOfFileWithoutNewline() {
            assertThat(types("( // trailing")).containsExactly(LEFT_PAREN, EOF);
        }

        @Test
        void blockComment() {
            assertThat(types("/* a\nb */ x")).containsExactly(IDENTIFIER, EOF);
        }

        @Test
        void blockCommentsNest() {
            assertThat(types("/* a /* b */ c */ x")).containsExactly(IDENTIFIER, EOF);
            assertThat(diagnostics("/* a /* b */ c */ x")).isEmpty();
        }

        @Test
        void blockCommentCountsNewlines() {
            Token x = tokens("/*\n\n*/ x").getFirst();
            assertThat(x.span()).isEqualTo(new Span(7, 1, 3, 4));
        }

        @Test
        void unterminatedBlockComment() {
            assertThat(types("x /* never closed")).containsExactly(IDENTIFIER, EOF);
            assertThat(diagnostics("x /* never closed")).singleElement()
                    .satisfies(d -> {
                        assertThat(d.message()).containsIgnoringCase("unterminated");
                        assertThat(d.span().line()).isEqualTo(1);
                        assertThat(d.span().column()).isEqualTo(3);
                    });
        }
    }

    @Nested
    class Strings {
        @Test
        void stringLiteralHasValueWithoutQuotes() {
            Token s = tokens("\"hello\"").getFirst();
            assertThat(s.type()).isEqualTo(STRING);
            assertThat(s.lexeme()).isEqualTo("\"hello\"");
            assertThat(s.literal()).isEqualTo("hello");
        }

        @Test
        void stringsMaySpanLinesAndPositionsStayCorrect() {
            List<Token> ts = tokens("\"a\nb\" x");
            assertThat(ts).extracting(Token::type).containsExactly(STRING, IDENTIFIER, EOF);
            assertThat(ts.get(0).literal()).isEqualTo("a\nb");
            assertThat(ts.get(1).span()).isEqualTo(new Span(6, 1, 2, 4));
        }

        @Test
        void unterminatedStringIsReportedAtOpeningQuote() {
            assertThat(types("x \"oops")).containsExactly(IDENTIFIER, EOF);
            assertThat(diagnostics("x \"oops")).singleElement()
                    .satisfies(d -> {
                        assertThat(d.message()).containsIgnoringCase("unterminated");
                        assertThat(d.span().line()).isEqualTo(1);
                        assertThat(d.span().column()).isEqualTo(3);
                    });
        }
    }

    @Nested
    class Numbers {
        @ParameterizedTest
        @CsvSource({"0, 0.0", "123, 123.0", "12.5, 12.5", "007, 7.0"})
        void numberLiterals(String source, double expected) {
            Token n = tokens(source).getFirst();
            assertThat(n.type()).isEqualTo(NUMBER);
            assertThat(n.literal()).isEqualTo(expected);
        }

        @Test
        void trailingDotIsNotPartOfNumber() {
            assertThat(types("12.")).containsExactly(NUMBER, DOT, EOF);
        }

        @Test
        void leadingDotIsNotANumber() {
            assertThat(types(".5")).containsExactly(DOT, NUMBER, EOF);
        }

        @Test
        void minusIsNotPartOfNumber() {
            assertThat(types("-1")).containsExactly(MINUS, NUMBER, EOF);
        }
    }

    @Nested
    class IdentifiersAndKeywords {
        static Stream<Arguments> keywords() {
            return Stream.of(
                    Arguments.of("and", AND), Arguments.of("class", CLASS), Arguments.of("else", ELSE),
                    Arguments.of("false", FALSE), Arguments.of("for", FOR), Arguments.of("fun", FUN),
                    Arguments.of("if", IF), Arguments.of("nil", NIL), Arguments.of("or", OR),
                    Arguments.of("print", PRINT), Arguments.of("return", RETURN), Arguments.of("super", SUPER),
                    Arguments.of("this", THIS), Arguments.of("true", TRUE), Arguments.of("var", VAR),
                    Arguments.of("while", WHILE));
        }

        @ParameterizedTest
        @MethodSource("keywords")
        void everyKeyword(String source, TokenType expected) {
            assertThat(types(source)).containsExactly(expected, EOF);
        }

        @Test
        void maximalMunchOnIdentifiers() {
            assertThat(types("orchid or nil_x _a a1 Or")).containsExactly(
                    IDENTIFIER, OR, IDENTIFIER, IDENTIFIER, IDENTIFIER, IDENTIFIER, EOF);
        }

        @Test
        void identifierLexeme() {
            assertThat(tokens("foo_bar9").getFirst().lexeme()).isEqualTo("foo_bar9");
        }
    }

    @Nested
    class Spans {
        @Test
        void offsetLengthLineColumn() {
            List<Token> ts = tokens("(\n  )");
            assertThat(ts.get(0).span()).isEqualTo(new Span(0, 1, 1, 1));
            assertThat(ts.get(1).span()).isEqualTo(new Span(4, 1, 2, 3));
            assertThat(ts.get(2).span()).isEqualTo(new Span(5, 0, 2, 4)); // EOF
        }

        @Test
        void multiCharTokenLength() {
            assertThat(tokens("  <=").getFirst().span()).isEqualTo(new Span(2, 2, 1, 3));
        }

        @Test
        void columnResetsAfterNewline() {
            assertThat(tokens("ab\nc").get(1).span()).isEqualTo(new Span(3, 1, 2, 1));
        }
    }

    @Nested
    class Errors {
        @Test
        void unexpectedCharactersAreReportedAndScanningContinues() {
            String src = "a @ b # c";
            assertThatCode(() -> tokens(src)).doesNotThrowAnyException();
            assertThat(types(src)).containsExactly(IDENTIFIER, IDENTIFIER, IDENTIFIER, EOF);
            assertThat(diagnostics(src)).extracting(d -> d.span().column()).containsExactly(3, 7);
        }

        @Test
        void validSourceHasNoDiagnostics() {
            assertThat(diagnostics("var x = 1 + 2; // ok")).isEmpty();
        }
    }
}
