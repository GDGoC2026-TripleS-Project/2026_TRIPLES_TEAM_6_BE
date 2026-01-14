package com.lastcup.api.domain.auth.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class NicknameGenerator {

    private static final List<String> ADJECTIVES = List.of("똑똑한", "행복한", "차분한", "용감한", "멋진", "잘생긴", "예쁜", "현명한", "멋쟁이", "사랑받는");
    private static final List<String> ANIMALS = List.of("기린", "고양이", "고래", "여우", "판다", "강아지", "너구리", "참새", "비둘기", "까치", "까마귀", "물개", "돌고래", "해달", "수달", "거미", "뱀", "잠자리", "호랑이", "치타", "표범");

    private final Random random = new Random();

    public String create() {
        return pick(ADJECTIVES) + " " + pick(ANIMALS);
    }

    private String pick(List<String> words) {
        return words.get(random.nextInt(words.size()));
    }
}
