# jlox21

Lox interpreter ([Crafting Interpreters](https://craftinginterpreters.com/)) in modern Java 21: sealed AST, source spans, error recovery.

## Build

```bash
./gradlew build   # compile + tests
./gradlew test    # tests only
```

Requires any JDK 17+ to run Gradle; the JDK 21 toolchain is downloaded automatically if missing.
