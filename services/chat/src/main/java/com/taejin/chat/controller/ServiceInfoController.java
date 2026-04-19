package com.taejin.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class ServiceInfoController {

    @GetMapping("/api/service-info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        return ResponseEntity.ok(Map.of(
                "serviceName", "tj-chat-service",
                "version", "1.0.0",
                "status", "running",
                "port", 8080
        ));
    }
}
