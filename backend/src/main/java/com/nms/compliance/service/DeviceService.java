package com.nms.compliance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nms.compliance.dto.CreateDeviceRequest;
import com.nms.compliance.dto.DeviceDTO;
import com.nms.compliance.dto.TestConnectionResponse;
import com.nms.compliance.dto.UpdateDeviceRequest;
import com.nms.compliance.entity.ConfigurationSnapshot;
import com.nms.compliance.entity.Device;
import com.nms.compliance.exception.ResourceNotFoundException;
import com.nms.compliance.network.factory.DeviceProviderFactory;
import com.nms.compliance.network.mock.DriftSimulationState;
import com.nms.compliance.network.model.NormalizedConfiguration;
import com.nms.compliance.network.provider.NetworkDeviceProvider;
import com.nms.compliance.repository.ConfigurationSnapshotRepository;
import com.nms.compliance.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final EncryptionService encryptionService;
    private final DeviceProviderFactory providerFactory;
    private final ConfigurationSnapshotRepository snapshotRepository;
    private final DriftSimulationState driftSimulationState;
    private final ObjectMapper objectMapper;

    public DeviceService(DeviceRepository deviceRepository, EncryptionService encryptionService,
                         DeviceProviderFactory providerFactory, ConfigurationSnapshotRepository snapshotRepository,
                         DriftSimulationState driftSimulationState, ObjectMapper objectMapper) {
        this.deviceRepository = deviceRepository;
        this.encryptionService = encryptionService;
        this.providerFactory = providerFactory;
        this.snapshotRepository = snapshotRepository;
        this.driftSimulationState = driftSimulationState;
        this.objectMapper = objectMapper;
    }

    public List<DeviceDTO> getAllDevices() {
        return deviceRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public DeviceDTO getDeviceById(Long id) {
        return mapToDTO(getDeviceEntity(id));
    }

    private Device getDeviceEntity(Long id) {
        return deviceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Device not found"));
    }

    @Transactional
    public DeviceDTO createDevice(CreateDeviceRequest request) {
        Device device = Device.builder()
                .hostname(request.getHostname())
                .ipAddress(request.getIpAddress())
                .deviceType(request.getDeviceType())
                .vendor(request.getVendor())
                .model(request.getModel())
                .managementProtocol(request.getManagementProtocol())
                .netconfPort(request.getNetconfPort() != null ? request.getNetconfPort() : 830)
                .restconfPort(request.getRestconfPort() != null ? request.getRestconfPort() : 443)
                .username(request.getUsername())
                .encryptedPassword(encryptionService.encrypt(request.getPassword()))
                .status(Device.DeviceStatus.UNKNOWN)
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return mapToDTO(deviceRepository.save(device));
    }

    @Transactional
    public DeviceDTO updateDevice(Long id, UpdateDeviceRequest request) {
        Device device = getDeviceEntity(id);
        device.setHostname(request.getHostname());
        device.setIpAddress(request.getIpAddress());
        device.setDeviceType(request.getDeviceType());
        device.setVendor(request.getVendor());
        device.setModel(request.getModel());
        device.setManagementProtocol(request.getManagementProtocol());
        if (request.getNetconfPort() != null) device.setNetconfPort(request.getNetconfPort());
        if (request.getRestconfPort() != null) device.setRestconfPort(request.getRestconfPort());
        device.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            device.setEncryptedPassword(encryptionService.encrypt(request.getPassword()));
        }
        device.setDescription(request.getDescription());
        device.setUpdatedAt(LocalDateTime.now());
        return mapToDTO(deviceRepository.save(device));
    }

    @Transactional
    public void deleteDevice(Long id) {
        deviceRepository.delete(getDeviceEntity(id));
    }

    public TestConnectionResponse testConnection(Long id) {
        Device device = getDeviceEntity(id);
        NetworkDeviceProvider provider = providerFactory.getProvider(device.getManagementProtocol());
        long start = System.currentTimeMillis();
        boolean success = false;
        String message = "Connection successful";
        try {
            success = provider.testConnection(device);
        } catch (Exception e) {
            message = e.getMessage();
        }
        long responseTime = System.currentTimeMillis() - start;
        return TestConnectionResponse.builder()
                .success(success)
                .message(message)
                .protocol(device.getManagementProtocol().name())
                .responseTime(responseTime)
                .build();
    }

    @Transactional
    public NormalizedConfiguration retrieveConfiguration(Long id) {
        Device device = getDeviceEntity(id);
        NetworkDeviceProvider provider = providerFactory.getProvider(device.getManagementProtocol());
        NormalizedConfiguration config = provider.getConfiguration(device);
        
        try {
            String configJson = objectMapper.writeValueAsString(config.parameters());
            String hash = sha256(configJson);
            
            ConfigurationSnapshot snapshot = ConfigurationSnapshot.builder()
                    .device(device)
                    .snapshotData(configJson)
                    .configHash(hash)
                    .retrievalProtocol(device.getManagementProtocol())
                    .retrievedAt(LocalDateTime.now())
                    .build();
            snapshotRepository.save(snapshot);
            
            device.setLastSeen(LocalDateTime.now());
            device.setStatus(Device.DeviceStatus.ONLINE);
            deviceRepository.save(device);
            
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing config", e);
        }
        
        return config;
    }

    public void simulateDrift(Long id) {
        simulateDrift(id, null, null);
    }

    public void simulateDrift(Long id, String param, String value) {
        Device device = getDeviceEntity(id);
        if (param != null && value != null) {
            driftSimulationState.applyDrift(id, param, value);
            if (param.endsWith("-enabled")) {
                driftSimulationState.applyDrift(id, param.substring(0, param.length() - 8), value);
            } else {
                driftSimulationState.applyDrift(id, param + "-enabled", value);
            }
        } else if ("Router-01".equals(device.getHostname())) {
            driftSimulationState.applyDrift(id, "telnet", "true");
            driftSimulationState.applyDrift(id, "telnet-enabled", "true");
            driftSimulationState.applyDrift(id, "ntp", "false");
            driftSimulationState.applyDrift(id, "ntp-enabled", "false");
        }
    }

    public void resetDrift(Long id) {
        driftSimulationState.resetDrift(id);
    }

    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DeviceDTO mapToDTO(Device device) {
        DeviceDTO dto = new DeviceDTO();
        dto.setId(device.getId());
        dto.setHostname(device.getHostname());
        dto.setIpAddress(device.getIpAddress());
        dto.setDeviceType(device.getDeviceType() != null ? device.getDeviceType().name() : null);
        dto.setVendor(device.getVendor());
        dto.setModel(device.getModel());
        dto.setManagementProtocol(device.getManagementProtocol() != null ? device.getManagementProtocol().name() : null);
        dto.setNetconfPort(device.getNetconfPort());
        dto.setRestconfPort(device.getRestconfPort());
        dto.setUsername(device.getUsername());
        dto.setStatus(device.getStatus() != null ? device.getStatus().name() : null);
        dto.setLastSeen(device.getLastSeen());
        dto.setDescription(device.getDescription());
        return dto;
    }
}
