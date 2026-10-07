package com.nms.compliance.service;

import com.nms.compliance.dto.CreateYangModelRequest;
import com.nms.compliance.dto.YangModelDTO;
import com.nms.compliance.entity.YangModel;
import com.nms.compliance.exception.ResourceNotFoundException;
import com.nms.compliance.repository.YangModelRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class YangService {
    private final YangModelRepository yangModelRepository;

    public YangService(YangModelRepository yangModelRepository) {
        this.yangModelRepository = yangModelRepository;
    }

    public List<YangModelDTO> getAllYangModels() {
        return yangModelRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public YangModelDTO getYangModelById(Long id) {
        return mapToDTO(yangModelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Not found")));
    }

    public YangModelDTO createYangModel(CreateYangModelRequest request) {
        YangModel model = YangModel.builder()
                .modelName(request.getModelName())
                .namespace(request.getNamespace())
                .revision(request.getRevision())
                .deviceType(request.getDeviceType())
                .description(request.getDescription())
                .modelContent(request.getModelContent())
                .isValid(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return mapToDTO(yangModelRepository.save(model));
    }

    private YangModelDTO mapToDTO(YangModel model) {
        YangModelDTO dto = new YangModelDTO();
        dto.setId(model.getId());
        dto.setModelName(model.getModelName());
        dto.setNamespace(model.getNamespace());
        dto.setRevision(model.getRevision());
        dto.setDeviceType(model.getDeviceType() != null ? model.getDeviceType().name() : null);
        dto.setDescription(model.getDescription());
        dto.setModelContent(model.getModelContent());
        dto.setValid(model.isValid());
        dto.setValidationError(model.getValidationError());
        dto.setCreatedAt(model.getCreatedAt());
        return dto;
    }
}
