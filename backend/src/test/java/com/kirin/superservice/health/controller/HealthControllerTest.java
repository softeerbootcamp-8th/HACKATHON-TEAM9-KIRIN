package com.kirin.superservice.health.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.kirin.superservice.global.slack.SlackErrorNotifier;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SlackErrorNotifier slackErrorNotifier;

    @Test
    void 헬스체크를_요청하면_상태값_OK를_반환한다() {
        // when
        var result = mvc.get().uri("/health").exchange();

        // then
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("OK");
    }
}
