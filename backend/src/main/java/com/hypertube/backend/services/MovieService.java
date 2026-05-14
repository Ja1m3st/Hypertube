package com.hypertube.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypertube.backend.models.Movie;
import com.hypertube.backend.repositories.MovieRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service    
@RequiredArgsConstructor
public class MovieService {

	@Value("${hypertube.security.tmdb.key}")
	private String tmdbApiKey;

    @Value("${hypertube.security.omdb.key}")
    private String omdbApiKey;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper mapper = new ObjectMapper();
	private final MovieRepository movieRepository;

	// ── TMDB: películas populares ──────────────────────────────
	public Object getPopularFromTmdb(String page, String language) throws Exception {
        if (page == null || page.trim().isEmpty()) page = "1";
        if (language == null || language.isBlank()) language = "en-US";
        
        String url = "https://api.themoviedb.org/3/movie/popular?api_key=" + tmdbApiKey 
                + "&language=" + language + "&page=" + page;        
        String json = restTemplate.getForObject(url, String.class);
        return mapper.readValue(json, Object.class);
    }

    // ── TMDB: obtener películas similares ──────────────────────────────
    public Object getSimilarMovies(Long id, String language) throws Exception {
        if (language == null || language.isBlank()) language = "en-US";
        String url = "https://api.themoviedb.org/3/movie/" + id 
                     + "/similar?api_key=" + tmdbApiKey + "&language=" + language + "&page=1";   
        String json = restTemplate.getForObject(url, String.class);
        return mapper.readValue(json, Object.class);
    }

	// ── TMDB: detalle de película ──────────────────────────────
    public Object getMovieDetail(Long id, String language) throws Exception {
        if (language == null || language.isBlank()) language = "en-US";
        String url = "https://api.themoviedb.org/3/movie/" + id 
                     + "?api_key=" + tmdbApiKey + "&language=" + language + "&append_to_response=credits";
        String json = restTemplate.getForObject(url, String.class);
        return mapper.readValue(json, Object.class);
    }

