package com.kirin.superservice.image.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kirin.superservice.global.auth.SessionConst;
import com.kirin.superservice.global.slack.SlackErrorNotifier;
import com.kirin.superservice.image.exception.InvalidImageFileException;
import com.kirin.superservice.image.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImageController.class)
class ImageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ImageService imageService;

    @MockitoBean
    SlackErrorNotifier slackErrorNotifier;

    @Test
    void 유효한_이미지를_업로드하면_201과_이미지URL을_반환한다() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "product.jpg", "image/jpeg", "dummy-image-bytes".getBytes());
        given(imageService.storeImage(any())).willReturn("/api/images/abc123.jpg");

        // when & then
        mockMvc.perform(multipart("/api/images")
                        .file(file)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value("/api/images/abc123.jpg"));
    }

    @Test
    void 지원하지_않는_형식의_파일을_업로드하면_400을_반환한다() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "dummy".getBytes());
        given(imageService.storeImage(any()))
                .willThrow(new InvalidImageFileException("지원하지 않는 파일 형식입니다"));

        // when & then
        mockMvc.perform(multipart("/api/images")
                        .file(file)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IMAGE_FILE"));
    }

    @Test
    void 로그인하지_않고_업로드하면_401을_반환한다() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "product.jpg", "image/jpeg", "dummy".getBytes());

        // when & then
        mockMvc.perform(multipart("/api/images").file(file))
                .andExpect(status().isUnauthorized());
    }
}
