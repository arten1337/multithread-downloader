# Multithread Downloader

A fast, concurrent HTTP file downloader written in Java 21. It splits files into chunks and downloads them in parallel using HTTP Range requests, with built-in resume support and a real-time progress bar.

## Features

- **Multithreaded downloads** -- splits files into chunks downloaded concurrently via a thread pool
- **HTTP Range requests** -- each thread downloads its own byte range for true parallelism
- **Resume support** -- progress is persisted to a `.mtdl` metadata file; interrupted downloads pick up where they left off
- **Real-time progress bar** -- displays percentage, transfer speed, and ETA in the terminal
- **Single-thread fallback** -- gracefully handles servers that don't advertise `Accept-Ranges: bytes`
- **Fat JAR packaging** -- produces a single runnable JAR via Maven Shade

## Requirements

- **Java 21+**
- **Maven 3.8+**

## Quick Start

### Build

```bash
mvn clean package
```

This produces `target/multithread-downloader-1.0-SNAPSHOT.jar`.

### Run

```bash
java -jar target/multithread-downloader-1.0-SNAPSHOT.jar <URL> [outputFile] [threads]
```

| Argument     | Required | Default                        | Description                         |
|--------------|----------|--------------------------------|-------------------------------------|
| `URL`        | Yes      | --                             | The HTTP/HTTPS URL to download      |
| `outputFile` | No       | Derived from URL path          | Local file path for the download    |
| `threads`    | No       | `4`                            | Number of concurrent download threads |

### Examples

```bash
# Download with defaults (4 threads, filename from URL)
java -jar target/multithread-downloader-1.0-SNAPSHOT.jar https://example.com/large-file.zip

# Download to a specific file with 8 threads
java -jar target/multithread-downloader-1.0-SNAPSHOT.jar https://example.com/large-file.zip myfile.zip 8
```

### Resume an interrupted download

Simply re-run the same command. The downloader detects the `.mtdl` metadata file and skips already-completed chunks:

```
Resuming download (2/4 chunks already complete)
```

## Project Structure

```
src/main/java/com/example/downloader/
├── App.java                        # CLI entry point, argument parsing
├── core/
│   ├── ChunkRange.java             # Byte range tracking for a single chunk
│   ├── ChunkResult.java            # Result record (success/failure per chunk)
│   ├── DownloadManager.java        # Orchestrator: probes server, splits file, manages threads
│   └── DownloadMetadata.java       # JSON-serializable state for resume support
├── task/
│   └── DownloadTask.java           # Callable worker that downloads a byte range
├── progress/
│   └── ProgressTracker.java        # Real-time progress bar with speed and ETA
└── util/
    └── FormatUtils.java            # Size and time formatting helpers
```

### Package responsibilities

| Package    | Role |
|------------|------|
| `core`     | Domain model and download orchestration |
| `task`     | Threaded download workers |
| `progress` | Terminal progress display |
| `util`     | Shared formatting utilities |

## How It Works

1. **Probe** -- sends an HTTP `HEAD` request to determine the file size and whether the server supports range requests.
2. **Plan** -- splits the file into _N_ equal byte ranges (one per thread) and persists the plan to a `.mtdl` JSON file.
3. **Download** -- submits each chunk as a `Callable` to a fixed thread pool. Each task opens its own HTTP connection with a `Range` header and writes directly to the correct offset in the output file using `RandomAccessFile`.
4. **Track** -- a daemon thread refreshes the progress bar every 500ms, aggregating bytes from all workers via an `AtomicLong`.
5. **Complete** -- once all chunks succeed, the `.mtdl` metadata file is deleted. If any chunk fails, the metadata is preserved so the next run can resume.

## Dependencies

| Library          | Version | Purpose                          |
|------------------|---------|----------------------------------|
| Jackson Databind | 2.15.2  | JSON serialization for metadata  |
| JUnit Jupiter    | 5.10.0  | Unit testing (test scope)        |

## License

This project is provided as-is for educational and personal use.
