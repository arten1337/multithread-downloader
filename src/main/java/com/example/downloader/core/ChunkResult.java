package com.example.downloader.core;

public record ChunkResult(int chunkIndex, boolean success, String error) {
}
