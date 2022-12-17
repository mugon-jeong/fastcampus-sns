package com.fastcampus.sns.service;

import com.fastcampus.sns.exception.ErrorCode;
import com.fastcampus.sns.exception.SnsApplicationException;
import com.fastcampus.sns.repository.EmitterRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    public static final String ALARM_NAME = "alarm";
    private final static Long DEFAULT_TIMEOUT = 60L * 1000 * 60;
    private final EmitterRepository emitterRepository;


    public SseEmitter connectAlarm(Integer userId) {
        SseEmitter sseEmitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitterRepository.save(userId,sseEmitter);
        sseEmitter.onCompletion(() -> emitterRepository.delete(userId));
        sseEmitter.onTimeout(() -> emitterRepository.delete(userId));
        try {
            sseEmitter.send(SseEmitter.event()
                                      .id("id")
                                      .name(ALARM_NAME)
                                      .data("connect completed"));

        } catch (IOException exception) {
            throw new SnsApplicationException(ErrorCode.ALARM_CONNECT_ERROR);
        }
        return sseEmitter;
    }

    // 이벤트 전송
    public void send(Integer alarmId, Integer userId) {
        emitterRepository.get(userId)
                         .ifPresentOrElse(sseEmitter -> {
                             try {
                                 sseEmitter.send(SseEmitter.event()
                                                           .id(alarmId.toString())
                                                           .name(ALARM_NAME)
                                                           .data("new alarm"));

                             } catch (IOException e) {
                                 emitterRepository.delete(userId);
                                 throw new SnsApplicationException(ErrorCode.ALARM_CONNECT_ERROR);
                             }
                         }, () -> log.info("no emitter founded"));
    }
}
