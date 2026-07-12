package com.rzodeczko.infrastructure.configuration;

import com.rzodeczko.application.port.out.PaymentRepository;
import com.rzodeczko.application.port.out.ProcessedMessageStore;
import com.rzodeczko.application.port.out.SagaReplyPort;
import com.rzodeczko.application.service.PaymentCommandService;
import com.rzodeczko.application.service.PaymentQueryServiceImpl;
import com.rzodeczko.infrastructure.tx.TransactionalPaymentCommandService;
import com.rzodeczko.infrastructure.tx.TransactionalPaymentQueryService;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "30s")
public class BeanConfiguration {

    @Bean
    public LockProvider lockProvider(JdbcTemplate jdbcTemplate) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration
                .builder()
                .withJdbcTemplate(jdbcTemplate)
                .usingDbTime()
                .build());
    }

    @Bean
    public PaymentCommandService paymentCommandService(
            ProcessedMessageStore processedMessageStore,
            PaymentRepository paymentRepository,
            SagaReplyPort sagaReplyPort
    ) {
        return new PaymentCommandService(processedMessageStore, paymentRepository, sagaReplyPort);
    }

    @Bean
    public PaymentQueryServiceImpl paymentQueryServiceImpl(PaymentRepository paymentRepository) {
        return new PaymentQueryServiceImpl(paymentRepository);
    }

    @Bean("transactionalPaymentCommandService")
    public TransactionalPaymentCommandService transactionalPaymentCommandService(
            PaymentCommandService paymentCommandService
    ) {
        return new TransactionalPaymentCommandService(paymentCommandService);
    }

    @Bean("transactionalPaymentQueryService")
    public TransactionalPaymentQueryService transactionalPaymentQueryService(
            PaymentQueryServiceImpl paymentQueryServiceImpl
    ) {
        return new TransactionalPaymentQueryService(paymentQueryServiceImpl);
    }
}

