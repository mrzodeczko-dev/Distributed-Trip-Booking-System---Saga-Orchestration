package com.rzodeczko.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusTest {

    @Test
    void shouldContainChargedAndRefunded() {
        assertThat(PaymentStatus.values()).containsExactly(PaymentStatus.CHARGED, PaymentStatus.REFUNDED);
    }

    @Test
    void valueOfShouldMatchEnumName() {
        assertThat(PaymentStatus.valueOf("CHARGED")).isEqualTo(PaymentStatus.CHARGED);
        assertThat(PaymentStatus.valueOf("REFUNDED")).isEqualTo(PaymentStatus.REFUNDED);
    }
}
