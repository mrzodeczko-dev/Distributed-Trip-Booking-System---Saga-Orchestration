package com.rzodeczko.presentation.controller;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.PaymentDto;
import com.rzodeczko.application.port.in.GetPaymentUseCase;
import com.rzodeczko.presentation.dto.response.PagedResponseDto;
import com.rzodeczko.presentation.dto.response.PaymentResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<PagedResponseDto<PaymentResponseDto>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<PaymentDto> result = getPaymentUseCase.list(new PageQuery(page, size));
        return ResponseEntity.ok(new PagedResponseDto<>(
                result.content().stream().map(PaymentResponseDto::from).toList(),
                result.page(),
                result.size(),
                result.totalElements()
        ));
    }

    @GetMapping("/{sagaId}")
    public ResponseEntity<PaymentResponseDto> getBySaga(@PathVariable UUID sagaId) {
        return getPaymentUseCase.getBySagaId(sagaId)
                .map(PaymentResponseDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
