package com.rzodeczko.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChargeOutcome")
class ChargeOutcomeTest {

    private final UUID sagaId = UUID.randomUUID();

    @Nested
    @DisplayName("attemptCharge")
    class AttemptCharge {

        @Test
        @DisplayName("returns Success when amount is below the credit limit")
        void returnsSuccessBelowLimit() {
            ChargeOutcome outcome = ChargeOutcome.attemptCharge(sagaId, "Alice", BigDecimal.valueOf(999_999));

            assertThat(outcome).isInstanceOf(ChargeOutcome.Success.class);
            ChargeOutcome.Success success = (ChargeOutcome.Success) outcome;
            assertThat(success.payment().getSagaId()).isEqualTo(sagaId);
            assertThat(success.payment().getCustomerName()).isEqualTo("Alice");
            assertThat(success.payment().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(999_999));
            assertThat(success.payment().getStatus()).isEqualTo(PaymentStatus.CHARGED);
        }

        @Test
        @DisplayName("returns Rejected when amount equals the credit limit")
        void returnsRejectedAtLimit() {
            ChargeOutcome outcome = ChargeOutcome.attemptCharge(sagaId, "Alice", BigDecimal.valueOf(1_000_000));

            assertThat(outcome).isInstanceOf(ChargeOutcome.Rejected.class);
            ChargeOutcome.Rejected rejected = (ChargeOutcome.Rejected) outcome;
            assertThat(rejected.reason()).isEqualTo("Payment declined - amount exceeds credit limit");
        }

        @Test
        @DisplayName("returns Rejected when amount exceeds the credit limit")
        void returnsRejectedAboveLimit() {
            ChargeOutcome outcome = ChargeOutcome.attemptCharge(sagaId, "Alice", BigDecimal.valueOf(5_000_000));

            assertThat(outcome).isInstanceOf(ChargeOutcome.Rejected.class);
        }

        @Test
        @DisplayName("returns Success when amount is null")
        void returnsSuccessWhenAmountNull() {
            ChargeOutcome outcome = ChargeOutcome.attemptCharge(sagaId, "Alice", null);

            assertThat(outcome).isInstanceOf(ChargeOutcome.Success.class);
        }
    }
}
