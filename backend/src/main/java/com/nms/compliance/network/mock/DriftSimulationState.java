package com.nms.compliance.network.mock;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DriftSimulationState {
    private final Map<Long, Map<String, String>> driftOverrides = new ConcurrentHashMap<>();

    public void applyDrift(Long deviceId, String parameter, String value) {
        driftOverrides.computeIfAbsent(deviceId, k -> new HashMap<>()).put(parameter, value);
    }

    public void resetDrift(Long deviceId) {
        driftOverrides.remove(deviceId);
    }

    public Map<String, String> getDriftOverrides(Long deviceId) {
        return driftOverrides.getOrDefault(deviceId, new HashMap<>());
    }
}