    public Object searchMoviesFromTmdb(String query, String language) throws Exception {
        if (language == null || language.isBlank()) language = "en-US";
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        String tmdbUrl = "https://api.themoviedb.org/3/search/movie?api_key=" 
                     + tmdbApiKey + "&language=" + language + "&query=" + encodedQuery + "&page=1&include_adult=false";
        
        String tmdbJson = restTemplate.getForObject(tmdbUrl, String.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> tmdbResponse = mapper.readValue(tmdbJson, Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tmdbResults = (List<Map<String, Object>>) tmdbResponse.getOrDefault("results", new java.util.ArrayList<>());

        String omdbUrl = "https://www.omdbapi.com/?apikey=" + omdbApiKey + "&s=" + encodedQuery + "&type=movie";
        List<Map<String, Object>> omdbResults = new java.util.ArrayList<>();
        
        try {
            String omdbJson = restTemplate.getForObject(omdbUrl, String.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> omdbResponse = mapper.readValue(omdbJson, Map.class);
            
            if (omdbResponse.containsKey("Search")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> search = (List<Map<String, Object>>) omdbResponse.get("Search");
                
                for (Map<String, Object> item : search) {
                    Map<String, Object> mappedMovie = new java.util.HashMap<>();
                    mappedMovie.put("title", item.get("Title"));
                    mappedMovie.put("release_date", item.get("Year"));
                    String poster = (String) item.get("Poster");
                    mappedMovie.put("poster_path", "N/A".equals(poster) ? null : poster);
                    String imdbId = (String) item.get("imdbID");
                    long numericId = Long.parseLong(imdbId.replace("tt", ""));
                    mappedMovie.put("id", numericId); 
                    mappedMovie.put("vote_average", 0.0);
                    omdbResults.add(mappedMovie);
                }
            }
        } catch (Exception e) {
        }

        Map<String, Map<String, Object>> mergedMap = new java.util.HashMap<>();

        for (Map<String, Object> peli : omdbResults) {
            String titulo = (String) peli.getOrDefault("title", "");
            mergedMap.put(titulo.toLowerCase(), peli);
        }
        for (Map<String, Object> peli : tmdbResults) {
            String titulo = (String) peli.getOrDefault("title", "");
            mergedMap.put(titulo.toLowerCase(), peli);
        }

        List<Map<String, Object>> finalResults = new java.util.ArrayList<>(mergedMap.values());

        finalResults.sort((peli1, peli2) -> {
            String titulo1 = (String) peli1.getOrDefault("title", "");
            String titulo2 = (String) peli2.getOrDefault("title", "");
            return titulo1.compareToIgnoreCase(titulo2);
        });

        Map<String, Object> finalResponse = new java.util.HashMap<>();
        finalResponse.put("page", 1);
        finalResponse.put("results", finalResults);
        finalResponse.put("total_results", finalResults.size());
        finalResponse.put("total_pages", 1);

        return finalResponse;
    }

	// ── TMDB: descubrir películas con filtros ──────────────────────────────
    public Object discoverFromTmdb(String page, String sortBy, String language) throws Exception {
        if (page == null || page.trim().isEmpty()) page = "1";
        if (language == null || language.isBlank()) language = "en-US";
        
        String tmdbSort = "popularity.desc";
        if ("rating".equals(sortBy)) {
            tmdbSort = "vote_average.desc&vote_count.gte=300"; 
        } else if ("newest".equals(sortBy)) {
            tmdbSort = "primary_release_date.desc";
        }

        String url = "https://api.themoviedb.org/3/discover/movie?api_key=" + tmdbApiKey 
                + "&language=" + language + "&page=" + page + "&sort_by=" + tmdbSort;        
        
        String json = restTemplate.getForObject(url, String.class);
        return mapper.readValue(json, Object.class);
    }

	// ── Archive.org: buscar torrent por título ──────────────────────────
	public String findMagnetLegal(String title, String year) throws Exception {
		String cleanTitle = title.replaceAll("[^a-zA-Z0-9 ]", "");
		String encodedTitle = URLEncoder.encode("\"" + cleanTitle + "\"", StandardCharsets.UTF_8);

		String url = "https://archive.org/advancedsearch.php?q=title:(" + encodedTitle + ")"
				+ "+AND+mediatype:(movies)&fl[]=identifier,title,year&sort[]=downloads+desc&output=json&rows=5";

		String json = restTemplate.getForObject(url, String.class);
		JsonNode root = mapper.readTree(json);
		
		JsonNode docs = root.path("response").path("docs");

		if (!docs.isArray() || docs.isEmpty()) {
			return null;
		}
		JsonNode best = null;
		for (JsonNode doc : docs) {
			String docYear = doc.path("year").asText();
			if (year != null && docYear.equals(year)) {
				best = doc;
				break;
			}
		}
		
		if (best == null) best = docs.get(0);

		String identifier = best.path("identifier").asText();

		String torrentUrl = "https://archive.org/download/" + identifier + "/" + identifier + "_archive.torrent";
		
		return torrentUrl;
	}

	@Transactional
    public Movie getOrCreateMovie(Long tmdbId) {
        return movieRepository.findById(tmdbId).orElseGet(() -> {
            try {
                String url = "https://api.themoviedb.org/3/movie/" + tmdbId 
                             + "?api_key=" + tmdbApiKey + "&language=en-US";
                
                String json = restTemplate.getForObject(url, String.class);
                JsonNode node = mapper.readTree(json);
                Movie newMovie = new Movie();
                
                newMovie.setId(tmdbId); 
                newMovie.setTitle(node.path("title").asText());
                
                if (node.hasNonNull("poster_path")) {
                    newMovie.setPosterPath(node.path("poster_path").asText());
                }
                if (node.hasNonNull("release_date")) {
                    newMovie.setYear(node.path("release_date").asText().split("-")[0]);
                }

                return movieRepository.save(newMovie);

            } catch (Exception e) {
                throw new RuntimeException("Error al importar película desde TMDB: " + e.getMessage());
            }
        });
    }

}