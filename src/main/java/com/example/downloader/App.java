package com.example.downloader;

import com.example.downloader.core.DownloadManager;

import java.nio.file.Path;

public class App {

    private static final int DEFAULT_THREADS = 4;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar multithread-downloader.jar <URL> [outputFile] [threads]");
            System.exit(1);
        }

        String url = args[0];
        String outputFile = args.length >= 2 ? args[1] : guessFileName(url);
        int threads = args.length >= 3 ? parseThreadCount(args[2]) : DEFAULT_THREADS;

        System.out.println("URL:     " + url);
        System.out.println("Output:  " + outputFile);
        System.out.println("Threads: " + threads);
        System.out.println();

        DownloadManager manager = new DownloadManager(url, Path.of(outputFile), threads);
        try {
            manager.download();
        } catch (Exception e) {
            System.err.println("Download failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String guessFileName(String url) {
        String path = url.replaceAll("\\?.*$", "");
        int lastSlash = path.lastIndexOf('/');
        String name = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        return name.isEmpty() ? "download" : name;
    }

    private static int parseThreadCount(String value) {
        try {
            int n = Integer.parseInt(value);
            if (n < 1) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            System.err.println("Invalid thread count '" + value + "', using default " + DEFAULT_THREADS);
            return DEFAULT_THREADS;
        }
    }
}
