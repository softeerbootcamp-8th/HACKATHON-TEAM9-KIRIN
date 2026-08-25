package com.kirin.superservice.product.controller;

import com.kirin.superservice.global.auth.LoginMember;
import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.dto.request.RegisterProductRequest;
import com.kirin.superservice.product.dto.request.ReserveLockerRequest;
import com.kirin.superservice.product.dto.response.ProductListResponse;
import com.kirin.superservice.product.dto.response.ProductResponse;
import com.kirin.superservice.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ProductResponse registerProduct(
            @LoginMember Long memberId,
            @RequestBody @Valid RegisterProductRequest request) {
        Product product = productService.registerProduct(request, memberId);
        return ProductResponse.fromEntity(product);
    }

    @PostMapping("/{productId}/locker-reservation")
    public ProductResponse reserveLocker(
            @LoginMember Long memberId,
            @PathVariable Long productId,
            @RequestBody @Valid ReserveLockerRequest request) {
        Product product = productService.reserveLocker(productId, request, memberId);
        return ProductResponse.fromEntity(product);
    }

    @PostMapping("/{productId}/locker-reservation/cancel")
    public ProductResponse cancelLockerReservation(
            @LoginMember Long memberId,
            @PathVariable Long productId) {
        Product product = productService.cancelLockerReservation(productId, memberId);
        return ProductResponse.fromEntity(product);
    }

    @PostMapping("/{productId}/deposit-start")
    public ProductResponse startDeposit(
            @LoginMember Long memberId,
            @PathVariable Long productId) {
        Product product = productService.startDeposit(productId, memberId);
        return ProductResponse.fromEntity(product);
    }

    @PostMapping("/{productId}/deposit-complete")
    public ProductResponse completeDeposit(
            @LoginMember Long memberId,
            @PathVariable Long productId) {
        Product product = productService.completeDeposit(productId, memberId);
        return ProductResponse.fromEntity(product);
    }

    @PostMapping("/{productId}/recovery-start")
    public ProductResponse startRecovery(
            @LoginMember Long memberId,
            @PathVariable Long productId) {
        Product product = productService.startRecovery(productId, memberId);
        return ProductResponse.fromEntity(product);
    }

    @PostMapping("/{productId}/recovery-complete")
    public ProductResponse completeRecovery(
            @LoginMember Long memberId,
            @PathVariable Long productId) {
        Product product = productService.completeRecovery(productId, memberId);
        return ProductResponse.fromEntity(product);
    }

    @GetMapping
    public ProductListResponse getProducts(
            @RequestParam(defaultValue = "SELLING") ProductStatus status) {
        return ProductListResponse.fromEntities(productService.findAllProductsByStatus(status));
    }

    @GetMapping("/me")
    public ProductListResponse getMyProducts(
            @LoginMember Long memberId,
            @RequestParam(required = false) ProductStatus status) {
        return ProductListResponse.fromEntities(productService.findAllProductsBySellerMemberId(memberId, status));
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable Long productId) {
        Product product = productService.getProduct(productId);
        return ProductResponse.fromEntity(product);
    }

}
