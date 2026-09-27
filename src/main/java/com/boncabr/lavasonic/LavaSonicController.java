package com.boncabr.lavasonic;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Read-only diagnostics for trusted Lavalink networks. */
@RestController
@RequestMapping("/v4/lavasonic")
public final class LavaSonicController {
    private final LavaSonicMetrics metrics;

    public LavaSonicController(LavaSonicMetrics metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return metrics.health();
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        return metrics.metrics();
    }
}