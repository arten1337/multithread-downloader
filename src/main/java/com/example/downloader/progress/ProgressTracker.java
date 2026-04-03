package com.example.downloader.progress;

import com.example.downloader.util.FormatUtils;

import java.util.concurrent.atomic.AtomicLong;

public class ProgressTracker {

    private final long totalSize;
    private final AtomicLong downloaded;
    private final long startTime;
    private volatile boolean running;
    private Thread displayThread;

    public ProgressTracker(long totalSize, long alreadyDownloaded) {
        this.totalSize = totalSize;
        this.downloaded = new AtomicLong(alreadyDownloaded);
        this.startTime = System.currentTimeMillis();
    }

    public void addBytes(long bytes) {
        downloaded.addAndGet(bytes);
    }

    public void start() {
        running = true;
        displayThread = new Thread(() -> {
            while (running) {
                printProgress();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "progress-display");
        displayThread.setDaemon(true);
        displayThread.start();
    }

    public void stop() {
        running = false;
        if (displayThread != null) {
            displayThread.interrupt();
        }
        printProgress();
        System.out.println();
    }

    private void printProgress() {
        long current = downloaded.get();
        long elapsed = System.currentTimeMillis() - startTime;
        double speed = elapsed > 0 ? (current * 1000.0) / elapsed : 0;

        if (totalSize > 0) {
            double percent = (current * 100.0) / totalSize;
            int barWidth = 40;
            int filled = (int) (percent / 100 * barWidth);

            StringBuilder bar = new StringBuilder("[");
            for (int i = 0; i < barWidth; i++) {
                bar.append(i < filled ? '=' : (i == filled ? '>' : ' '));
            }
            bar.append(']');

            String eta = "N/A";
            if (speed > 0) {
                long remaining = (long) ((totalSize - current) / speed);
                eta = FormatUtils.formatTime(remaining);
            }

            System.out.printf("\r%s %5.1f%% | %s / %s | %s/s | ETA: %s   ",
                    bar, percent,
                    FormatUtils.formatSize(current),
                    FormatUtils.formatSize(totalSize),
                    FormatUtils.formatSize((long) speed),
                    eta);
        } else {
            System.out.printf("\r%s | %s/s   ",
                    FormatUtils.formatSize(current),
                    FormatUtils.formatSize((long) speed));
        }
    }
}
