package com.rzodeczko.application.service;

import com.rzodeczko.application.command.PaymentCommand;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.event.SagaParticipantReply;
import com.rzodeczko.application.port.out.PaymentRepository;
import com.rzodeczko.application.port.out.ProcessedMessageStore;
import com.rzodeczko.application.port.out.SagaReplyPort;
import com.rzodeczko.domain.model.Payment;
import com.rzodeczko.domain.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCommandService")
class PaymentCommandServiceTest {

    @Mock
    private ProcessedMessageStore processedMessageStore;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SagaReplyPort sagaReplyPort;

    private PaymentCommandService service;

    private UUID sagaId;

    @BeforeEach
    void setUp() {
        service = new PaymentCommandService(processedMessageStore, paymentRepository, sagaReplyPort);
        sagaId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("handle - duplicate detection")
    class DuplicateHandling {

        @Test
        @DisplayName("skips processing when the message key was already processed")
        void skipsDuplicateMessage() {
            PaymentCommand command = new PaymentCommand(sagaId, SagaAction.RESERVE, "Alice", "dest", BigDecimal.TEN);
            when(processedMessageStore.existsByMessageKey(sagaId + ":RESERVE")).thenReturn(true);

            service.handle(command);

            verify(processedMessageStore, never()).markProcessed(anyString());
            verify(sagaReplyPort, never()).publish(any());
            verifyNoInteractions(paymentRepository);
        }
    }

    @Nested
    @DisplayName("handle - RESERVE (charge)")
    class Reserve {

        @Test
        @DisplayName("charges successfully when amount is below credit limit and saga not yet charged")
        void chargesSuccessfully() {
            PaymentCommand command = new PaymentCommand(sagaId, SagaAction.RESERVE, "Alice", "dest", BigDecimal.valueOf(500));
            when(processedMessageStore.existsByMessageKey(anyString())).thenReturn(false);
            when(paymentRepository.existsBySagaId(sagaId)).thenReturn(false);

            service.handle(command);

            verify(paymentRepository).save(any(Payment.class));
            verify(processedMessageStore).markProcessed(sagaId + ":RESERVE");

            ArgumentCaptor<SagaParticipantReply> replyCaptor = ArgumentCaptor.forClass(SagaParticipantReply.class);
            verify(sagaReplyPort).publish(replyCaptor.capture());
            SagaParticipantReply reply = replyCaptor.getValue();
            assertThat(reply.sagaId()).isEqualTo(sagaId);
            assertThat(reply.step()).isEqualTo("PAYMENT");
            assertThat(reply.action()).isEqualTo(SagaAction.RESERVE);
            assertThat(reply.status()).isEqualTo("SUCCESS");
            assertThat(reply.reason()).isNull();
        }

        @Test
        @DisplayName("rejects charge when amount exceeds the credit limit and publishes failure reply")
        void rejectsChargeAboveLimit() {
            PaymentCommand command = new PaymentCommand(sagaId, SagaAction.RESERVE, "Alice", "dest", BigDecimal.valueOf(1_000_000));
            when(processedMessageStore.existsByMessageKey(anyString())).thenReturn(false);
            when(paymentRepository.existsBySagaId(sagaId)).thenReturn(false);

            service.handle(command);

            verify(paymentRepository, never()).save(any());
            verify(processedMessageStore).markProcessed(sagaId + ":RESERVE");

            ArgumentCaptor<SagaParticipantReply> replyCaptor = ArgumentCaptor.forClass(SagaParticipantReply.class);
            verify(sagaReplyPort).publish(replyCaptor.capture());
            SagaParticipantReply reply = replyCaptor.getValue();
            assertThat(reply.status()).isEqualTo("FAILURE");
            assertThat(reply.reason()).isEqualTo("Payment declined - amount exceeds credit limit");
        }

        @Test
        @DisplayName("is idempotent when a payment already exists for the saga")
        void isIdempotentWhenAlreadyCharged() {
            PaymentCommand command = new PaymentCommand(sagaId, SagaAction.RESERVE, "Alice", "dest", BigDecimal.valueOf(500));
            when(processedMessageStore.existsByMessageKey(anyString())).thenReturn(false);
            when(paymentRepository.existsBySagaId(sagaId)).thenReturn(true);

            service.handle(command);

            verify(paymentRepository, never()).save(any());
            ArgumentCaptor<SagaParticipantReply> replyCaptor = ArgumentCaptor.forClass(SagaParticipantReply.class);
            verify(sagaReplyPort).publish(replyCaptor.capture());
            assertThat(replyCaptor.getValue().status()).isEqualTo("SUCCESS");
        }
    }

    @Nested
    @DisplayName("handle - CANCEL (refund)")
    class Cancel {

        @Test
        @DisplayName("refunds the payment when it exists for the saga")
        void refundsExistingPayment() {
            PaymentCommand command = new PaymentCommand(sagaId, SagaAction.CANCEL, "Alice", "dest", BigDecimal.valueOf(500));
            Payment payment = Payment.charge(sagaId, "Alice", BigDecimal.valueOf(500));
            when(processedMessageStore.existsByMessageKey(anyString())).thenReturn(false);
            when(paymentRepository.findBySagaId(sagaId)).thenReturn(Optional.of(payment));

            service.handle(command);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            verify(paymentRepository).save(payment);
            verify(processedMessageStore).markProcessed(sagaId + ":CANCEL");

            ArgumentCaptor<SagaParticipantReply> replyCaptor = ArgumentCaptor.forClass(SagaParticipantReply.class);
            verify(sagaReplyPort).publish(replyCaptor.capture());
            assertThat(replyCaptor.getValue().status()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("is idempotent (still reports success) when no payment exists for the saga")
        void isIdempotentWhenNoPaymentExists() {
            PaymentCommand command = new PaymentCommand(sagaId, SagaAction.CANCEL, "Alice", "dest", BigDecimal.valueOf(500));
            when(processedMessageStore.existsByMessageKey(anyString())).thenReturn(false);
            when(paymentRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());

            service.handle(command);

            verify(paymentRepository, never()).save(any());
            ArgumentCaptor<SagaParticipantReply> replyCaptor = ArgumentCaptor.forClass(SagaParticipantReply.class);
            verify(sagaReplyPort).publish(replyCaptor.capture());
            assertThat(replyCaptor.getValue().status()).isEqualTo("SUCCESS");
        }
    }
}
