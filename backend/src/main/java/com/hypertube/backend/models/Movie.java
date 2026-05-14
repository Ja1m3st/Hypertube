package com.hypertube.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.io.File;
import java.time.Instant;
import java.time.Duration;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Data
@Entity
@Table(name = "movies")
public class Movie {

    public enum DownloadStatus {
        PENDING,
        DOWNLOADING,
        STREAMABLE,
        COMPLETED,
        ERROR
    }

    private static final long MIN_STREAMABLE_BYTES = 50L * 1024 * 1024;
    
    @Id
    private Long id;
    
    private String title;
    
    @Column(name = "release_year")
    private String year;

    @Column(length = 500)
    private String posterPath;
    
    @Transient
    private String magnetUri;
    
    private String downloadDir;

    @Transient
    private File existingFile; 
    
    @Transient
    private Integer currentProgress;
    
    @Transient
    private int port;

    @Transient
    private DownloadStatus status = DownloadStatus.PENDING;
    
    @Transient
    private Instant startedAt;
    
    @Transient
    private Instant completedAt;
    
    @Transient
    private long downloadedBytes;
    
    @Transient
    private long totalBytes;
    
    @Transient
    private int peersCount;
    
    @Transient
    private long downloadSpeedBps;

    @Transient
    private SseEmitter emitter;
    
    @Transient
    private int percent;

    public Movie() {}

    public Movie(Long id, String title, String year, File existingFile, Integer currentProgress, String magnetUri) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.existingFile = existingFile;
        this.currentProgress = currentProgress;
        this.magnetUri = magnetUri;
    }

    public long getElapsedSeconds() {
        if (startedAt == null) return 0;
        Instant end = (completedAt != null) ? completedAt : Instant.now();
        return Duration.between(startedAt, end).getSeconds();
    }

    public long getEtaSeconds() {
        if (downloadSpeedBps <= 0 || totalBytes <= 0) return -1;
        long remaining = totalBytes - downloadedBytes;
        return remaining / downloadSpeedBps;
    }

    public double getSpeedMbps() {
        return downloadSpeedBps / (1024.0 * 1024.0);
    }

    public boolean isFailed() {
        return status == DownloadStatus.ERROR || (currentProgress != null && currentProgress < 0);
    }

    public boolean isStreamable() {
        return downloadedBytes >= MIN_STREAMABLE_BYTES
            || status == DownloadStatus.STREAMABLE
            || status == DownloadStatus.COMPLETED;
    }

    public void markStarted() {
        this.startedAt = Instant.now();
        this.status = DownloadStatus.DOWNLOADING;
    }

    public void markCompleted() {
        this.completedAt = Instant.now();
        this.currentProgress = 100;
        this.status = DownloadStatus.COMPLETED;
    }

    public void updateTelemetry(int percent, long downloadedBytes, long totalBytes, int peersCount, long speedBps) {
        this.percent = percent;
        this.currentProgress = percent;
        this.downloadedBytes = downloadedBytes;
        this.totalBytes = totalBytes;
        this.peersCount = peersCount;
        this.downloadSpeedBps = speedBps;

        if (percent >= 100) {
            markCompleted();
        } else if (isStreamable() && status == DownloadStatus.DOWNLOADING) {
            this.status = DownloadStatus.STREAMABLE;
        }
    }


}