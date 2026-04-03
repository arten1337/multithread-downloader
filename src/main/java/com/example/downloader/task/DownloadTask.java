package com.example.downloader.task;

import com.example.downloader.core.ChunkRange;
import com.example.downloader.core.ChunkResult;
import com.example.downloader.progress.ProgressTracker;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.Callable;

public class DownloadTask implements Callable<ChunkResult> {

    private static final int BUFFER_SIZE = 8192;

    private final String url;
    private final Path outputPath;
    private final ChunkRange chunk;
    private final int chunkIndex;
    private final ProgressTracker tracker;

    public DownloadTask(String url, Path outputPath, ChunkRange chunk, int chunkIndex,
                        ProgressTracker tracker) {
        this.url = url;
        this.outputPath = outputPath;
        this.chunk = chunk;
        this.chunkIndex = chunkIndex;
        this.tracker = tracker;
    }

    @Override
    public ChunkResult call() {
        try {
            long start = chunk.getCurrentPosition();
            long end = chunk.getEnd();

            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(30_000);

            int code = conn.getResponseCode();
            if (code != 206 && code != 200) {
                conn.disconnect();
                return new ChunkResult(chunkIndex, false, "HTTP " + code);
            }

            try (InputStream in = conn.getInputStream();
                 RandomAccessFile raf = new RandomAccessFile(outputPath.toFile(), "rw")) {

                raf.seek(start);
                byte[] buffer = new byte[BUFFER_SIZE];
                long remaining = end - start + 1;
                int bytesRead;

                while (remaining > 0 && (bytesRead = in.read(buffer, 0,
                        (int) Math.min(buffer.length, remaining))) != -1) {
                    raf.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                    chunk.advance(bytesRead);
                    tracker.addBytes(bytesRead);
                }
            } finally {
                conn.disconnect();
            }

            chunk.markComplete();
            return new ChunkResult(chunkIndex, true, null);

        } catch (Exception e) {
            return new ChunkResult(chunkIndex, false, e.getMessage());
        }
    }

    /**
     * Fallback for servers that don't support range requests.
     */
    public void downloadWithoutRange() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(outputPath.toFile())) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                tracker.addBytes(bytesRead);
            }
        } finally {
            conn.disconnect();
        }
    }
}
