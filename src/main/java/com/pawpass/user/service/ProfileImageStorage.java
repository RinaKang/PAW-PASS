package com.pawpass.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * 프로필 이미지를 로컬 디스크에 저장한다(2026-09-15 추가, 사용자와 논의해서 클라우드 스토리지 대신 로컬
 * 디스크로 결정 - 서버 재배포/이전 시 파일이 사라지는 한계는 감수). WebMvcConfig가 file.upload-dir을
 * /uploads/**로 그대로 서빙하므로 여기서 저장하는 경로와 정확히 맞아야 한다.
 */
@Slf4j
@Component
public class ProfileImageStorage {

    private static final String SUBDIR = "profile-images";
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    /** 검증 후 저장하고, 프론트가 바로 쓸 수 있는 절대 URL(baseUrl 포함)을 반환한다. */
    public String store(MultipartFile file, Long userId) {
        validate(file);
        String filename = userId + "_" + UUID.randomUUID() + EXTENSION_BY_CONTENT_TYPE.get(file.getContentType());
        Path targetDir = Path.of(uploadDir, SUBDIR);
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetDir.resolve(filename));
        } catch (IOException e) {
            throw new IllegalStateException("프로필 이미지 저장에 실패했습니다.", e);
        }
        return publicPathPrefix() + filename;
    }

    /**
     * 재업로드로 더 이상 안 쓰이게 된 예전 파일을 지운다 - 지우지 않으면 재업로드할 때마다 디스크에
     * 파일이 계속 쌓인다. pictureUrl이 우리가 저장한 파일이 아니면(구글 계정 사진 URL 등) 그냥 넘어간다
     * (건드리면 안 되는 외부 URL이라서).
     */
    public void deleteIfManaged(String pictureUrl) {
        if (pictureUrl == null || !pictureUrl.startsWith(publicPathPrefix())) {
            return;
        }
        String filename = pictureUrl.substring(publicPathPrefix().length());
        try {
            Files.deleteIfExists(Path.of(uploadDir, SUBDIR, filename));
        } catch (IOException e) {
            log.warn("예전 프로필 이미지 파일 삭제 실패(다음 정리 때 재시도 없이 그냥 방치됨): {}", pictureUrl, e);
        }
    }

    private String publicPathPrefix() {
        return baseUrl + "/uploads/" + SUBDIR + "/";
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지가 없습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("이미지 용량은 5MB를 넘을 수 없습니다.");
        }
        if (!EXTENSION_BY_CONTENT_TYPE.containsKey(file.getContentType())) {
            throw new IllegalArgumentException("jpg, png, webp 형식의 이미지만 업로드할 수 있습니다.");
        }
    }
}
