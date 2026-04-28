package com.hypertube.backend.controllers;

import com.hypertube.backend.services.TorrentDownloader;
import com.hypertube.backend.models.Movie;
import com.hypertube.backend.models.Movie.DownloadStatus;
import com.hypertube.backend.services.MovieService;
import com.hypertube.backend.services.SubtitleService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final TorrentDownloader torrentDownloader;
    private final MovieService      movieService;
    private final SubtitleService subtitleService;

    public static final ConcurrentHashMap<Long, Integer>    progressMap  = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Long, SseEmitter> emitters     = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<Long, File>       downloadDirs = new ConcurrentHashMap<>();

    public List<Movie> arrayMovies = new ArrayList<>();

    private static final long MIN_STREAMABLE_BYTES = 50L * 1024 * 1024;


    // ══════════════════════════════════════════════════════════════════════════
    //  GET /{id}/subtitles?lang=en&imdbId=tt1234567
    // ══════════════════════════════════════════════════════════════════════════
    @CrossOrigin(origins = "*")
    @GetMapping(value = "/{id}/subtitles", produces = "text/vtt")
    public void getSubtitles(
            @PathVariable Long id,
            @RequestParam(defaultValue = "en") String lang,
            @RequestParam String imdbId,
            HttpServletResponse response) throws Exception {
        String vttPath = subtitleService.getSubtitle(id, imdbId, lang);

        if (vttPath == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No subtitles found");
            return;
        }

        response.setContentType("text/vtt; charset=UTF-8");
        response.setHeader("Cache-Control", "public, max-age=86400");
        response.setHeader("Access-Control-Allow-Origin", "*");

        Files.copy(Paths.get(vttPath), response.getOutputStream());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GET /popular
    // ══════════════════════════════════════════════════════════════════════════
    @GetMapping("/popular")
    public ResponseEntity<?> getPopular(
        @RequestParam(defaultValue = "1") String page,
        @RequestParam(defaultValue = "en") String language) {
        try {
            return ResponseEntity.ok(movieService.getPopularFromTmdb(page, language));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GET /{id}/similar
    // ══════════════════════════════════════════════════════════════════════════
    @GetMapping("/{id}/similar")
    public ResponseEntity<?> getSimilarMovies(
        @PathVariable Long id, 
        @RequestParam(defaultValue = "en-US") String language
    ) {
        try {
            Object result = movieService.getSimilarMovies(id, language);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/discover")
    public ResponseEntity<?> discoverMovies(
        @RequestParam(defaultValue = "1") String page,
        @RequestParam(defaultValue = "popular") String sortBy,
        @RequestParam(defaultValue = "en-US") String language) {
        try {
            return ResponseEntity.ok(movieService.discoverFromTmdb(page, sortBy, language));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GET /search
    // ══════════════════════════════════════════════════════════════════════════
    @GetMapping("/search")
    public ResponseEntity<?> searchMovies(
        @RequestParam String query, 
        @RequestParam(defaultValue = "en-US") String language
    ) {
        try {
            Object result = movieService.searchMoviesFromTmdb(query, language);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GET /{id}
    // ══════════════════════════════════════════════════════════════════════════
    @GetMapping("/{id}")
    public ResponseEntity<?> getMovieDetail(
        @PathVariable Long id, 
        @RequestParam(defaultValue = "en-US") String language
    ) {
        try {
            Object result = movieService.getMovieDetail(id, language);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  POST /{id}/watch
    // ══════════════════════════════════════════════════════════════════════════
    @PostMapping("/{id}/watch")
    public ResponseEntity<?> watchMovie(@PathVariable Long id,
                                        @RequestBody Map<String, String> body) {
        String title = body.get("title");
        String year  = body.get("year");

        Optional<Movie> existing = arrayMovies.stream()
            .filter(m -> m.getId().equals(id))
            .findFirst();

        if (existing.isPresent()) {
            Movie movie = existing.get();
            int prog = movie.getCurrentProgress() != null ? movie.getCurrentProgress() : 0;

            if (prog >= 100 || movie.getStatus() == DownloadStatus.COMPLETED) {
                return ResponseEntity.ok(Map.of(
                    "status",  "already_downloaded",
                    "message", "File already available"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "status",  "already_downloading",
                "message", "Already downloading"
            ));
        }

        File torrentsRoot = new File("/tmp/torrents");
        if (torrentsRoot.exists() && torrentsRoot.isDirectory()) {
            File[] dirs = torrentsRoot.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    File found = buscarRecursivo(dir);
                    if (found != null) {
                        if (found.length() > MIN_STREAMABLE_BYTES) {
    
                            Movie movie = new Movie(id, title, year, found, 100, null);
                            movie.setStatus(DownloadStatus.COMPLETED);
                            movie.setCurrentProgress(100);
                            arrayMovies.add(movie);
                            downloadDirs.put(id, dir);
                            return ResponseEntity.ok(Map.of(
                                "status",  "already_downloaded",
                                "message", "File already available"
                            ));
                        } else {
                            found.delete();
                        }
                    }
                }
            }
        }

        String magnetUri;
        
        try {
            magnetUri = movieService.findMagnetLegal(title, year);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }

        if (magnetUri == null) {
            return ResponseEntity.status(404).body(Map.of(
                "error", "No torrent found for: " + title
            ));
        }


        Movie movie = new Movie(id, title, year, findDownloadedFile(id), 0, magnetUri);
        movie.setStatus(DownloadStatus.PENDING);
        arrayMovies.add(movie);

        CompletableFuture.runAsync(() -> {
            try {
                torrentDownloader.downloads(movie);
            } catch (Exception e) {
                movie.setStatus(DownloadStatus.ERROR);
                movie.setCurrentProgress(-1);
            }
        });

        return ResponseEntity.ok(Map.of(
            "status",  "downloading",
            "message", "Download started for: " + title
        ));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GET /{id}/progress  (SSE)
    // ══════════════════════════════════════════════════════════════════════════
    @GetMapping(value = "/{id}/progress", produces = "text/event-stream")
    public SseEmitter getProgress(@PathVariable Long id) {

        SseEmitter emitter = new SseEmitter(300_000L);
        emitters.put(id, emitter);

        emitter.onCompletion(() -> {
            emitters.remove(id);
        });
        emitter.onTimeout(() -> {
            emitters.remove(id);
        });

        try {
            int current = progressMap.getOrDefault(id, 0);
            emitter.send(SseEmitter.event().data(String.valueOf(current)));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GET /{id}/ready
    // ══════════════════════════════════════════════════════════════════════════
    @GetMapping("/{id}/ready")
    public ResponseEntity<?> isReady(@PathVariable Long id) {
        File f = findDownloadedFile(id);
        Map<String, Object> res = new HashMap<>();

        if (f == null || !f.exists()) {
            res.put("ready",   false);
            res.put("size",    0);
            return ResponseEntity.ok(res);
        }

        long size    = f.length();
        boolean ready = size > MIN_STREAMABLE_BYTES;

        Movie movie = arrayMovies.stream()
            .filter(m -> m.getId().equals(id))
            .findFirst()
            .orElse(null);

        res.put("ready",    ready);
        res.put("size",     size);
        res.put("status",   movie != null ? movie.getStatus().name() : "UNKNOWN");
        res.put("progress", movie != null ? movie.getCurrentProgress() : 0);
        if (movie != null) {
            res.put("speedMbps",  movie.getSpeedMbps());
            res.put("peers",      movie.getPeersCount());
            res.put("etaSeconds", movie.getEtaSeconds());
            res.put("elapsed",    movie.getElapsedSeconds());
        }

        return ResponseEntity.ok(res);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GET /{id}/transcode
    // ══════════════════════════════════════════════════════════════════════════
    @GetMapping("/{id}/transcode")
    public void transcodeMovie(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") long startSeconds,
            HttpServletResponse response) throws Exception {

        File videoFile = findDownloadedFile(id);
        if (videoFile == null || !videoFile.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long durationSeconds = probeVideoDuration(videoFile);

        response.setContentType("video/mp4");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        if (durationSeconds > 0)
            response.setHeader("X-Video-Duration", String.valueOf(durationSeconds));

        List<String> cmd = new ArrayList<>(Arrays.asList(
            "ffmpeg",
			"-v", "0",
            "-ss", String.valueOf(startSeconds),
            "-i", videoFile.getAbsolutePath(),
            "-c:v", "copy",
            "-c:a", "aac",
            "-preset", "ultrafast",
            "-crf", "23",
            "-b:a", "192k",
            "-ac", "2"
        ));
        if (durationSeconds > 0)
            cmd.addAll(Arrays.asList("-metadata", "duration=" + durationSeconds));
        cmd.addAll(Arrays.asList(
            "-movflags", "frag_keyframe+empty_moov+default_base_moof",
            "-frag_duration", "2000000",
            "-f", "mp4",
            "pipe:1"
        ));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        Process process = pb.start();

        Thread stderrThread = new Thread(() -> {
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getErrorStream()))) {
                reader.lines().forEach(line -> {});
            } catch (Exception ignored) {}
        });
        stderrThread.setDaemon(true);
        stderrThread.start();

        try (InputStream ffmpegOut = process.getInputStream();
            OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[65536];
            int bytesRead;
            while ((bytesRead = ffmpegOut.read(buffer)) != -1) {
                try {
                    out.write(buffer, 0, bytesRead);
                    out.flush();
                } catch (IOException clientGone) {
                    return;
                }
            }
        } catch (Exception e) {
        } finally {
            process.destroyForcibly();
        }
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  Utilidades privadas
    // ══════════════════════════════════════════════════════════════════════════

    private File findDownloadedFile(Long movieId) {
        File dir = downloadDirs.get(movieId);
        if (dir != null && dir.exists()) {
            File found = buscarRecursivo(dir);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private File buscarRecursivo(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File found = buscarRecursivo(f);
                if (found != null) return found;
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi"))
                    return f;
            }
        }
        return null;
    }

    private long probeVideoDuration(File videoFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "quiet",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                videoFile.getAbsolutePath()
            );
            Process p  = pb.start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            return (long) Double.parseDouble(out);
        } catch (Exception e) {
            return 0;
        }
    }
}