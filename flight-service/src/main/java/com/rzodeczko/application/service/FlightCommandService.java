package com.rzodeczko.application.service;

import com.rzodeczko.application.command.FlightCommand;
import com.rzodeczko.application.event.CommandResult;
import com.rzodeczko.application.event.SagaParticipantReply;
import com.rzodeczko.application.port.in.ProcessFlightCommandUseCase;
import com.rzodeczko.common.idempotency.ProcessedMessageStore;
import com.rzodeczko.application.port.out.SagaReplyPort;
import com.rzodeczko.application.port.out.SeatReservationRepository;
import com.rzodeczko.domain.model.ReservationOutcome;

import java.util.logging.Logger;

/**
 * Jedna transakcja (zapewnia dekorator infrastruktury) obejmuje:
 * 1. Idempotencje
 * 2. Operacja bieznesowa
 * 3. Zapis odpowiedzi przez port SagaReplyPort
 */
public class FlightCommandService implements ProcessFlightCommandUseCase {

    private static final String STEP = "FLIGHT";
    private static final Logger log = Logger.getLogger(FlightCommandService.class.getName());

    private final ProcessedMessageStore processedMessageStore;
    private final SeatReservationRepository seatReservationRepository;
    private final SagaReplyPort sagaReplyPort;

    public FlightCommandService(ProcessedMessageStore processedMessageStore, SeatReservationRepository seatReservationRepository, SagaReplyPort sagaReplyPort) {
        this.processedMessageStore = processedMessageStore;
        this.seatReservationRepository = seatReservationRepository;
        this.sagaReplyPort = sagaReplyPort;
    }

    @Override
    public void handle(FlightCommand command) {
        String messageKey = command.sagaId() + ":" + command.action();

        if (processedMessageStore.existsByMessageKey(messageKey)) {
            log.info(() -> "[FLIGHT] Duplicate command " + messageKey + " - already processed, skipping");
            return;
        }

        CommandResult result = switch (command.action()) {
            case RESERVE -> reserve(command);
            case CANCEL -> cancel(command);
        };

        processedMessageStore.markProcessed(messageKey);
        sagaReplyPort.publish(SagaParticipantReply.from(command.sagaId(), STEP, command.action(), result));

        log.info(
                () -> "[FLIGHT] saga=" + command.sagaId() + ", action=" + command.action()
                        + ", result=" + result.statusString()
        );
    }

    private CommandResult reserve(FlightCommand command) {
        if (seatReservationRepository.existsBySagaId(command.sagaId())) {
            return CommandResult.success();
        }

        return switch (ReservationOutcome.attemptReserve(
                command.sagaId(), command.customerName(), command.destination()
        )) {
            case ReservationOutcome.Success success -> {
                seatReservationRepository.save(success.reservation());
                yield CommandResult.success();
            }
            case ReservationOutcome.Rejected rejected -> CommandResult.failure(rejected.reason());
        };
    }

    private CommandResult cancel(FlightCommand command) {
        seatReservationRepository
                .findBySagaId(command.sagaId())
                .ifPresent(reservation -> {
                    reservation.cancel();
                    seatReservationRepository.save(reservation);
                });
        return CommandResult.success();
    }
}
