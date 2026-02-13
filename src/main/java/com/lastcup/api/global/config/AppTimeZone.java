package com.lastcup.api.global.config;

import java.time.ZoneId;

/**
 * 애플리케이션 전역에서 사용하는 타임존 상수.
 * 모든 날짜/시간 연산에서 명시적으로 사용하여 서버 OS 타임존에 의존하지 않도록 한다.
 */
public final class AppTimeZone {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private AppTimeZone() {
    }
}
