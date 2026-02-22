package com.hotel.service;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.hotel.dto.RoomAvailabilityDto;

@Component
public class AvailabilityPublisher {
    private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    public void publish(RoomAvailabilityDto dto) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(dto);
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
