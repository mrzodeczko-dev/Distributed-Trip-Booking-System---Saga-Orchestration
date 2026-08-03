package com.rzodeczko.application.port.out;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    void save(Payment payment);

    boolean existsBySagaId(UUID sagaId);

    Optional<Payment> findBySagaId(UUID sagaId);

    PageResult<Payment> findAll(PageQuery query);
}
