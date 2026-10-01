package com.shopsphere.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private Long orderId;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency = "USD";
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private PaymentStatus status;
    @Column(nullable = false, unique = true) private String transactionReference;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    protected Payment() {}
    public Payment(Long orderId, Long userId, BigDecimal amount, PaymentStatus status, String transactionReference) {
        this.orderId = orderId; this.userId = userId; this.amount = amount; this.status = status;
        this.transactionReference = transactionReference;
    }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getTransactionReference() { return transactionReference; }
    public Instant getCreatedAt() { return createdAt; }
}