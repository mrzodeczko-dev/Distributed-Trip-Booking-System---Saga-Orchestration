package com.rzodeczko.infrastructure.tx;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.PaymentDto;
import com.rzodeczko.application.port.in.GetPaymentUseCase;
import com.rzodeczko.application.service.PaymentQueryServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public class TransactionalPaymentQueryService implements GetPaymentUseCase {

    private final PaymentQueryServiceImpl delegate;

    public TransactionalPaymentQueryService(PaymentQueryServiceImpl delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PaymentDto> list(PageQuery query) {
        return delegate.list(query);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentDto> getBySagaId(UUID sagaId) {
        return delegate.getBySagaId(sagaId);
    }
}
