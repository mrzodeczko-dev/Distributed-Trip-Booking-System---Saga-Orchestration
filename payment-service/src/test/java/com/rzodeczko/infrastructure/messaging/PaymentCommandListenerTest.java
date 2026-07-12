package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.application.command.PaymentCommand;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.port.in.ProcessPaymentCommandUseCase;
import com.rzodeczko.infrastructure.messaging.dto.PaymentCommandMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCommandListener")
class PaymentCommandListenerTest {

    @Mock
    private ProcessPaymentCommandUseCase processPaymentCommandUseCase;

    @Test
    @DisplayName("maps a RESERVE PaymentCommandMessage to a PaymentCommand and delegates to the use case")
    void mapsAndDelegatesReserveCommand() {
        PaymentCommandListener listener = new PaymentCommandListener(processPaymentCommandUseCase);
        UUID sagaId = UUID.randomUUID();
        PaymentCommandMessage message = new PaymentCommandMessage(
                sagaId, "RESERVE", "Alice", "dest-account", BigDecimal.valueOf(250));

        listener.onCommand(message);

        ArgumentCaptor<PaymentCommand> captor = ArgumentCaptor.forClass(PaymentCommand.class);
        verify(processPaymentCommandUseCase).handle(captor.capture());
        PaymentCommand command = captor.getValue();

        assertThat(command.sagaId()).isEqualTo(sagaId);
        assertThat(command.action()).isEqualTo(SagaAction.RESERVE);
        assertThat(command.customerName()).isEqualTo("Alice");
        assertThat(command.destination()).isEqualTo("dest-account");
        assertThat(command.amount()).isEqualByComparingTo(BigDecimal.valueOf(250));
    }

    @Test
    @DisplayName("maps a CANCEL PaymentCommandMessage to a PaymentCommand and delegates to the use case")
    void mapsAndDelegatesCancelCommand() {
        PaymentCommandListener listener = new PaymentCommandListener(processPaymentCommandUseCase);
        UUID sagaId = UUID.randomUUID();
        PaymentCommandMessage message = new PaymentCommandMessage(
                sagaId, "CANCEL", "Bob", "dest-account", BigDecimal.valueOf(75));

        listener.onCommand(message);

        ArgumentCaptor<PaymentCommand> captor = ArgumentCaptor.forClass(PaymentCommand.class);
        verify(processPaymentCommandUseCase).handle(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo(SagaAction.CANCEL);
    }
}
