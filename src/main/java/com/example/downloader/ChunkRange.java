package com.example.downloader;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChunkRange {

    private final long start;
    private final long end;
    private long currentPosition;
    private boolean complete;

    @JsonCreator
    public ChunkRange(@JsonProperty("start") long start, @JsonProperty("end") long end) {
        this.start = start;
        this.end = end;
        this.currentPosition = start;
        this.complete = false;
    }

    public long getStart() { return start; }
    public long getEnd() { return end; }

    public synchronized long getCurrentPosition() { return currentPosition; }

    public synchronized void advance(long bytes) {
        currentPosition += bytes;
    }

    public synchronized boolean isComplete() { return complete; }

    public synchronized void markComplete() {
        this.complete = true;
        this.currentPosition = end + 1;
    }

    public synchronized long getBytesDownloaded() {
        return currentPosition - start;
    }
}
