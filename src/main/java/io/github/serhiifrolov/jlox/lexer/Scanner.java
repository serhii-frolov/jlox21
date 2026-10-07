package io.github.serhiifrolov.jlox.lexer;

import static io.github.serhiifrolov.jlox.lexer.TokenType.*;
import static java.util.Map.entry;

import io.github.serhiifrolov.jlox.Diagnostic;
import io.github.serhiifrolov.jlox.Span;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Turns Lox source text into tokens (Crafting Interpreters ch. 4).
 *
 * <p>Contract: never throws on bad input. Problems are recorded as {@link Diagnostic}s and scanning
 * continues, so one run reports every lexical error in the file.
 *
 * <p>Lookahead is at most two characters ({@link #peek()} and {@link #peekNext()}). A leading byte
 * order mark (U+FEFF) is skipped; it is not part of the language but most Windows editors emit it.
 */
public final class Scanner {

    private static final char BOM = '﻿';

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            entry("and", AND),
            entry("class", CLASS),
            entry("else", ELSE),
            entry("false", FALSE),
            entry("for", FOR),
            entry("fun", FUN),
            entry("if", IF),
            entry("nil", NIL),
            entry("or", OR),
            entry("print", PRINT),
            entry("return", RETURN),
            entry("super", SUPER),
            entry("this", THIS),
            entry("true", TRUE),
            entry("var", VAR),
            entry("while", WHILE));

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    // Where the current lexeme starts (captured at the top of each scanToken() turn)
    private int start = 0;
    private int startLine = 1;
    private int startColumn = 1;

    // Where the scanner is now
    private int current = 0;
    private int line = 1;
    private int column = 1;

    public Scanner(String source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    public List<Token> scanTokens() {
        if (peek() == BOM) {
            current++; // not advance(): editors do not show the BOM, so it must not take a column
        }
        while (!isAtEnd()) {
            start = current;
            startLine = line;
            startColumn = column;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, new Span(current, 0, line, column)));
        return List.copyOf(tokens);
    }

    public List<Diagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }

    /** Scans exactly one lexeme starting at {@code start}. */
    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(' -> addToken(LEFT_PAREN);
            case ')' -> addToken(RIGHT_PAREN);
            case '{' -> addToken(LEFT_BRACE);
            case '}' -> addToken(RIGHT_BRACE);
            case ',' -> addToken(COMMA);
            case '.' -> addToken(DOT);
            case '-' -> addToken(MINUS);
            case '+' -> addToken(PLUS);
            case ';' -> addToken(SEMICOLON);
            case '*' -> addToken(STAR);

            case '!' -> addToken(match('=') ? BANG_EQUAL : BANG);
            case '=' -> addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            case '<' -> addToken(match('=') ? LESS_EQUAL : LESS);
            case '>' -> addToken(match('=') ? GREATER_EQUAL : GREATER);

            case ' ', '\r', '\t', '\n' -> { }

            case '"' -> string();

            case '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else if (match('*')) {
                    blockComment();
                } else {
                    addToken(SLASH);
                }
            }

            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    unexpectedCharacter(c);
                }
            }
        }
    }

    /** Ch. 4.6.1. Strings may span lines. Unterminated → error anchored at the opening quote. */
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            advance();
        }

        if (isAtEnd()) {
            error("Unterminated string.", 1);
            return;
        }

        advance();

        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    /** Ch. 4.6.2. Digits, optional '.' + digits. Neither ".5" nor "5." is a number. */
    private void number() {
        while (isDigit(peek())) {
            advance();
        }
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) {
                advance();
            }
        }
        // Cannot throw: the lexeme is digits with at most one interior dot.
        addToken(NUMBER, Double.parseDouble(lexeme()));
    }

    /** Ch. 4.7. Maximal munch, then look the lexeme up in {@link #KEYWORDS}. */
    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        addToken(KEYWORDS.getOrDefault(lexeme(), IDENTIFIER));
    }

    /** Challenge 4. Nesting allowed. Unterminated → error anchored at the opening slash-star. */
    private void blockComment() {
        int depth = 1;
        while (depth > 0 && !isAtEnd()) {
            if (peek() == '/' && peekNext() == '*') {
                advance();
                advance();
                depth++;
            } else if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                depth--;
            } else {
                advance();
            }
        }
        if (depth > 0) {
            error("Unterminated block comment.", 2);
        }
    }

    /**
     * Reports a character Lox has no use for. A surrogate pair is consumed whole so the diagnostic
     * names one code point with length 2 instead of two half-characters.
     */
    private void unexpectedCharacter(char c) {
        if (Character.isHighSurrogate(c) && Character.isLowSurrogate(peek())) {
            advance();
        }
        error("Unexpected character " + describe(lexeme()) + ".");
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    /** Consumes one char and keeps line/column in sync. The only place that moves {@code current} forward. */
    private char advance() {
        char c = source.charAt(current++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    /** Current unconsumed char, or '\0' at end. One char of lookahead. */
    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(current);
    }

    /** The char after {@link #peek()}, or '\0'. Two chars of lookahead; the maximum we allow. */
    private char peekNext() {
        return current + 1 >= source.length() ? '\0' : source.charAt(current + 1);
    }

    /** Conditional advance: consumes the next char only if it is {@code expected}. */
    private boolean match(char expected) {
        if (isAtEnd() || source.charAt(current) != expected) {
            return false;
        }
        advance();
        return true;
    }

    // ASCII only on purpose: Character.isDigit/isLetter accept Unicode digits and letters we don't want.
    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    /**
     * Human-readable form of a lexeme for diagnostics. A visible character is quoted and named,
     * {@code 'x' (U+0078)}; anything else (controls, separators, format chars such as the BOM,
     * marks, unassigned, private use, lone surrogates) is named only, {@code U+XXXX}. The code
     * point is always present so the message is searchable and survives fonts that lack the glyph.
     * Takes the lexeme rather than a char so a surrogate pair prints as one code point.
     */
    private static String describe(String lexeme) {
        int cp = lexeme.codePointAt(0);
        String name = "U+%04X".formatted(cp);
        return isVisible(cp) ? "'" + lexeme + "' (" + name + ")" : name;
    }

    private static boolean isVisible(int cp) {
        if (cp == ' ') {
            return true;
        }
        return switch (Character.getType(cp)) {
            case Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER,
                 Character.MODIFIER_LETTER, Character.OTHER_LETTER,
                 Character.DECIMAL_DIGIT_NUMBER, Character.LETTER_NUMBER, Character.OTHER_NUMBER,
                 Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION,
                 Character.START_PUNCTUATION, Character.END_PUNCTUATION,
                 Character.INITIAL_QUOTE_PUNCTUATION, Character.FINAL_QUOTE_PUNCTUATION,
                 Character.OTHER_PUNCTUATION,
                 Character.MATH_SYMBOL, Character.CURRENCY_SYMBOL, Character.MODIFIER_SYMBOL,
                 Character.OTHER_SYMBOL -> true;
            default -> false;
        };
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        tokens.add(new Token(type, lexeme(), literal, currentSpan()));
    }

    /** Records a diagnostic covering the current lexeme (start..current). Scanning continues. */
    private void error(String message) {
        diagnostics.add(new Diagnostic(message, currentSpan()));
    }

    /** Records a diagnostic covering only the first {@code length} chars of the current lexeme. */
    private void error(String message, int length) {
        assert length <= current - start : "diagnostic longer than lexeme";
        diagnostics.add(new Diagnostic(message, new Span(start, length, startLine, startColumn)));
    }

    private String lexeme() {
        return source.substring(start, current);
    }

    private Span currentSpan() {
        return new Span(start, current - start, startLine, startColumn);
    }
}
