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

    List<Product> findAllByStatusOrderByCreatedAtDescIdDesc(ProductStatus status);

    List<Product> findAllBySellerMemberIdOrderByCreatedAtDescIdDesc(Long sellerMemberId);

    List<Product> findAllBySellerMemberIdAndStatusOrderByCreatedAtDescIdDesc(
            Long sellerMemberId, ProductStatus status);

    List<Product> findAllByStatusAndReservationExpiresAtLessThanEqual(
            ProductStatus status, LocalDateTime reservationExpiresAt);

    List<Product> findAllByStatusAndSellingExpiresAtLessThanEqual(
            ProductStatus status, LocalDateTime sellingExpiresAt);

    /**
     * lockerId는 물품이 회수·수령완료된 뒤에도 지워지지 않고 이력으로 남기 때문에, 같은
     * lockerId를 가진 물품이 여러 건 존재할 수 있다. 가장 최근에 등록된 물품이 현재 그
     * 사물함을 점유 중인 물품이다.
     */
    Optional<Product> findFirstByLockerIdOrderByCreatedAtDescIdDesc(Long lockerId);

    List<Product> findAllByLockerIdIsNotNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);
}
