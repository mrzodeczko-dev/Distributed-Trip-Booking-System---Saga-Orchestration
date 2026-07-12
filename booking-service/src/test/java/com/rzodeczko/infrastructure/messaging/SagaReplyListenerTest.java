package com.rzodeczko.infrastructure.messaging;

import com.rzodeczko.application.event.ReplyStatus;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.event.SagaReply;
import com.rzodeczko.application.port.in.HandleSagaReplyUseCase;
import com.rzodeczko.domain.model.saga.SagaStepName;
import com.rzodeczko.infrastructure.messaging.dto.SagaReplyMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SagaReplyListenerTest {

    @Mock
    private HandleSagaReplyUseCase handleSagaReplyUseCase;

    @Captor
    private ArgumentCaptor<SagaReply> replyCaptor;

    @InjectMocks
    private SagaReplyListener listener;

    @Nested
    @DisplayName("onReply()")
    class OnReply {

        @Test
        void shouldMapSuccessReplyMessageAndDelegateToUseCase() {
            UUID sagaId = UUID.randomUUID();
            SagaReplyMessage message = new SagaReplyMessage(
                    sagaId, "FLIGHT", "RESERVE", "SUCCESS", null
            );

            listener.onReply(message);

            verify(handleSagaReplyUseCase).handle(replyCaptor.capture());
            SagaReply reply = replyCaptor.getValue();
            assertThat(reply.sagaId()).isEqualTo(sagaId);
            assertThat(reply.step()).isEqualTo(SagaStepName.FLIGHT);
            assertThat(reply.action()).isEqualTo(SagaAction.RESERVE);
            assertThat(reply.status()).isEqualTo(ReplyStatus.SUCCESS);
            assertThat(reply.reason()).isNull();
        }

        @Test
        void shouldMapFailureReplyMessageWithReason() {
            UUID sagaId = UUID.randomUUID();
            SagaReplyMessage message = new SagaReplyMessage(
                    sagaId, "HOTEL", "CANCEL", "FAILURE", "No cabins available"
            );

            listener.onReply(message);

            verify(handleSagaReplyUseCase).handle(replyCaptor.capture());
            SagaReply reply = replyCaptor.getValue();
            assertThat(reply.sagaId()).isEqualTo(sagaId);
            assertThat(reply.step()).isEqualTo(SagaStepName.HOTEL);
            assertThat(reply.action()).isEqualTo(SagaAction.CANCEL);
            assertThat(reply.status()).isEqualTo(ReplyStatus.FAILURE);
            assertThat(reply.reason()).isEqualTo("No cabins available");
        }

        @Test
        void shouldMapPaymentStep() {
            UUID sagaId = UUID.randomUUID();
            SagaReplyMessage message = new SagaReplyMessage(
                    sagaId, "PAYMENT", "RESERVE", "SUCCESS", null
            );

            listener.onReply(message);

            verify(handleSagaReplyUseCase).handle(replyCaptor.capture());
            assertThat(replyCaptor.getValue().step()).isEqualTo(SagaStepName.PAYMENT);
        }
    }
}
