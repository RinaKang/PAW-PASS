package com.pawpass.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @Value로 주입되는 uploadDir/baseUrl은 이 클래스가 Spring 없이(순수 단위 테스트로) 만들어질 때 채워지지
 * 않으므로, PetService의 @GeneratedValue id를 세팅하던 것과 같은 방식으로 ReflectionTestUtils로 직접 넣는다.
 */
class ProfileImageStorageTest {

    @TempDir
    Path tempDir;

    private ProfileImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new ProfileImageStorage();
        ReflectionTestUtils.setField(storage, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(storage, "baseUrl", "http://localhost:8080");
    }

    @Test
    void 정상_이미지를_저장하면_파일이_실제로_생기고_공개_URL을_반환한다() {
        MultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        String url = storage.store(image, 1L);

        assertThat(url).startsWith("http://localhost:8080/uploads/profile-images/1_").endsWith(".jpg");
        String filename = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.exists(tempDir.resolve("profile-images").resolve(filename))).isTrue();
    }

    @Test
    void 빈_파일은_거부한다() {
        MultipartFile empty = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> storage.store(empty, 1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 허용되지_않는_형식은_거부한다() {
        MultipartFile pdf = new MockMultipartFile("image", "doc.pdf", "application/pdf", new byte[]{1});

        assertThatThrownBy(() -> storage.store(pdf, 1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 용량_상한을_넘으면_거부한다() {
        MultipartFile tooBig = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[6 * 1024 * 1024]);

        assertThatThrownBy(() -> storage.store(tooBig, 1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 우리가_저장한_파일이면_삭제한다() throws IOException {
        MultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1});
        String url = storage.store(image, 1L);
        String filename = url.substring(url.lastIndexOf('/') + 1);
        Path saved = tempDir.resolve("profile-images").resolve(filename);
        assertThat(Files.exists(saved)).isTrue();

        storage.deleteIfManaged(url);

        assertThat(Files.exists(saved)).isFalse();
    }

    // 구글 로그인 시 받아온 picture URL(우리가 저장한 파일이 아님)은 절대 건드리면 안 된다.
    @Test
    void 우리가_저장하지_않은_URL은_건드리지_않는다() {
        storage.deleteIfManaged("https://lh3.googleusercontent.com/구글사진");
        // 예외 없이 조용히 넘어가면 성공 - 삭제 시도 자체를 안 함
    }

    @Test
    void null_picture는_그냥_넘어간다() {
        storage.deleteIfManaged(null);
    }
}
