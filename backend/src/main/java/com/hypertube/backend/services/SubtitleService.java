package com.hypertube.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.*;

import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class SubtitleService {

    @Value("${hypertube.security.opensubtitles.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper       = new ObjectMapper();
    private static final String API_URL = "https://api.opensubtitles.com/api/v1";
    private static final String USER_AGENT = "hypertube v1.0";

    public String getSubtitle(Long movieId, String imdbId, String lang) throws Exception {
        Path vttPath = Paths.get("/tmp/subtitles/" + movieId + "_" + lang + ".vtt");

        if (Files.exists(vttPath))
            return vttPath.toString();

        Files.createDirectories(vttPath.getParent());
        String fileId = searchSubtitle(imdbId, lang);
        if (fileId == null)
            return null;
        String downloadUrl = getDownloadUrl(fileId);
        if (downloadUrl == null)
            return null;

        byte[] srtBytes = restTemplate.getForObject(downloadUrl, byte[].class);
        if (srtBytes == null)
            return null;

        String srtContent = new String(srtBytes, "UTF-8");
        String vttContent = srtToVtt(srtContent);
        Files.writeString(vttPath, vttContent);
        return vttPath.toString();
    }
    
    private String searchSubtitle(String imdbId, String lang) throws Exception {
        HttpHeaders headers = buildHeaders();
        String cleanImdbId = imdbId.startsWith("tt") ? imdbId.substring(2) : imdbId;
        String url = API_URL + "/subtitles?imdb_id=" + cleanImdbId
                + "&languages=" + lang
                + "&order_by=download_count";
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                return null;
            }
            
            String fileId = data.get(0)
                            .path("attributes")
                            .path("files")
                            .get(0)
                            .path("file_id")
                            .asText(null);

            return fileId;
        } catch (RestClientResponseException e) {
            throw e;
        } catch (Exception e) {
            throw e;
        }
    }

    private String getDownloadUrl(String fileId) throws Exception {
        HttpHeaders headers = buildHeaders();
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("file_id", Integer.parseInt(fileId));
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                API_URL + "/download",
                HttpMethod.POST,
                new HttpEntity<>(bodyMap, headers),
                String.class
            );
            JsonNode root = mapper.readTree(response.getBody());
            String link = root.path("link").asText(null);
            return link;
        } catch (RestClientResponseException e) {
            throw e;
        } catch (Exception e) {
            throw e;
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Api-Key", apiKey);
        headers.set("User-Agent", USER_AGENT);
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        return headers;
    }

    private String srtToVtt(String srt) {
        StringBuilder vtt = new StringBuilder("WEBVTT\n\n");
        String clean = srt.startsWith("\uFEFF") ? srt.substring(1) : srt;
        String converted = clean
            .replaceAll("(\\d{2}:\\d{2}:\\d{2}),(\\d{3})", "$1.$2")
            .replaceAll("\r\n", "\n")
            .replaceAll("\r", "\n");
        vtt.append(converted);
        return vtt.toString();
    }
}