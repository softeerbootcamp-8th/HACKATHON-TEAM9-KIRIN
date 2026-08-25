package com.kirin.superservice.product.controller;

import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import com.kirin.superservice.product.dto.request.CancelLockerReservationRequest;
import com.kirin.superservice.product.dto.request.CompleteDepositRequest;
import com.kirin.superservice.product.dto.request.RegisterProductRequest;
import com.kirin.superservice.product.dto.request.ReserveLockerRequest;
import com.kirin.superservice.product.dto.request.StartDepositRequest;
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
    public ProductResponse registerProduct(@RequestBody @Valid RegisterProductRequest request) {
        Product product = productService.registerProduct(request);
        return ProductResponse.fromEntity(product);
    }

    @PostMapping("/{productId}/locker-reservation")
    public ProductResponse reserveLocker(
            @PathVariable Long productId,
            @RequestBody @Valid ReserveLockerRequest request) {
        Product product = productService.reserveLocker(productId, request);
        return ProductResponse.fromEntity(product);
    }

    @PostMapping("/{productId}/locker-reservation/cancel")
    public ProductResponse cancelLockerReservation(
            @PathVariable Long productId,
            @RequestBody @Valid CancelLockerReservationRequest request) {
        Product product = productService.cancelLockerReservation(productId, request);
        return ProductResponse.fromEntity(product);
    }

    @PostMapping("/{productId}/deposit-start")
    public ProductResponse startDeposit(
            @PathVariable Long productId,
            @RequestBody @Valid StartDepositRequest request) {
        Product product = productService.startDeposit(productId, request);
        return ProductResponse.fromEntity(product);
    }

    @PostMapping("/{productId}/deposit-complete")
    public ProductResponse completeDeposit(
            @PathVariable Long productId,
            @RequestBody @Valid CompleteDepositRequest request) {
        Product product = productService.completeDeposit(productId, request);
        return ProductResponse.fromEntity(product);
    }

    @GetMapping
    public ProductListResponse getProducts(
            @RequestParam(defaultValue = "SELLING") ProductStatus status) {
        return ProductListResponse.fromEntities(productService.findAllProductsByStatus(status));
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable Long productId) {
        Product product = productService.getProduct(productId);
        return ProductResponse.fromEntity(product);
    }

}
