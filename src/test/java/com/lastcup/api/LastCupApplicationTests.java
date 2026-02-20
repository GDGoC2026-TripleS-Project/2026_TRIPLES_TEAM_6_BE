package com.lastcup.api;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class LastCupApplicationTests {

    @MockitoBean
    FirebaseApp firebaseApp;

    @MockitoBean
    FirebaseMessaging firebaseMessaging;

    @Test
    void contextLoads() {
    }
}
