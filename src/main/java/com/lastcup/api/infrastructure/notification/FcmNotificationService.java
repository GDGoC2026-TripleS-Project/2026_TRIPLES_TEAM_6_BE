package com.lastcup.api.infrastructure.notification;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.BatchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FcmNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FcmNotificationService.class);

    private final FirebaseMessaging firebaseMessaging;

    public FcmNotificationService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    public void sendToTokens(List<String> tokens, String title, String body) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .putHeader("apns-push-type", "alert") // 알림 타입 설정, 99%는 alret 사용한다고 함.
                        /*
                        우선순위 설정
                        10: 즉시 전달, 배터리 소모 있음
                        5: 절전 전달, 배터리 상태에 따라 묶어서 전달
                        1: 최저 우선순위, 기기가 가장 여유로울 때 전달, 상당한 지연 가능
                         */
                        .putHeader("apns-priority", "10")
                        /*
                        Apple이 실제로 읽는 payload 본체.
                        iOS는 FCM의 Notification 블록을 보지 않고 aps 안의 내용만 본다.
                         */
                        .setAps(Aps.builder()
                                // 알림 배너에 표시될 제목과 본문. 없으면 알림 UI 안뜸.
                                .setAlert(ApsAlert.builder()
                                        .setTitle(title) // 제목
                                        .setBody(body) // 본문
                                        .build())
                                .setSound("default") // 알림 도착시 기본 알림음 재생
                                .build())
                        .build())
                .addAllTokens(tokens)
                .build();
        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("FCM multicast sent. success={}, failure={}", response.getSuccessCount(), response.getFailureCount());
            if (response.getFailureCount() > 0) {
                logFailedResponses(tokens, response.getResponses());
            }
        } catch (FirebaseMessagingException e) {
            log.error("FCM multicast send failed. tokens={}", tokens.size(), e);
            throw new IllegalStateException("FCM send failed", e);
        }
    }

    private void logFailedResponses(List<String> tokens, List<SendResponse> responses) {
        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            if (response.isSuccessful()) {
                continue;
            }
            log.warn("FCM send failed for token index {}. token={}, error={}",
                    i,
                    tokens.get(i),
                    response.getException() != null ? response.getException().getMessage() : "unknown");
        }
    }
}

