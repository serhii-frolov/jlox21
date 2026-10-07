# Notes

Working notes per chapter. Written for future me and for anyone reviewing the repo.

## Scanner (Crafting Interpreters ch. 4)

**Maximal munch.** When two lexical rules both match at the current position, the one that
consumes more characters wins. `<=` is one token, not `<` then `=`; `orchid` is an identifier,
not `or` + `chid`. Consequence: keywords can only be recognised *after* the whole identifier has
been consumed, by looking the lexeme up in a table. Trying to match keywords character by
character (`case 'o': if (match('r'))`) breaks on any identifier that starts with a keyword.

**Keep scanning after an error.** An unexpected character becomes a diagnostic, the character is
consumed, and the loop continues. One run reports every lexical error in the file instead of
one per run. The parser later refuses to execute code with diagnostics, so continuing is safe.
Consuming the bad character is what prevents an infinite loop.

**Spans, not line numbers.** Every token and diagnostic carries `Span(offset, length, line,
column)`. A SAST finding is useless without an exact location: the user needs to jump to it,
and the tool needs to underline the whole lexeme. The book tracks only `line`; real tools track
offset+length and derive line/column lazily. I track both eagerly because the scanner already
knows them and the cost is two ints per token.

**Line/column are updated in exactly one place: `advance()`.** The book increments `line` in
three places (`scanToken`, `string`, and challenge 4's block comment). I did it the book's way
once by accident and every newline counted twice. One owner for position state, nothing else
touches it.

**Diagnostics instead of a global.** The book uses `Lox.error()` + a static `hadError` flag.
Here the scanner returns `List<Diagnostic>` alongside the tokens (`RunResult`). No global state
means each `Scanner` is independent, testable without capturing stderr, and usable from
anything other than the CLI.

**Unterminated string / comment errors point at the opening delimiter.** The book reports the
line where scanning gave up, i.e. end of file. For a 500-line file with an unclosed string on
line 12, that is the wrong line. `start` is still at the opening `"` or `/*` when the error is
raised, so the span is right for free.

**Nested block comments need a counter (challenges 1 and 4).** A boolean `inComment` closes
`/* a /* b */ c */` at the first `*/` and lexes ` c */` as `IDENTIFIER STAR SLASH`. A depth
counter fixes it. This is also the answer to challenge 1: regular languages (regexes, DFAs)
cannot count, so a grammar with nesting is not regular. Python's INDENT/DEDENT and Haskell's
layout rule are the same problem: they need a stack of indentation levels.

**Why a scanner might keep comments and whitespace (challenge 3).** Formatters and
pretty-printers need them to round-trip source. Doc generators read doc comments. IDE
refactorings must not drop comments. For SAST specifically: suppression pragmas
(`// NOSONAR`, `#pragma`, `@SuppressWarnings`-style markers in comments) and commented-out-code
detection both need the comments to survive scanning. Roslyn and the TypeScript compiler attach
"trivia" to tokens for this reason. Not implemented here; candidate for a later PR.

**Deliberate gaps.**
- No escape sequences in strings: `"a\nb"` is a backslash and an `n`. Unescaping is where
  string lexing gets hard (`\u{...}`, raw strings, interpolation); deferred to phase 2.
- `char`-based, so a code point outside the BMP is two `char`s. `describe()` prints the UTF-16
  code unit. Switching to code points is a phase 3 item.
- `isAlpha` is ASCII-only on purpose; `Character.isLetter` would accept identifiers Lox does
  not define.

**Divergences from the book, summary.** Records for `Token`/`Span`/`Diagnostic`; `Map.ofEntries`
keyword table; arrow-form `switch`; `Main`/`Lox` split so `Lox.run` has no I/O and no
`System.exit`; `getOrDefault` instead of `get` + null check.
