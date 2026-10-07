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
one per run. `Lox.run` will refuse to interpret a program that has diagnostics (ch. 7), so
continuing to scan is safe.
Consuming the bad character is what prevents an infinite loop.

**Spans, not line numbers.** Every token and diagnostic carries `Span(offset, length, line,
column)`. A SAST finding is useless without an exact location: the user needs to jump to it,
and the tool needs to underline the whole lexeme. The book tracks only `line`; real tools track
offset+length and derive line/column lazily. I track both eagerly because the scanner already
knows them and the cost is two ints per token.

**Line/column are updated in exactly one place: `advance()`.** The book increments `line` in
two places (`scanToken` and `string`), and a block-comment scanner written the same way makes
three. I did it the book's way once by accident and every newline counted twice. One owner for position state, nothing else
touches it.

**Diagnostics instead of a global.** The book uses `Lox.error()` + a static `hadError` flag.
Here the scanner returns `List<Diagnostic>` alongside the tokens (`RunResult`). No global state
means each `Scanner` is independent, testable without capturing stderr, and usable from
anything other than the CLI.

**Unterminated string / comment errors point at the opening delimiter.** The book reports the
line where scanning gave up, i.e. end of file. For a 500-line file with an unclosed string on
line 12, that is the wrong line. `start` is still at the opening `"` or `/*` when the error is
raised, so the position is right for free. The diagnostic span is deliberately 1 or 2 chars
(just the opener), not opener-to-EOF: an IDE underlining 400 lines is correct and useless.

**Nested block comments need a counter (challenges 1 and 4).** A boolean `inComment` closes
`/* a /* b */ c */` at the first `*/` and lexes ` c */` as `IDENTIFIER STAR SLASH`. A depth
counter fixes it. This is also the answer to challenge 1: regular languages (regexes, DFAs)
cannot count, so a grammar with nesting is not regular. Haskell has the same feature (`{- -}`
nests). Python's INDENT/DEDENT is also non-regular but needs more than a counter: a stack of
indentation levels. Haskell's layout rule goes further still and needs feedback from the parser
(the `parse-error(t)` rule), so it is not a purely lexical problem.

**Why a scanner might keep comments and whitespace (challenge 3).** Formatters and
pretty-printers need them to round-trip source. Doc generators read doc comments. IDE
refactorings must not drop comments. For SAST specifically: comment-based suppressions
(`// NOSONAR`, `// fortify[suppress]`-style markers) and commented-out-code detection both need
the comments to survive scanning. Roslyn attaches "trivia" to tokens for this reason; the
TypeScript compiler keeps a full-start position per token and re-scans comments on demand. Not implemented here; candidate for a later PR.

**Unexpected characters: readable, not raw.** A control char, a format char (BOM), a non-ASCII
space or a lone surrogate prints as `U+XXXX`; anything visibly printable is quoted. A leading BOM
is skipped silently: every Windows editor emits one and a diagnostic the user cannot act on is
noise. A BOM anywhere else is an error.

**Deliberate gaps.**
- No escape sequences in strings: `"a\nb"` is a backslash and an `n`. Unescaping is where
  string lexing gets hard (`\u{...}`, raw strings, interpolation); deferred to phase 2.
- `char`-based, so a code point outside the BMP is two `char`s. The error path pairs a surrogate
  pair so one bad emoji is one diagnostic of length 2, but `isAlpha` and friends still see
  code units. Scanning by code point throughout is a phase 3 item.
- `isAlpha` is ASCII-only on purpose; `Character.isLetter` would accept identifiers Lox does
  not define.

**Divergences from the book, summary.** Records for `Token`/`Span`/`Diagnostic`; `Map.ofEntries`
keyword table; arrow-form `switch`; `Main`/`Lox` split so `Lox.run` has no I/O and no
`System.exit`; `getOrDefault` instead of `get` + null check.
