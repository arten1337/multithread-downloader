# Contributing

Thanks for your interest in contributing to Multithread Downloader!

## Prerequisites

- **Java 21** -- verify with `java --version`
- **Maven 3.8+** -- verify with `mvn --version`

## Building

```bash
# Compile only
mvn compile

# Compile + run tests
mvn test

# Full build (compile, test, package fat JAR)
mvn clean package
```

The fat JAR is output to `target/multithread-downloader-1.0-SNAPSHOT.jar`.

## Running locally

```bash
java -jar target/multithread-downloader-1.0-SNAPSHOT.jar <URL> [outputFile] [threads]
```

## Project layout

```
multithread-downloader/
├── pom.xml                          # Maven build configuration
├── README.md                        # User-facing documentation
├── CONTRIBUTING.md                  # This file
├── .gitignore
└── src/
    └── main/java/com/example/downloader/
        ├── App.java                 # Entry point
        ├── core/                    # Domain model + orchestration
        ├── task/                    # Download workers
        ├── progress/                # Progress display
        └── util/                    # Shared helpers
```

## Code conventions

- **Java 21** features are welcome (records, pattern matching, etc.)
- Keep classes focused -- one responsibility per class
- Use `synchronized` or `java.util.concurrent` primitives for shared mutable state
- No external HTTP client libraries -- the project uses `java.net.HttpURLConnection` intentionally to stay dependency-light

## Adding a new feature

1. Create a feature branch from `main`
2. Add code in the appropriate package (`core`, `task`, `progress`, or `util`)
3. Write unit tests under `src/test/java` using JUnit 5
4. Ensure `mvn clean package` passes
5. Open a pull request with a clear description

## Reporting issues

Open a GitHub issue with:
- Steps to reproduce
- Expected vs. actual behavior
- Java version and OS
