package com.hypertube.backend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class TorrentCleanupService {

	private static final String TORRENTS_DIR = "/tmp/torrents";
	private static final String SUBTITLES_DIR = "/tmp/subtitles";
	private static final int DAYS_TO_KEEP = 30;

	@Scheduled(cron = "0 0 3 * * ?")
	public void cleanupOldTorrents() {
		Instant threshold = Instant.now().minus(DAYS_TO_KEEP, ChronoUnit.DAYS);
		cleanDirectory(new File(TORRENTS_DIR), threshold);
		cleanDirectory(new File(SUBTITLES_DIR), threshold);
	}

	private void cleanDirectory(File rootDir, Instant threshold) {
		if (!rootDir.exists() || !rootDir.isDirectory()) {
			return;
		}

		File[] files = rootDir.listFiles();
		if (files != null) {
			for (File file : files) {
				Instant lastModified = Instant.ofEpochMilli(file.lastModified());
				if (lastModified.isBefore(threshold)) {
					FileSystemUtils.deleteRecursively(file);
				}
			}
		}
	}
}