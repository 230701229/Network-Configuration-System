package com.nms.compliance.controller;

import com.nms.compliance.dto.AIAnalysisDTO;
import com.nms.compliance.repository.AIAnalysisRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AIController {
    private final AIAnalysisRepository aiAnalysisRepository;

    public AIController(AIAnalysisRepository aiAnalysisRepository) {
        this.aiAnalysisRepository = aiAnalysisRepository;
    }

    @GetMapping("/analyses/{deviceId}")
    public ResponseEntity<List<AIAnalysisDTO>> getAnalysesByDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(aiAnalysisRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId).stream().map(a -> {
            AIAnalysisDTO dto = new AIAnalysisDTO();
            dto.setId(a.getId());
            dto.setDeviceId(a.getDevice().getId());
            dto.setSummary(a.getSummary());
            dto.setAnalysis(a.getAnalysis());
            dto.setRecommendations(a.getRecommendations());
            dto.setCreatedAt(a.getCreatedAt());
            return dto;
        }).collect(Collectors.toList()));
    }
}
