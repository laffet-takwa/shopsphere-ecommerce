package com.shopsphere.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * The parameters are explicitly cast because Hibernate 6 otherwise leaves an unbound {@code null}
     * untyped, and PostgreSQL infers {@code bytea} for it. That made {@code lower(bytea)} blow up and
     * took the whole catalog endpoint down whenever {@code category} or {@code keyword} was absent.
     */
    @Query("select p from Product p where p.active = true and " +
            "(:category is null or lower(p.category) = lower(cast(:category as string))) and " +
            "(:keyword is null or lower(p.name) like lower(concat('%', cast(:keyword as string), '%')) " +
            "or lower(p.description) like lower(concat('%', cast(:keyword as string), '%')))")
    Page<Product> browse(@Param("category") String category, @Param("keyword") String keyword, Pageable pageable);
}