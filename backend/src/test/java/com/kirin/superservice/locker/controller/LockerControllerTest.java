package com.kirin.superservice.locker.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kirin.superservice.global.auth.SessionConst;
import com.kirin.superservice.locker.domain.Locker;
import com.kirin.superservice.locker.domain.LockStatus;
import com.kirin.superservice.locker.domain.UsageStatus;
import com.kirin.superservice.global.slack.SlackErrorNotifier;
import com.kirin.superservice.locker.exception.LockerAccessDeniedException;
import com.kirin.superservice.locker.exception.LockerNotFoundException;
import com.kirin.superservice.locker.service.LockerService;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.exception.SellerMismatchException;
import com.kirin.superservice.product.service.ProductService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    @MockitoBean
    ProductService productService;

    @MockitoBean
    SlackErrorNotifier slackErrorNotifier;

    @Test
    void 비어있는_보관함을_조회하면_200과_잠금상태와_사용상태를_반환한다() throws Exception {
        // given
        given(lockerService.findAllLockers()).willReturn(List.of(
                new Locker(1L, LockStatus.LOCKED, UsageStatus.AVAILABLE)));
        given(productService.findAllProductsByLockerId()).willReturn(Map.of());

        // when & then
        mockMvc.perform(get("/api/lockers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockers[0].lockerId").value(1))
                .andExpect(jsonPath("$.lockers[0].lockStatus").value("LOCKED"))
                .andExpect(jsonPath("$.lockers[0].usageStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.lockers[0].isMine").value(false));
    }

    @Test
    void 본인이_예약중인_보관함을_조회하면_예약상태와_남은_예약시간을_반환한다() throws Exception {
        // given
        LocalDateTime reservedAt = LocalDateTime.now();
        Product product = new Product("item", 1000L, "설명", null, 1L, "seller");
        product.reserveLocker(1L, reservedAt, reservedAt.plusHours(4));
        given(lockerService.findAllLockers()).willReturn(List.of(
                new Locker(1L, LockStatus.LOCKED, UsageStatus.RESERVED)));
        given(productService.findAllProductsByLockerId()).willReturn(Map.of(1L, product));

        // when & then
        mockMvc.perform(get("/api/lockers")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockers[0].isMine").value(true))
                .andExpect(jsonPath("$.lockers[0].usageStatus").value("RESERVED"))
                .andExpect(jsonPath("$.lockers[0].reservationExpiresAt").exists())
                .andExpect(jsonPath("$.lockers[0].sellingExpiresAt").doesNotExist());
    }

    @Test
    void 본인이_판매중인_보관함을_조회하면_판매상태와_남은_판매기간을_반환한다() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product("item", 1000L, "설명", null, 1L, "seller");
        product.reserveLocker(1L, now, now.plusHours(4));
        product.startDeposit(now);
        product.completeDeposit(now, now.plusDays(7));
        given(lockerService.findAllLockers()).willReturn(List.of(
                new Locker(1L, LockStatus.LOCKED, UsageStatus.OCCUPIED)));
        given(productService.findAllProductsByLockerId()).willReturn(Map.of(1L, product));

        // when & then
        mockMvc.perform(get("/api/lockers")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockers[0].isMine").value(true))
                .andExpect(jsonPath("$.lockers[0].usageStatus").value("OCCUPIED"))
                .andExpect(jsonPath("$.lockers[0].sellingExpiresAt").exists())
                .andExpect(jsonPath("$.lockers[0].reservationExpiresAt").doesNotExist());
    }

    @Test
    void 타인이_예약중인_보관함을_조회하면_예약여부_없이_사용중으로만_반환한다() throws Exception {
        // given
        LocalDateTime reservedAt = LocalDateTime.now();
        Product product = new Product("item", 1000L, "설명", null, 1L, "seller");
        product.reserveLocker(1L, reservedAt, reservedAt.plusHours(4));
        given(lockerService.findAllLockers()).willReturn(List.of(
                new Locker(1L, LockStatus.LOCKED, UsageStatus.RESERVED)));
        given(productService.findAllProductsByLockerId()).willReturn(Map.of(1L, product));

        // when & then
        mockMvc.perform(get("/api/lockers")
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockers[0].isMine").value(false))
                .andExpect(jsonPath("$.lockers[0].usageStatus").value("OCCUPIED"))
                .andExpect(jsonPath("$.lockers[0].reservationExpiresAt").doesNotExist());
    }

    @Test
    void 로그인하지_않고_판매중인_보관함을_조회하면_사용중으로만_반환한다() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product("item", 1000L, "설명", null, 1L, "seller");
        product.reserveLocker(1L, now, now.plusHours(4));
        product.startDeposit(now);
        product.completeDeposit(now, now.plusDays(7));
        given(lockerService.findAllLockers()).willReturn(List.of(
                new Locker(1L, LockStatus.LOCKED, UsageStatus.OCCUPIED)));
        given(productService.findAllProductsByLockerId()).willReturn(Map.of(1L, product));

        // when & then
        mockMvc.perform(get("/api/lockers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockers[0].isMine").value(false))
                .andExpect(jsonPath("$.lockers[0].usageStatus").value("OCCUPIED"))
                .andExpect(jsonPath("$.lockers[0].sellingExpiresAt").doesNotExist());
    }

    @Test
    void 존재하는_보관함의_잠금상태를_조회하면_200과_잠금상태를_반환한다() throws Exception {
        // given
        Locker locker = new Locker(1L, LockStatus.LOCKED, UsageStatus.AVAILABLE);
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
    void 판매자가_잠금상태를_변경하면_200과_변경된_상태를_반환한다() throws Exception {
        // given
        Locker locker = new Locker(1L, LockStatus.UNLOCKED, UsageStatus.OCCUPIED);
        willDoNothing().given(productService).validateLockerSeller(1L, 1L);
        given(lockerService.changeLockStatus(1L, LockStatus.UNLOCKED)).willReturn(locker);

        // when & then
        mockMvc.perform(patch("/api/lockers/1/lock-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content("{\"lockStatus\":\"UNLOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockerId").value(1))
                .andExpect(jsonPath("$.lockStatus").value("UNLOCKED"));
    }

    @Test
    void 로그인하지_않고_잠금상태를_변경하면_401을_반환한다() throws Exception {
        // when & then
        mockMvc.perform(patch("/api/lockers/1/lock-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lockStatus\":\"UNLOCKED\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 판매자가_아닌_회원이_잠금상태를_변경하면_403을_반환한다() throws Exception {
        // given
        willThrow(new SellerMismatchException(1L))
                .given(productService).validateLockerSeller(1L, 2L);

        // when & then
        mockMvc.perform(patch("/api/lockers/1/lock-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 2L)
                        .content("{\"lockStatus\":\"UNLOCKED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SELLER_MISMATCH"));
    }

    @Test
    void 판매중인_물품이_없는_보관함의_잠금상태를_변경하면_403을_반환한다() throws Exception {
        // given
        willThrow(new LockerAccessDeniedException(1L))
                .given(productService).validateLockerSeller(1L, 1L);

        // when & then
        mockMvc.perform(patch("/api/lockers/1/lock-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr(SessionConst.LOGIN_MEMBER_ID, 1L)
                        .content("{\"lockStatus\":\"UNLOCKED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LOCKER_ACCESS_DENIED"));
    }
}
