package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.PaymentDto;
import com.rzodeczko.application.port.in.GetPaymentUseCase;
import com.rzodeczko.application.port.out.PaymentRepository;
import com.rzodeczko.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public class PaymentQueryServiceImpl implements GetPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public PaymentQueryServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PageResult<PaymentDto> list(PageQuery query) {
        PageResult<Payment> page = paymentRepository.findAll(query);
        return new PageResult<>(
                page.content().stream().map(PaymentDto::from).toList(),
                page.page(),
                page.size(),
                page.totalElements()
        );
    }

    @Override
    public Optional<PaymentDto> getBySagaId(UUID sagaId) {
        return paymentRepository.findBySagaId(sagaId).map(PaymentDto::from);
    }
}
