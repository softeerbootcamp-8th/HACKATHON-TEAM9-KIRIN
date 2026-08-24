package com.kirin.superservice.locker.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.exception.LockerNotFoundException;
import com.kirin.superservice.locker.service.LockerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LockerController.class)
class LockerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LockerService lockerService;

    @Test
    void 존재하는_보관함의_잠금상태를_조회하면_200과_잠금상태를_반환한다() throws Exception {
        // given
        Locker locker = new Locker(1L, LockStatus.LOCKED);
        given(lockerService.getLocker(1L)).willReturn(locker);

        // when & then
        mockMvc.perform(get("/api/lockers/1/lock-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockerId").value(1))
                .andExpect(jsonPath("$.lockStatus").value("LOCKED"));
    }

    @Test
    void 존재하지_않는_보관함의_잠금상태를_조회하면_404를_반환한다() throws Exception {
        // given
        given(lockerService.getLocker(999L)).willThrow(new LockerNotFoundException(999L));

        // when & then
        mockMvc.perform(get("/api/lockers/999/lock-status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOCKER_NOT_FOUND"));
    }

    @Test
    void 보관함의_잠금상태를_변경하면_200과_변경된_상태를_반환한다() throws Exception {
        // given
        Locker locker = new Locker(1L, LockStatus.UNLOCKED);
        given(lockerService.changeLockStatus(1L, LockStatus.UNLOCKED)).willReturn(locker);

        // when & then
        mockMvc.perform(patch("/api/lockers/1/lock-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lockStatus\":\"UNLOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockerId").value(1))
                .andExpect(jsonPath("$.lockStatus").value("UNLOCKED"));
    }
}
