package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.application.command.HotelCommand;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.port.in.ProcessHotelCommandUseCase;
import com.rzodeczko.infrastructure.messaging.dto.HotelCommandMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class HotelCommandListenerTest {

    @Mock
    private ProcessHotelCommandUseCase processHotelCommandUseCase;

    @Captor
    private ArgumentCaptor<HotelCommand> commandCaptor;

    private HotelCommandListener listener;

    @BeforeEach
    void setUp() {
        listener = new HotelCommandListener(processHotelCommandUseCase);
    }

    @Test
    @DisplayName("should map incoming message to HotelCommand and delegate to use case")
    void shouldMapMessageAndDelegate() {
        UUID sagaId = UUID.randomUUID();
        HotelCommandMessage message = new HotelCommandMessage(
                sagaId, "RESERVE", "Jan Kowalski", "Venus", new BigDecimal("100.00")
        );

        listener.onCommand(message);

        verify(processHotelCommandUseCase).handle(commandCaptor.capture());
        HotelCommand command = commandCaptor.getValue();
        assertThat(command.sagaId()).isEqualTo(sagaId);
        assertThat(command.action()).isEqualTo(SagaAction.RESERVE);
        assertThat(command.customerName()).isEqualTo("Jan Kowalski");
        assertThat(command.destination()).isEqualTo("Venus");
        assertThat(command.amount()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("should correctly map CANCEL action")
    void shouldMapCancelAction() {
        UUID sagaId = UUID.randomUUID();
        HotelCommandMessage message = new HotelCommandMessage(
                sagaId, "CANCEL", "Anna Nowak", "Mars", null
        );

        listener.onCommand(message);

        verify(processHotelCommandUseCase).handle(commandCaptor.capture());
        assertThat(commandCaptor.getValue().action()).isEqualTo(SagaAction.CANCEL);
    }
}
