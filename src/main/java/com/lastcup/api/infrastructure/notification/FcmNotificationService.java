package com.lastcup.api.infrastructure.notification;

import com.google.firebase.messaging.ApnsConfig;
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
                .setApnsConfig(ApnsConfig.builder().build())
                .addAllTokens(tokens)
                .build();
        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.debug("FCM multicast sent. success={}, failure={}", response.getSuccessCount(), response.getFailureCount());
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

