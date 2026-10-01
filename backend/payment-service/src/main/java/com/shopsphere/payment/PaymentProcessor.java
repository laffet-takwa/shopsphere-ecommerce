package com.shopsphere.payment;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the single write that turns a reservation into a payment.
 *
 * <p>It is a separate bean on purpose. Spring's {@code @Transactional} works through a proxy, so
 * calling a transactional method from inside the same bean would silently skip the transaction.
 * Keeping the charge here lets {@link PaymentService} commit first and publish to Kafka afterwards.
 */
@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    private final PaymentRepository payments;
    private final SimulatedPaymentGateway gateway;

    public PaymentProcessor(PaymentRepository payments, SimulatedPaymentGateway gateway) {
        this.payments = payments;
        this.gateway = gateway;
    }

    /**
     * Charges an order at most once: the unique constraint on {@code order_id} plus this lookup
     * make a redelivered reservation a no-op rather than a second charge.
     */
    @Transactional
    public Payment charge(Long orderId, Long userId, BigDecimal amount) {
        return payments.findByOrderId(orderId).orElseGet(() -> {
            SimulatedPaymentGateway.Authorization authorization = gateway.authorize(orderId, amount);
            PaymentStatus status = authorization.approved() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            log.info("simulated payment orderId={} status={} reference={}",
                    orderId, status, authorization.transactionReference());
            return payments.save(new Payment(orderId, userId, amount, status, authorization.transactionReference()));
        });
    }
}