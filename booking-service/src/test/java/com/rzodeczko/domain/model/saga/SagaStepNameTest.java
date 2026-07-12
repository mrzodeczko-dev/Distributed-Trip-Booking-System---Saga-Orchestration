package com.rzodeczko.domain.model.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SagaStepNameTest {

    @Test
    @DisplayName("FLIGHT.next() should return HOTEL")
    void flightNextShouldBeHotel() {
        assertThat(SagaStepName.FLIGHT.next()).hasValue(SagaStepName.HOTEL);
    }

    @Test
    @DisplayName("HOTEL.next() should return PAYMENT")
    void hotelNextShouldBePayment() {
        assertThat(SagaStepName.HOTEL.next()).hasValue(SagaStepName.PAYMENT);
    }

    @Test
    @DisplayName("PAYMENT.next() should return empty (last step)")
    void paymentNextShouldBeEmpty() {
        assertThat(SagaStepName.PAYMENT.next()).isEmpty();
    }

    @Test
    @DisplayName("Steps should be ordered: FLIGHT, HOTEL, PAYMENT")
    void stepsShouldBeOrdered() {
        assertThat(SagaStepName.values())
                .containsExactly(SagaStepName.FLIGHT, SagaStepName.HOTEL, SagaStepName.PAYMENT);
    }
}
