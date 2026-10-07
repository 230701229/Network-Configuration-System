package com.nms.compliance.service;

import com.nms.compliance.dto.AIAnalysisDTO;
import com.nms.compliance.entity.ComplianceResult;
import com.nms.compliance.entity.ComplianceScan;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AIServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AIServiceClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${ai.service.enabled:true}")
    private boolean aiEnabled;

    public AIServiceClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public AIAnalysisDTO analyzeViolations(ComplianceScan scan, List<ComplianceResult> violations) {
        AIAnalysisDTO fallback = buildFallback(scan, violations);

        if (!aiEnabled) {
            log.info("AI service disabled — returning fallback analysis");
            return fallback;
        }

        if (violations.isEmpty()) {
            return fallback;
        }

        try {
            // Build request — NEVER include passwords or credentials
            Map<String, Object> request = new HashMap<>();
            request.put("deviceHostname", scan.getDevice().getHostname());
            request.put("deviceType",     scan.getDevice().getDeviceType().name());
            request.put("complianceScore", scan.getComplianceScore());
            request.put("totalViolations", violations.size());

            List<Map<String, String>> violationList = violations.stream().map(v -> {
                Map<String, String> map = new HashMap<>();
                map.put("parameter",    v.getParameter());
                map.put("expectedValue", v.getExpectedValue());
                map.put("actualValue",  v.getActualValue() != null ? v.getActualValue() : "NOT_CONFIGURED");
                map.put("severity",     v.getSeverity().name());
                map.put("description",  v.getDescription() != null ? v.getDescription() : "");
                map.put("remediation",  v.getRemediation() != null ? v.getRemediation() : "");
                return map;
            }).collect(Collectors.toList());
            request.put("violations", violationList);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    aiServiceUrl + "/analyze",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> body = response.getBody();
                AIAnalysisDTO dto = new AIAnalysisDTO();
                dto.setScanId(scan.getId());
                dto.setDeviceId(scan.getDevice().getId());
                dto.setSummary(getString(body, "summary"));
                dto.setAnalysis(getString(body, "analysis"));
                dto.setPriority(getString(body, "priority"));
                dto.setModelUsed(getString(body, "model_used"));
                dto.setAiAvailable(Boolean.TRUE.equals(body.get("ai_available")));
                dto.setCreatedAt(LocalDateTime.now());

                // Convert recommendations list to valid JSON string
                Object recs = body.get("recommendations");
                if (recs instanceof List<?> recList) {
                    try {
                        dto.setRecommendations(objectMapper.writeValueAsString(recList));
                    } catch (Exception e) {
                        dto.setRecommendations("[]");
                    }
                } else {
                    dto.setRecommendations("[]");
                }
                log.info("AI analysis completed for device: {}", scan.getDevice().getHostname());
                return dto;
            }

        } catch (Exception e) {
            log.warn("AI Service call failed for device {}: {} — using fallback", 
                     scan.getDevice().getHostname(), e.getMessage());
        }

        return fallback;
    }

    private AIAnalysisDTO buildFallback(ComplianceScan scan, List<ComplianceResult> violations) {
        AIAnalysisDTO dto = new AIAnalysisDTO();
        dto.setScanId(scan.getId());
        dto.setDeviceId(scan.getDevice() != null ? scan.getDevice().getId() : null);
        dto.setAiAvailable(false);
        dto.setModelUsed("fallback-rule-based");
        dto.setCreatedAt(LocalDateTime.now());

        if (violations.isEmpty()) {
            dto.setSummary("Device is fully compliant. No violations detected.");
            dto.setAnalysis("All configuration parameters match the approved policy.");
            dto.setRecommendations("[]");
            dto.setPriority("INFO");
        } else {
            boolean hasCritical = violations.stream().anyMatch(v -> v.getSeverity().name().equals("CRITICAL"));
            boolean hasHigh     = violations.stream().anyMatch(v -> v.getSeverity().name().equals("HIGH"));
            dto.setPriority(hasCritical ? "CRITICAL" : hasHigh ? "HIGH" : "MEDIUM");
            dto.setSummary(String.format("%s has %d configuration violation(s) with a compliance score of %.1f%%.",
                    scan.getDevice().getHostname(), violations.size(),
                    scan.getComplianceScore().doubleValue()));
            dto.setAnalysis(String.format(
                    "The device has %d violation(s) that deviate from the approved policy. " +
                    "AI analysis service is currently unavailable. " +
                    "Review each failed parameter and apply the suggested remediation.",
                    violations.size()));
            List<String> recs = violations.stream()
                    .map(v -> v.getRemediation() != null ? v.getRemediation() 
                                                         : "Fix " + v.getParameter() + " to " + v.getExpectedValue())
                    .collect(Collectors.toList());
            try {
                dto.setRecommendations(objectMapper.writeValueAsString(recs));
            } catch (Exception e) {
                dto.setRecommendations("[]");
            }
        }
        return dto;
    }

    private String getString(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
