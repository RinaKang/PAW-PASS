package com.pawpass.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * 업로드된 파일(프로필 이미지 등, 2026-09-15 추가)을 file.upload-dir 경로에서 읽어 /uploads/**로 그대로
 * 서빙한다 - ProfileImageStorage가 저장한 파일과 여기 경로가 반드시 일치해야 한다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + Path.of(uploadDir).toAbsolutePath() + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
