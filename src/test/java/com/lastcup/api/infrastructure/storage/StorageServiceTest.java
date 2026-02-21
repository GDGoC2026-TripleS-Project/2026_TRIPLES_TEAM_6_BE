package com.lastcup.api.infrastructure.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * StorageService 단위 테스트.
 * 프로필 이미지 업로드 위임, 파일 유효성 검증 로직을 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock private S3Client s3Client;

    @InjectMocks
    private StorageService storageService;

    // 1. uploadProfileImage — 프로필 이미지 업로드
    // 흐름: validateFile → 디렉토리 경로 생성 → s3Client.upload() 위임

    @Nested
    @DisplayName("프로필 이미지 업로드 (uploadProfileImage)")
    class UploadProfileImage {

        @Test
        @DisplayName("정상 업로드 — S3Client에 올바른 디렉토리로 위임")
        void uploadSuccess() {
            // given
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn("image/png");

            UploadResult expected = new UploadResult(
                    "users/1/profile/uuid.png", "https://s3.url/users/1/profile/uuid.png", 2048);
            // s3Client.upload()의 첫 번째 인자가 "users/1/profile"인지 검증
            when(s3Client.upload(eq("users/1/profile"), eq(file))).thenReturn(expected);

            // when
            UploadResult result = storageService.uploadProfileImage(1L, file);

            // then
            assertEquals("users/1/profile/uuid.png", result.key());
            assertEquals("https://s3.url/users/1/profile/uuid.png", result.url());
            assertEquals(2048, result.size());
            verify(s3Client).upload("users/1/profile", file);
        }

        @Test
        @DisplayName("file이 null — 예외 발생")
        void uploadNullFile() {
            // given & when & then
            // validateFile()에서 file == null 체크
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> storageService.uploadProfileImage(1L, null));

            assertEquals("file is empty", ex.getMessage());
            // 유효성 검증 실패 시 S3 호출 없어야 함
            verify(s3Client, never()).upload(anyString(), any());
        }

        @Test
        @DisplayName("file이 비어있음 — 예외 발생")
        void uploadEmptyFile() {
            // given — file.isEmpty() == true
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(true);

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> storageService.uploadProfileImage(1L, file));

            assertEquals("file is empty", ex.getMessage());
        }

        @Test
        @DisplayName("이미지가 아닌 파일 — 예외 발생")
        void uploadNonImageFile() {
            // given — contentType이 image/로 시작하지 않는 경우
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn("application/pdf");

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> storageService.uploadProfileImage(1L, file));

            assertEquals("file is not image", ex.getMessage());
        }

        @Test
        @DisplayName("contentType이 null — 예외 발생 (이미지 아님으로 판단)")
        void uploadNullContentType() {
            // given — contentType이 null이면 isImage()가 false 반환
            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn(null);

            // when & then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> storageService.uploadProfileImage(1L, file));

            assertEquals("file is not image", ex.getMessage());
        }
    }
}
