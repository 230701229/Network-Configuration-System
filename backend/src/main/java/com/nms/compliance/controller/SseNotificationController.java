package com.nms.compliance.controller;

import com.nms.compliance.service.SseNotificationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class SseNotificationController {

    private final SseNotificationService sseNotificationService;

    public SseNotificationController(SseNotificationService sseNotificationService) {
        this.sseNotificationService = sseNotificationService;
    }

    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents() {
        return sseNotificationService.registerClient();
    }

    @GetMapping(value = "/drift/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDriftEvents() {
        return sseNotificationService.registerClient();
    }
}
