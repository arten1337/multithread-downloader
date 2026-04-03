package com.example.downloader.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DownloadMetadata {

    private final String url;
    private final long fileSize;
    private final List<ChunkRange> chunks;

    @JsonCreator
    public DownloadMetadata(@JsonProperty("url") String url,
                            @JsonProperty("fileSize") long fileSize,
                            @JsonProperty("chunks") List<ChunkRange> chunks) {
        this.url = url;
        this.fileSize = fileSize;
        this.chunks = chunks;
    }

    public String getUrl() { return url; }
    public long getFileSize() { return fileSize; }
    public List<ChunkRange> getChunks() { return chunks; }

    public long getTotalDownloaded() {
        return chunks.stream().mapToLong(ChunkRange::getBytesDownloaded).sum();
    }
}
