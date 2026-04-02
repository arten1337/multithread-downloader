package com.example.downloader;

public record ChunkResult(int chunkIndex, boolean success, String error) {
}
