package com.nms.compliance.service;

import com.nms.compliance.dto.RealtimeNotificationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SseNotificationService.class);
    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter registerClient() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitters.add(emitter);
        log.info("New SSE client subscribed. Total active listeners: {}", emitters.size());

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE client completed. Active listeners: {}", emitters.size());
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
            log.debug("SSE client timed out. Active listeners: {}", emitters.size());
        });

        emitter.onError(e -> {
            emitters.remove(emitter);
            log.debug("SSE client error: {}. Active listeners: {}", e.getMessage(), emitters.size());
        });

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(RealtimeNotificationDTO.builder()
                            .eventType(RealtimeNotificationDTO.EventType.HEARTBEAT)
                            .message("Connected to real-time network compliance stream")
                            .timestamp(LocalDateTime.now())
                            .build()));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void broadcast(RealtimeNotificationDTO notification) {
        if (emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(notification.getEventType().name())
                        .data(notification));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            log.debug("Removed {} unreachable SSE emitters. Active listeners: {}", deadEmitters.size(), emitters.size());
        }
    }

    public void broadcastDriftEvent(Long deviceId, String hostname, String message, Object data) {
        broadcast(RealtimeNotificationDTO.builder()
                .eventType(RealtimeNotificationDTO.EventType.DRIFT_DETECTED)
                .deviceId(deviceId)
                .hostname(hostname)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public void broadcastScanCompleted(Long deviceId, String hostname, String message, Object data) {
        broadcast(RealtimeNotificationDTO.builder()
                .eventType(RealtimeNotificationDTO.EventType.SCAN_COMPLETED)
                .deviceId(deviceId)
                .hostname(hostname)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public void broadcastDeviceStatus(Long deviceId, String hostname, String status) {
        broadcast(RealtimeNotificationDTO.builder()
                .eventType(RealtimeNotificationDTO.EventType.DEVICE_STATUS_CHANGED)
                .deviceId(deviceId)
                .hostname(hostname)
                .message("Device status updated: " + status)
                .data(status)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Send heartbeat every 15s to keep SSE connection alive across proxies/gateways
     */
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        RealtimeNotificationDTO heartbeat = RealtimeNotificationDTO.builder()
                .eventType(RealtimeNotificationDTO.EventType.HEARTBEAT)
                .message("heartbeat")
                .timestamp(LocalDateTime.now())
                .build();
        broadcast(heartbeat);
    }
}
