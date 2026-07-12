package com.rzodeczko.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Payment")
class PaymentTest {

    @Nested
    @DisplayName("charge factory")
    class Charge {

        @Test
        @DisplayName("creates a CHARGED payment with generated id and creation timestamp")
        void createsChargedPayment() {
            UUID sagaId = UUID.randomUUID();
            BigDecimal amount = BigDecimal.valueOf(150);

            Instant before = Instant.now();
            Payment payment = Payment.charge(sagaId, "Alice", amount);
            Instant after = Instant.now();

            assertThat(payment.getId()).isNotNull();
            assertThat(payment.getSagaId()).isEqualTo(sagaId);
            assertThat(payment.getCustomerName()).isEqualTo("Alice");
            assertThat(payment.getAmount()).isEqualByComparingTo(amount);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CHARGED);
            assertThat(payment.getCreatedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("generates a different id for each charge")
        void generatesUniqueIds() {
            UUID sagaId = UUID.randomUUID();
            Payment p1 = Payment.charge(sagaId, "Alice", BigDecimal.TEN);
            Payment p2 = Payment.charge(sagaId, "Alice", BigDecimal.TEN);

            assertThat(p1.getId()).isNotEqualTo(p2.getId());
        }
    }

    @Nested
    @DisplayName("restore factory")
    class Restore {

        @Test
        @DisplayName("rebuilds a payment preserving all given fields")
        void restoresPayment() {
            UUID id = UUID.randomUUID();
            UUID sagaId = UUID.randomUUID();
            BigDecimal amount = BigDecimal.valueOf(500);
            Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");

            Payment payment = Payment.restore(id, sagaId, "Bob", amount, PaymentStatus.REFUNDED, createdAt);

            assertThat(payment.getId()).isEqualTo(id);
            assertThat(payment.getSagaId()).isEqualTo(sagaId);
            assertThat(payment.getCustomerName()).isEqualTo("Bob");
            assertThat(payment.getAmount()).isEqualByComparingTo(amount);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    @Nested
    @DisplayName("refund")
    class Refund {

        @Test
        @DisplayName("transitions status from CHARGED to REFUNDED")
        void refundsChargedPayment() {
            Payment payment = Payment.charge(UUID.randomUUID(), "Alice", BigDecimal.TEN);

            payment.refund();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("is idempotent when called multiple times")
        void refundIsIdempotent() {
            Payment payment = Payment.charge(UUID.randomUUID(), "Alice", BigDecimal.TEN);

            payment.refund();
            payment.refund();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }
    }
}
