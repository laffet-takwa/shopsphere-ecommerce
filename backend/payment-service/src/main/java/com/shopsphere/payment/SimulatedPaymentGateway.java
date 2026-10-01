package com.shopsphere.payment;

import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real payment processor. No provider SDK, no card data, no network egress.
 *
 * <p>Declines are deterministic rather than random so that the same order always produces the same
 * outcome. That keeps the demo reproducible and keeps tests meaningful; a production integration
 * would obviously invert this and model real processor decline codes.
 */
@Component
public class SimulatedPaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentGateway.class);

    private final BigDecimal declineAboveAmount;

    public SimulatedPaymentGateway(
            @Value("${shopsphere.payment.decline-above-amount:10000}") BigDecimal declineAboveAmount) {
        this.declineAboveAmount = declineAboveAmount;
    }

    /**
     * @return an authorisation result carrying either an approval flag or a decline reason
     */
    public Authorization authorize(Long orderId, BigDecimal amount) {
        String reference = "SIM-" + UUID.randomUUID();
        if (amount.compareTo(declineAboveAmount) > 0) {
            String reason = "Amount " + amount.toPlainString()
                    + " exceeds the simulated card limit of " + declineAboveAmount.toPlainString();
            log.info("simulated decline orderId={} reference={} reason={}", orderId, reference, reason);
            return new Authorization(false, reference, reason);
        }
        return new Authorization(true, reference, null);
    }

    /**
     * @param transactionReference always present: a decline still produces a processor reference
     */
    public record Authorization(boolean approved, String transactionReference, String reason) {
    }
}