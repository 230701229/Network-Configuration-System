package com.nms.compliance.controller;

import com.nms.compliance.dto.CreateYangModelRequest;
import com.nms.compliance.dto.YangModelDTO;
import com.nms.compliance.service.YangService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/yang-models")
public class YangController {
    private final YangService yangService;

    public YangController(YangService yangService) {
        this.yangService = yangService;
    }

    @GetMapping
    public ResponseEntity<List<YangModelDTO>> getAllYangModels() {
        return ResponseEntity.ok(yangService.getAllYangModels());
    }

    @PostMapping
    public ResponseEntity<YangModelDTO> createYangModel(@Valid @RequestBody CreateYangModelRequest request) {
        return ResponseEntity.ok(yangService.createYangModel(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<YangModelDTO> getYangModelById(@PathVariable Long id) {
        return ResponseEntity.ok(yangService.getYangModelById(id));
    }
}
