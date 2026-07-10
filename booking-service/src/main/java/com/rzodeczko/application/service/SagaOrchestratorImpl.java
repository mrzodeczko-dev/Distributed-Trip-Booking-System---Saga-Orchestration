package com.rzodeczko.application.service;

import com.rzodeczko.application.command.StartTripBookingCommand;
import com.rzodeczko.application.dto.SagaInstanceDto;
import com.rzodeczko.application.event.SagaAction;
import com.rzodeczko.application.event.SagaReply;
import com.rzodeczko.application.port.in.HandleSagaReplyUseCase;
import com.rzodeczko.application.port.in.StartTripBookingUseCase;
import com.rzodeczko.application.port.out.SagaCommandPort;
import com.rzodeczko.application.port.out.SagaInstanceRepository;
import com.rzodeczko.domain.exception.SagaNotFoundException;
import com.rzodeczko.domain.model.saga.SagaInstance;
import com.rzodeczko.domain.model.saga.SagaStepName;
import com.rzodeczko.domain.model.saga.SagaStepStatus;

import java.util.Optional;
import java.util.logging.Logger;

public class SagaOrchestratorImpl implements StartTripBookingUseCase, HandleSagaReplyUseCase {

    private static final Logger log = Logger.getLogger(SagaOrchestratorImpl.class.getName());

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaCommandPort sagaCommandPort;

    public SagaOrchestratorImpl(SagaInstanceRepository sagaInstanceRepository, SagaCommandPort sagaCommandPort) {
        this.sagaInstanceRepository = sagaInstanceRepository;
        this.sagaCommandPort = sagaCommandPort;
    }

    @Override
    public SagaInstanceDto start(StartTripBookingCommand command) {
        SagaInstance saga = SagaInstance.start(
                command.customerName(),
                command.destination(),
                command.amount()
        );
        SagaStepName firstStep = saga.nextStepToReserve().orElseThrow();
        sagaInstanceRepository.save(saga);
        sagaCommandPort.sendReserve(saga, firstStep);
        log.info("Saga started. sagaId=%s, firstStep=%s".formatted(saga.getId(), firstStep));
        return SagaInstanceDto.from(saga);
    }

    @Override
    public void handle(SagaReply reply) {
        SagaInstance saga = sagaInstanceRepository
                .findByIdForUpdate(reply.sagaId())
                .orElseThrow(() -> new SagaNotFoundException(reply.sagaId()));

        if (saga.isForwardPhase()) {
            handleForwardReply(saga, reply);
        } else if (saga.isCompensating()) {
            handleCompensationReply(saga, reply);
        } else {
            log.info("Saga %s terminal %s - ignoring late reply".formatted(saga.getId(), saga.getStatus()));
        }
    }

    private void handleForwardReply(SagaInstance saga, SagaReply reply) {
        if (reply.action() != SagaAction.RESERVE) {
            return;
        }

        switch (reply.status()) {
            case SUCCESS -> {
                if (saga.getStep(reply.step()).isReserved()) {
                    return;
                }
                saga.markReserved(reply.step());
                Optional<SagaStepName> next = saga.nextStepToReserve();
                if (next.isPresent()) {
                    sagaInstanceRepository.save(saga);
                    sagaCommandPort.sendReserve(saga, next.get());
                } else {
                    saga.complete();
                    sagaInstanceRepository.save(saga);
                }
            }
            case FAILURE -> {
                saga.failAndStartCompensation(reply.step(), reply.reason());
                Optional<SagaStepName> toCompensate = saga.nextStepToCompensate();
                if (toCompensate.isPresent()) {
                    sagaInstanceRepository.save(saga);
                    sagaCommandPort.sendCancel(saga, toCompensate.get());
                } else {
                    saga.cancel();
                    sagaInstanceRepository.save(saga);
                }
            }
        }
    }

    private void handleCompensationReply(SagaInstance saga, SagaReply reply) {
        if (reply.action() != SagaAction.CANCEL) {
            return;
        }

        switch (reply.status()) {
            case SUCCESS -> {
                if (saga.getStep(reply.step()).getStatus() == SagaStepStatus.COMPENSATED) {
                    return;
                }
                saga.markCompensated(reply.step());
                Optional<SagaStepName> next = saga.nextStepToCompensate();
                if (next.isPresent()) {
                    sagaInstanceRepository.save(saga);
                    sagaCommandPort.sendCancel(saga, next.get());
                } else {
                    saga.cancel();
                    sagaInstanceRepository.save(saga);
                }
            }
            case FAILURE -> {
                saga.markCompensationFailed(reply.step(), reply.reason());
                sagaInstanceRepository.save(saga);
            }
        }
    }
}
