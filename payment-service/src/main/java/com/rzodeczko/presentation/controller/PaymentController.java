package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.port.in.GetPaymentUseCase;
import com.rzodeczko.presentation.dto.response.PaymentResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final GetPaymentUseCase getPaymentUseCase;

    public PaymentController(
            @Qualifier("transactionalPaymentQueryService")
            GetPaymentUseCase getPaymentUseCase
    ) {
        this.getPaymentUseCase = getPaymentUseCase;
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponseDto>> listAll() {
        return ResponseEntity.ok(getPaymentUseCase.listAll().stream()
                .map(PaymentResponseDto::from)
                .toList());
    }

    @GetMapping("/{sagaId}")
    public ResponseEntity<PaymentResponseDto> getBySaga(@PathVariable UUID sagaId) {
        return getPaymentUseCase.getBySagaId(sagaId)
                .map(PaymentResponseDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

