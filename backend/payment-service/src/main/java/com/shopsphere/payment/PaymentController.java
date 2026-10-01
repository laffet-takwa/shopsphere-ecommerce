package com.shopsphere.payment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentRepository payments;
    public PaymentController(PaymentRepository payments) { this.payments = payments; }
    @GetMapping("/{orderId}")
    public PaymentResponse get(@PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId,
                               @RequestHeader("X-User-Role") String role) {
        return PaymentResponse.from(findVisible(orderId, userId, role));
    }
    @PostMapping("/process")
    public PaymentResponse process(@Valid @RequestBody ProcessPaymentRequest request,
                                   @RequestHeader("X-User-Id") Long userId,
                                   @RequestHeader("X-User-Role") String role) {
        Payment payment = payments.findByOrderId(request.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Payment processing starts after inventory is reserved"));
        if (!"ADMIN".equals(role) && !payment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return PaymentResponse.from(payment);
    }
    private Payment findVisible(Long orderId, Long userId, String role) {
        return payments.findByOrderId(orderId).filter(payment -> "ADMIN".equals(role) || payment.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    public record ProcessPaymentRequest(@NotNull Long orderId) {}
    public record PaymentResponse(Long id, Long orderId, Long userId, java.math.BigDecimal amount,
                                  String currency, PaymentStatus status, String transactionReference,
                                  java.time.Instant createdAt) {
        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getUserId(),
                    payment.getAmount(), payment.getCurrency(), payment.getStatus(),
                    payment.getTransactionReference(), payment.getCreatedAt());
        }
    }
}