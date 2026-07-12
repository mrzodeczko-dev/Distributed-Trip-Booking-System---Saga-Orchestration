package com.rzodeczko.application.port.in;

import com.rzodeczko.application.dto.PaymentDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GetPaymentUseCase {

    List<PaymentDto> listAll();

    Optional<PaymentDto> getBySagaId(UUID sagaId);
}
