package com.hjj.lingxibi.manager;

import com.hjj.lingxibi.model.entity.Chart;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SSEManager {

    private final Map<Long, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    public SseEmitter createConnection(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        sseEmitters.put(userId, emitter);
        emitter.onCompletion(() -> sseEmitters.remove(userId));
        emitter.onTimeout(() -> sseEmitters.remove(userId));
        return emitter;
    }

    public void sendChartUpdate(Long userId, Chart chart) {
        SseEmitter emitter = sseEmitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("chart-update")
                        .data(chart));
            } catch (Exception e) {
                sseEmitters.remove(userId);
            }
        }
    }

    public void removeConnection(Long userId) {
        sseEmitters.remove(userId);
    }

}