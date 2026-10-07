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
three. I added the book's `line++` on top of `advance()`'s once by accident and every newline counted twice. One owner for position state, nothing else
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
cannot track unbounded nesting depth, so a grammar with nesting is not regular. Haskell has the same feature (`{- -}`
nests). Python's INDENT/DEDENT is also non-regular but needs more than a counter: a stack of
indentation levels. Haskell's layout rule goes further still and needs feedback from the parser
(the `parse-error(t)` rule), so it is not a purely lexical problem.

**Why a scanner might keep comments and whitespace (challenge 3).** Formatters and
pretty-printers need them to round-trip source. Doc generators read doc comments. IDE
refactorings must not drop comments. For SAST specifically: comment-based suppressions
(`// NOSONAR`, `# noqa`, `// nosemgrep`, `// eslint-disable-line`) and commented-out-code detection both need
the comments to survive scanning. Roslyn attaches "trivia" to tokens for this reason; the
TypeScript compiler keeps a full-start position per token and re-scans comments on demand. Not implemented here; candidate for a later PR.

**Unexpected characters: readable, not raw.** A letter, digit, punctuation or symbol is quoted
and named, `'@' (U+0040)`; everything else (controls, separators, format chars such as the BOM,
combining marks, private use, unassigned, lone surrogates) is named only, `U+XXXX`. The code
point is always there so a message is searchable and survives a terminal without the glyph. An
allow-list of visible categories, not a deny-list: the first version deny-listed and missed
U+2028. A leading BOM is skipped and takes no column, so line-1 columns match what the editor
shows; every Windows editor emits one and a diagnostic the user cannot act on is noise. A BOM
anywhere else is an error.

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

## AST (Crafting Interpreters ch. 5)

**What a syntax tree is for.** The scanner turns characters into a flat token list; the
parser (ch. 6) will turn tokens into a tree whose shape encodes precedence and grouping.
This chapter only defines the tree. `1 + 2 * 3` and `(1 + 2) * 3` have the same tokens
minus two parens but different trees; everything downstream (printer, interpreter, later a
dataflow analysis) consumes the tree and never looks at tokens again.

**Sealed interface + records instead of Visitor + GenerateAst.** The book writes a small
code generator (`GenerateAst`) that emits an abstract `Expr` with a nested class per node
type and a `Visitor<R>` interface. That is the 2015 answer to a 2015 Java problem: no cheap
way to define an immutable data class, and no way to switch over a closed set of types.
Java 21 is the first release where all three answers are final: records (16, JEP 395),
sealed types (17, JEP 409), pattern matching for `switch` (21, JEP 441). `sealed interface Expr permits Binary, Grouping, Literal, Unary` closes
the set; each node is a `record` (constructor, accessors, `equals`/`hashCode`/`toString`
for free); and `switch (expr) { case Expr.Binary b -> ... }` is checked for exhaustiveness
by the compiler. Add a fifth node type and every switch that forgets it stops compiling —
the same guarantee Visitor gave, without the double-dispatch ceremony or the generator — with
one caveat: Visitor's guarantee is unconditional, the switch's holds only while nobody adds a
`default` branch. No `default` in AST switches, ever. `permits` is redundant for nested types
in the same file; it is kept as documentation of the closed set.

**The expression problem.** Visitor and pattern matching are two answers to the same
tension. Rows are node types, columns are operations (print, evaluate, resolve). An OO
hierarchy with a method per operation makes adding a *row* easy (new subclass) and adding a
*column* hard (touch every class). Visitor flips it: a new operation is one new class, a new
node type touches every visitor. Pattern matching over a sealed type sits with Visitor:
operations are free-standing functions, node types are the fixed axis. For a language
implementation that is the right trade — the grammar changes rarely, the number of passes
over it grows for years. A SAST engine is an extreme case: one AST per language, dozens of
passes (translation to an IR, control flow, dataflow, taint rules).

**Why `Span` is on every node.** Each record carries a `Span` and the interface demands it
(`Span span()`), so any pass can report a location without re-walking children. The
parser will set it from the first token's start to the last token's end. For a printer it
is dead weight; for a diagnostic it is the whole point. This is the same decision as the
scanner's `Span` on every token, pushed one level up. IntelliJ flags `span()` as unused
until ch. 6 — correct and expected.

**`Literal` holds `Object`.** The book does the same. The alternative is one record per
literal kind (`NumberLiteral(double)`, `StringLiteral(String)`, `BoolLiteral`, `NilLiteral`)
which is cleaner for a typed language but inflates the switch in every pass for no gain in
Lox, where values are dynamically typed anyway. Revisit if the interpreter's type checks get
noisy. `nil` is a Java `null` inside `Literal` — the only `null` the tree permits; every other
component is checked in a compact constructor, so a pass never has to null-check a child.

**Printers as the first pass.** `AstPrinter` prints Lisp-style, `(* (- 123.0) (group 45.67))`;
it exists to make parser tests readable, not for users. `RpnPrinter` (challenge 3) prints
postfix, `1.0 2.0 + 4.0 3.0 - *`. Grouping vanishes in RPN because tree shape already encodes
order — the parens were only ever an instruction to the parser. `Grouping` still has to exist as a
node: in ch. 8 `(a) = 1` must be rejected as an invalid assignment target, which only works if
the parser can see the parens were there. Unary minus is printed as
`~`: in postfix `-` already means binary subtraction, and `1 2 -` vs `1 ~` must stay
distinguishable. The book accepts any choice; this one is documented so the next reader does
not reopen it. String literals print in quotes in both printers, otherwise `"nil"` and `nil`,
or `"a b"` and two operands, are indistinguishable — found in review, not by me.

**Deliberate gaps.**
- `Expr` only. Statements come in ch. 8 as a second sealed interface `Stmt`.
- No `Visitor`, no `GenerateAst`, no `accept()`; if a pass ever needs state threaded
  through the walk, a class with a `switch` in a method does that without an interface.
- Spans are placeholders in tests (`Span(0,0,1,1)`); real values arrive with the parser.
- Printers recurse; a tree ~10k levels deep overflows the stack. Fine for a debug tool.
- Record `equals` includes the span, so two structurally equal trees from different source
  positions are not equal. Parser tests compare printed output, not trees.

**Divergences from the book, summary.** Sealed interface + records for the node types;
pattern-matching `switch` in place of Visitor; no code generator; `Span` field on every
node; printers are final utility classes with a private constructor.
