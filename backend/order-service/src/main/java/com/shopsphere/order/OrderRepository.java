package com.shopsphere.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /**
     * {@code items} is a lazy collection, and these responses are mapped after the repository call
     * returns, outside any transaction. Without a fetch join that is a LazyInitializationException
     * on every order read, so the collection is loaded eagerly here instead. The entity graph also
     * avoids the N+1 that a per-order item lookup would cause on the list endpoints.
     */
    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findWithItemsById(Long id);

    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
