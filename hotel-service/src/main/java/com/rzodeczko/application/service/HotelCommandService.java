package com.rzodeczko.application.service;

import com.rzodeczko.application.command.HotelCommand;
import com.rzodeczko.application.event.CommandResult;
import com.rzodeczko.application.event.SagaParticipantReply;
import com.rzodeczko.application.port.in.ProcessHotelCommandUseCase;
import com.rzodeczko.application.port.out.CabinReservationRepository;
import com.rzodeczko.common.idempotency.ProcessedMessageStore;
import com.rzodeczko.application.port.out.SagaReplyPort;
import com.rzodeczko.domain.model.CabinReservation;
import com.rzodeczko.domain.model.ReservationOutcome;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HotelCommandService implements ProcessHotelCommandUseCase {

    private static final String STEP = "HOTEL";
    private static final Logger log = LoggerFactory.getLogger(HotelCommandService.class);

    private final ProcessedMessageStore processedMessageStore;
    private final CabinReservationRepository cabinReservationRepository;
    private final SagaReplyPort sagaReplyPort;

    public HotelCommandService(
            ProcessedMessageStore processedMessageStore,
            CabinReservationRepository cabinReservationRepository,
            SagaReplyPort sagaReplyPort) {
        this.processedMessageStore = processedMessageStore;
        this.cabinReservationRepository = cabinReservationRepository;
        this.sagaReplyPort = sagaReplyPort;
    }

    @Override
    public void handle(HotelCommand command) {
        String messageKey = command.sagaId() + ":" + command.action();

        if (processedMessageStore.existsByMessageKey(messageKey)) {
            log.info("[HOTEL] Duplicate command {} - already processed, skipping", messageKey);
            return;
        }

        CommandResult result = switch (command.action()) {
            case RESERVE -> reserve(command);
            case CANCEL -> cancel(command);
        };

        processedMessageStore.markProcessed(messageKey);
        sagaReplyPort.publish(SagaParticipantReply.from(command.sagaId(), STEP, command.action(), result));

        log.info("[HOTEL] action={}, result={}", command.action(), result.statusString());
    }

    private CommandResult reserve(HotelCommand command) {
        if (cabinReservationRepository.existsBySagaId(command.sagaId())) {
            return CommandResult.success();
        }

        return switch (ReservationOutcome.attemptReserve(
                command.sagaId(),
                command.customerName(),
                command.destination()
        )) {
            case ReservationOutcome.Success(CabinReservation reservation) -> {
                cabinReservationRepository.save(reservation);
                yield CommandResult.success();
            }
            case ReservationOutcome.Rejected rejected -> CommandResult.failure(rejected.reason());
        };
    }

    private CommandResult cancel(HotelCommand command) {
        cabinReservationRepository.findBySagaId(command.sagaId()).ifPresent(reservation -> {
            reservation.cancel();
            cabinReservationRepository.save(reservation);
        });
        return CommandResult.success();
    }
}
