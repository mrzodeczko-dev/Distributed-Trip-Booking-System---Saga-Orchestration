package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.application.command.FlightCommand;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.port.in.ProcessFlightCommandUseCase;
import com.rzodeczko.infrastructure.messaging.dto.FlightCommandMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FlightCommandListenerTest {

    @Mock
    private ProcessFlightCommandUseCase processFlightCommandUseCase;

    @Captor
    private ArgumentCaptor<FlightCommand> commandCaptor;

    private FlightCommandListener listener;

    private static final UUID SAGA_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listener = new FlightCommandListener(processFlightCommandUseCase);
    }

    @Nested
    @DisplayName("onCommand")
    class OnCommand {

        @Test
        void shouldMapReserveMessageToCommandAndDelegate() {
            FlightCommandMessageDto message = new FlightCommandMessageDto(
                    SAGA_ID, "RESERVE", "Jan Kowalski", "Mars", new BigDecimal("1500.00")
            );

            listener.onCommand(message);

            verify(processFlightCommandUseCase).handle(commandCaptor.capture());
            FlightCommand command = commandCaptor.getValue();
            assertThat(command.sagaId()).isEqualTo(SAGA_ID);
            assertThat(command.action()).isEqualTo(SagaAction.RESERVE);
            assertThat(command.customerName()).isEqualTo("Jan Kowalski");
            assertThat(command.destination()).isEqualTo("Mars");
            assertThat(command.amount()).isEqualByComparingTo("1500.00");
        }

        @Test
        void shouldMapCancelMessageToCommandAndDelegate() {
            FlightCommandMessageDto message = new FlightCommandMessageDto(
                    SAGA_ID, "CANCEL", "Jan Kowalski", "Mars", new BigDecimal("1500.00")
            );

            listener.onCommand(message);

            verify(processFlightCommandUseCase).handle(commandCaptor.capture());
            assertThat(commandCaptor.getValue().action()).isEqualTo(SagaAction.CANCEL);
        }

        @Test
        void shouldThrowForUnknownAction() {
            FlightCommandMessageDto message = new FlightCommandMessageDto(
                    SAGA_ID, "UNKNOWN_ACTION", "Jan Kowalski", "Mars", BigDecimal.TEN
            );

            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> listener.onCommand(message)
            );
        }
    }
}
