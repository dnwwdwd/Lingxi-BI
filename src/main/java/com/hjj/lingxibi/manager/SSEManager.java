package com.hjj.lingxibi.manager;

import com.hjj.lingxibi.model.entity.Chart;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SSEManager {

    private final Map<Long, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    private final Map<Long, SseEmitter> teamChartSseEmitters = new ConcurrentHashMap<>();

    public SseEmitter createChartSSEConnection(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        sseEmitters.put(userId, emitter);
        emitter.onCompletion(() -> sseEmitters.remove(userId));
        emitter.onTimeout(() -> sseEmitters.remove(userId));
        return emitter;
    }

    public SseEmitter createTeamChartSSEConnection(Long teamId) {
        SseEmitter emitter = new SseEmitter(0L);
        teamChartSseEmitters.put(teamId, emitter);
        emitter.onCompletion(() -> teamChartSseEmitters.remove(teamId));
        emitter.onTimeout(() -> teamChartSseEmitters.remove(teamId));
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

    public void sendTeamChartUpdate(Long teamId, Chart chart) {
        SseEmitter emitter = teamChartSseEmitters.get(teamId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("team-chart-update")
                        .data(chart));
            } catch (Exception e) {
                sseEmitters.remove(teamId);
            }
        }
    }

    public void removeChartSSEConnection(Long userId) {
        sseEmitters.remove(userId);
    }

    public void removeTeamChartSSEConnection(Long teamId) {
        teamChartSseEmitters.remove(teamId);
    }


}