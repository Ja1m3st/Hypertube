package com.hypertube.backend.services;

import bt.Bt;
import bt.data.Storage;
import bt.data.file.FileSystemStorage;
import bt.dht.DHTConfig;
import bt.dht.DHTModule;
import bt.runtime.BtClient;
import bt.runtime.Config;
import bt.torrent.selector.SequentialSelector;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.hypertube.backend.controllers.MovieController;
import com.hypertube.backend.models.Movie;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Service
public class TorrentDownloader {


    private long lastSnapshotBytes = 0;
    private long lastSnapshotTime  = 0;

    public void downloads(Movie movie) throws Exception {

        Path targetDirectory = Paths.get("/tmp/torrents");

        Long   movieId   = movie.getId();
        String magnetUri = movie.getMagnetUri();


        try {
            java.nio.file.Files.createDirectories(targetDirectory);
            targetDirectory.toFile().setWritable(true, false);
            targetDirectory.toFile().setReadable(true, false);
        } catch (IOException e) {
            return;
        }

        Storage storage = new FileSystemStorage(targetDirectory);
        int port = 6891 + (int)(movieId % 100);
        movie.setPort(port);

        Config config = new Config() {{
            setAcceptorPort(port);
            setPeerConnectionTimeout(Duration.ofSeconds(12));
            setMaxPeerConnections(100);
            setNetworkBufferSize(1 << 23);
            setMaxTransferBlockSize(1 << 17);
        }};

        DHTConfig dhtConfig = new DHTConfig() {{
            setListeningPort(port);
            setShouldUseRouterBootstrap(true);
        }};

        var clientBuilder = Bt.client()
            .config(config)
            .storage(storage)
            .autoLoadModules()
            .module(new DHTModule(dhtConfig))
            .selector(SequentialSelector.sequential())
            .stopWhenDownloaded();

        if (magnetUri.startsWith("http")) {
            clientBuilder.torrent(new java.net.URL(magnetUri));
        } else {
            clientBuilder.magnet(magnetUri);
        }

        BtClient client = clientBuilder.build();


        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        movie.markStarted();

        lastSnapshotBytes = 0;
        lastSnapshotTime  = System.currentTimeMillis();

        client.startAsync(state -> {

            int  total      = state.getPiecesTotal();
            int  done       = state.getPiecesComplete();
            long bytes      = state.getDownloaded();
            long peers      = 100;
            long now        = System.currentTimeMillis();

            long deltaBytes = bytes - lastSnapshotBytes;
            long deltaMs    = Math.max(1, now - lastSnapshotTime);
            long speedBps   = (deltaBytes * 1000) / deltaMs;
            lastSnapshotBytes = bytes;
            lastSnapshotTime  = now;


            int percent = (total > 0) ? (int)((done * 100.0) / total) : 0;

            if (percent < 5 && bytes >= 50L * 1024 * 1024) {
                percent = 5;
            }

            if (bytes > 0 && MovieController.downloadDirs.get(movieId) == null) {
                File baseDir = targetDirectory.toFile();
                File[] subdirs = baseDir.listFiles(File::isDirectory);
                if (subdirs != null) {
                    File newest = null;
                    for (File sub : subdirs) {
                        boolean yaMapeado = MovieController.downloadDirs.values()
                            .stream().anyMatch(f -> f.equals(sub));
                        if (!yaMapeado) {
                            if (newest == null || sub.lastModified() > newest.lastModified())
                                newest = sub;
                        }
                    }
                    if (newest != null) {
                        movie.setDownloadDir(newest.getAbsolutePath());
                        MovieController.downloadDirs.put(movieId, newest);
                    }
                }
            }

            long totalBytes = (percent > 0) ? (bytes * 100) / percent : 0;
            movie.updateTelemetry(percent, bytes, totalBytes, (int) peers, speedBps);
            MovieController.progressMap.put(movieId, percent);

            SseEmitter emitter = MovieController.emitters.get(movieId);
            movie.setEmitter(emitter);

            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event().data(String.valueOf(percent)));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                    MovieController.emitters.remove(movieId);
                }
            }


        }, 2000).join();

    }

}