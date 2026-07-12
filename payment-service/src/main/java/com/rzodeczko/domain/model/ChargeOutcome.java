package com.rzodeczko.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public sealed interface ChargeOutcome permits ChargeOutcome.Success, ChargeOutcome.Rejected {

    BigDecimal CREDIT_LIMIT = BigDecimal.valueOf(1_000_000);

    record Success(Payment payment) implements ChargeOutcome {
    }

    record Rejected(String reason) implements ChargeOutcome {
    }

    static ChargeOutcome attemptCharge(UUID sagaId, String customerName, BigDecimal amount) {
        if (amount != null && amount.compareTo(CREDIT_LIMIT) >= 0) {
            return new Rejected("Payment declined - amount exceeds credit limit");
        }
        return new Success(Payment.charge(sagaId, customerName, amount));
    }
}
