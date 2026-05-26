package com.taejin.chat.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class ServiceInfoController {

    /** 실제 구동 포트를 설정에서 주입받는다 (하드코딩 금지). */
    @Value("${server.port:8081}")
    private int serverPort;

    @GetMapping("/api/service-info")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {
        return ResponseEntity.ok(Map.of(
                "serviceName", "tj-chat-service",
                "version", "1.0.0",
                "status", "running",
                "port", serverPort
        ));
    }
}
