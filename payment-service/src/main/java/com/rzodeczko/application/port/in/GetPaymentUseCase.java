package com.rzodeczko.application.port.in;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.PaymentDto;

import java.util.Optional;
import java.util.UUID;

public interface GetPaymentUseCase {
    PageResult<PaymentDto> list(PageQuery query);
    Optional<PaymentDto> getBySagaId(UUID sagaId);
}
