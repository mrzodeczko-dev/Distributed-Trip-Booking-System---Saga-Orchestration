package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.PaymentDto;
import com.rzodeczko.application.port.in.GetPaymentUseCase;
import com.rzodeczko.application.port.out.PaymentRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PaymentQueryServiceImpl implements GetPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public PaymentQueryServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public List<PaymentDto> listAll() {
        return paymentRepository.findAll()
                .stream()
                .map(PaymentDto::from)
                .toList();
    }

    @Override
    public Optional<PaymentDto> getBySagaId(UUID sagaId) {
        return paymentRepository.findBySagaId(sagaId).map(PaymentDto::from);
    }
}
