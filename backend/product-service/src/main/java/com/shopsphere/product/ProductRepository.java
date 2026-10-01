package com.shopsphere.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("select p from Product p where p.active = true and " +
            "(:category is null or lower(p.category) = lower(:category)) and " +
            "(:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')) " +
            "or lower(p.description) like lower(concat('%', :keyword, '%')))")
    Page<Product> browse(@Param("category") String category, @Param("keyword") String keyword, Pageable pageable);
}