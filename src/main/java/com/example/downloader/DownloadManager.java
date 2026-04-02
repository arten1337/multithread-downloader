package com.example.downloader;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DownloadManager {

    private final String url;
    private final Path outputPath;
    private final int threadCount;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DownloadManager(String url, Path outputPath, int threadCount) {
        this.url = url;
        this.outputPath = outputPath;
        this.threadCount = threadCount;
    }

    public void download() throws Exception {
        long fileSize = getFileSize();
        boolean supportsRanges = fileSize > 0;

        if (!supportsRanges) {
            System.out.println("Server does not support range requests. Downloading with single thread...");
            singleThreadDownload();
            return;
        }

        System.out.printf("File size: %s%n", formatSize(fileSize));

        // Load or create download metadata for resume support
        DownloadMetadata metadata = loadOrCreateMetadata(fileSize);
        List<ChunkRange> chunks = metadata.getChunks();

        // Pre-allocate the output file
        try (RandomAccessFile raf = new RandomAccessFile(outputPath.toFile(), "rw")) {
            raf.setLength(fileSize);
        }

        ProgressTracker tracker = new ProgressTracker(fileSize, metadata.getTotalDownloaded());
        tracker.start();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<ChunkResult>> futures = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            ChunkRange chunk = chunks.get(i);
            if (chunk.isComplete()) {
                continue; // Skip already-completed chunks (resume)
            }
            DownloadTask task = new DownloadTask(url, outputPath, chunk, i, tracker);
            futures.add(executor.submit(task));
        }

        boolean allSuccess = true;
        for (Future<ChunkResult> future : futures) {
            ChunkResult result = future.get();
            if (!result.success()) {
                System.err.println("Chunk " + result.chunkIndex() + " failed: " + result.error());
                allSuccess = false;
            }
            // Save progress after each chunk completes
            saveMetadata(metadata);
        }

        executor.shutdown();
        tracker.stop();

        if (allSuccess) {
            // Clean up metadata file on success
            Files.deleteIfExists(metadataPath());
            System.out.println("\nDownload complete: " + outputPath);
        } else {
            System.err.println("\nSome chunks failed. Re-run the command to resume.");
        }
    }

    private long getFileSize() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("HEAD");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        try {
            int code = conn.getResponseCode();
            if (code >= 400) {
                throw new IOException("Server returned HTTP " + code);
            }
            String acceptRanges = conn.getHeaderField("Accept-Ranges");
            long length = conn.getContentLengthLong();
            if (!"bytes".equalsIgnoreCase(acceptRanges) || length <= 0) {
                return -1;
            }
            return length;
        } finally {
            conn.disconnect();
        }
    }

    private void singleThreadDownload() throws Exception {
        ProgressTracker tracker = new ProgressTracker(-1, 0);
        tracker.start();

        ChunkRange chunk = new ChunkRange(0, Long.MAX_VALUE);
        DownloadTask task = new DownloadTask(url, outputPath, chunk, 0, tracker);
        task.downloadWithoutRange();

        tracker.stop();
        System.out.println("\nDownload complete: " + outputPath);
    }

    private DownloadMetadata loadOrCreateMetadata(long fileSize) {
        Path metaPath = metadataPath();
        if (Files.exists(metaPath)) {
            try {
                DownloadMetadata meta = objectMapper.readValue(metaPath.toFile(), DownloadMetadata.class);
                if (meta.getFileSize() == fileSize && meta.getUrl().equals(url)) {
                    long alreadyDone = meta.getChunks().stream()
                            .filter(ChunkRange::isComplete)
                            .count();
                    System.out.printf("Resuming download (%d/%d chunks already complete)%n",
                            alreadyDone, meta.getChunks().size());
                    return meta;
                }
            } catch (IOException e) {
                // Corrupted metadata, start fresh
            }
        }

        List<ChunkRange> chunks = splitIntoChunks(fileSize);
        DownloadMetadata meta = new DownloadMetadata(url, fileSize, chunks);
        saveMetadata(meta);
        return meta;
    }

    private List<ChunkRange> splitIntoChunks(long fileSize) {
        List<ChunkRange> chunks = new ArrayList<>();
        long chunkSize = fileSize / threadCount;
        for (int i = 0; i < threadCount; i++) {
            long start = i * chunkSize;
            long end = (i == threadCount - 1) ? fileSize - 1 : (start + chunkSize - 1);
            chunks.add(new ChunkRange(start, end));
        }
        return chunks;
    }

    private void saveMetadata(DownloadMetadata metadata) {
        try {
            objectMapper.writeValue(metadataPath().toFile(), metadata);
        } catch (IOException e) {
            System.err.println("Warning: could not save progress metadata: " + e.getMessage());
        }
    }

    private Path metadataPath() {
        return outputPath.resolveSibling(outputPath.getFileName() + ".mtdl");
    }

    static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        String[] units = {"KB", "MB", "GB", "TB"};
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        exp = Math.min(exp, units.length);
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), units[exp - 1]);
    }
}
