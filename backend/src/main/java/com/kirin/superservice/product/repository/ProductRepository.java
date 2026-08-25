package com.kirin.superservice.product.repository;

import com.kirin.superservice.product.domain.Product;
import com.kirin.superservice.product.domain.ProductStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByStatusOrderByCreatedAtDesc(ProductStatus status);

    List<Product> findAllByStatusAndReservationExpiresAtLessThanEqual(
            ProductStatus status, LocalDateTime reservationExpiresAt);

    List<Product> findAllByStatusAndSellingExpiresAtLessThanEqual(
            ProductStatus status, LocalDateTime sellingExpiresAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);
}
