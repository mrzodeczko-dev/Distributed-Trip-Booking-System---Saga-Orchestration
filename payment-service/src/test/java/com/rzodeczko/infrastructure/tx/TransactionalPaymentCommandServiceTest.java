package com.rzodeczko.infrastructure.tx;

import com.rzodeczko.application.command.PaymentCommand;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.service.PaymentCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionalPaymentCommandServiceTest {

    @Mock
    private PaymentCommandService delegate;

    @InjectMocks
    private TransactionalPaymentCommandService sut;

    @Test
    @DisplayName("handle() delegates to PaymentCommandService")
    void shouldDelegateHandle() {
        var cmd = new PaymentCommand(UUID.randomUUID(), SagaAction.RESERVE, "Jan", "Zakopane", BigDecimal.TEN);

        sut.handle(cmd);

        verify(delegate).handle(cmd);
    }
}
