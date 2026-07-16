package com.rzodeczko.application.service;

import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.application.dto.SagaInstanceDto;
import com.rzodeczko.application.port.out.SagaInstanceRepository;
import com.rzodeczko.domain.exception.SagaNotFoundException;
import com.rzodeczko.domain.model.saga.SagaInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SagaQueryServiceImplTest {

    @Mock
    private SagaInstanceRepository sagaInstanceRepository;

    private SagaQueryServiceImpl queryService;

    private static final String CUSTOMER = "Jan Kowalski";
    private static final String DESTINATION = "Mars";
    private static final BigDecimal AMOUNT = new BigDecimal("2500.00");

    @BeforeEach
    void setUp() {
        queryService = new SagaQueryServiceImpl(sagaInstanceRepository);
    }

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        void shouldReturnDtoWhenSagaFound() {
            SagaInstance saga = SagaInstance.start(CUSTOMER, DESTINATION, AMOUNT);
            when(sagaInstanceRepository.findById(saga.getId())).thenReturn(Optional.of(saga));

            SagaInstanceDto result = queryService.getById(saga.getId());

            assertThat(result.sagaId()).isEqualTo(saga.getId().toString());
            assertThat(result.customerName()).isEqualTo(CUSTOMER);
            assertThat(result.destination()).isEqualTo(DESTINATION);
            assertThat(result.amount()).isEqualByComparingTo(AMOUNT);
            assertThat(result.status()).isEqualTo("IN_PROGRESS");
            assertThat(result.steps()).hasSize(3);
        }

        @Test
        void shouldThrowWhenSagaNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(sagaInstanceRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatExceptionOfType(SagaNotFoundException.class)
                    .isThrownBy(() -> queryService.getById(unknownId))
                    .withMessageContaining(unknownId.toString());
        }
    }

    @Nested
    @DisplayName("list()")
    class ListPaged {

        @Test
        void shouldReturnMappedDtosWhenSagasExist() {
            SagaInstance saga1 = SagaInstance.start(CUSTOMER, DESTINATION, AMOUNT);
            SagaInstance saga2 = SagaInstance.start("Anna Nowak", "Venus", new BigDecimal("100.00"));
            PageQuery query = new PageQuery(0, 20);
            when(sagaInstanceRepository.findAll(any(PageQuery.class)))
                    .thenReturn(new PageResult<>(List.of(saga1, saga2), 0, 20, 2));

            PageResult<SagaInstanceDto> result = queryService.list(query);

            assertThat(result.content()).hasSize(2);
            assertThat(result.content())
                    .extracting(SagaInstanceDto::sagaId)
                    .containsExactlyInAnyOrder(saga1.getId().toString(), saga2.getId().toString());
            assertThat(result.totalElements()).isEqualTo(2);
            verify(sagaInstanceRepository).findAll(any(PageQuery.class));
        }

        @Test
        void shouldReturnEmptyPageWhenNoSagasExist() {
            PageQuery query = new PageQuery(0, 20);
            when(sagaInstanceRepository.findAll(any(PageQuery.class)))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0));

            PageResult<SagaInstanceDto> result = queryService.list(query);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }
}
