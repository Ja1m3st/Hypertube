package com.hypertube.backend.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hypertube.backend.models.User;
import com.hypertube.backend.models.UserHistory;
import com.hypertube.backend.services.UserHistoryService;
import com.hypertube.backend.repositories.UserHistoryRepository;

@RestController
@RequestMapping("/api/history")
public class UserHistoryController {

    @Autowired
    private UserHistoryService historyService;

    @Autowired
    private UserHistoryRepository historyRepository;

    @PostMapping("/{movieId}/progress")
    public ResponseEntity<?> updateProgess(
        @PathVariable Long movieId,
        @RequestBody Map<String, Object> body,
        @AuthenticationPrincipal User user
    ) {
        try {

            Long userId = user.getId();
            Integer percentage = (Integer) body.get("percentage");
            String  title = (String) body.get("title");
            String  year = (String) body.get("year");
            String  posterPath = (String) body.get("posterPath");

            historyService.updateProgess(userId, movieId, title, year, posterPath ,percentage);

            return ResponseEntity.ok().body(Map.of("message", "\\u001B[32mProgess is saved successful\\u001B[0m"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyHistory(@AuthenticationPrincipal User user) {
        try {
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
            }

            List<UserHistory> history = historyRepository.findByUserId(user.getId());
            
            List<Map<String, Object>> response = history.stream().map(h -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", h.getMovie().getId());
                map.put("title", h.getMovie().getTitle());
                map.put("year", h.getMovie().getYear());
                map.put("posterPath", h.getMovie().getPosterPath() != null ? h.getMovie().getPosterPath() : "");
                map.put("progress", h.getLastThreshold());
                map.put("completed", h.isCompleted());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }
}
