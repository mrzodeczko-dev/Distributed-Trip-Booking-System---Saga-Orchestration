package com.rzodeczko.infrastructure.configuration;

import com.rzodeczko.common.idempotency.ProcessedMessageStore;
import com.rzodeczko.application.port.out.SagaReplyPort;
import com.rzodeczko.application.port.out.SeatReservationRepository;
import com.rzodeczko.application.service.FlightCommandService;
import com.rzodeczko.application.service.SeatReservationQueryServiceImpl;
import com.rzodeczko.infrastructure.tx.TransactionalFlightCommandService;
import com.rzodeczko.infrastructure.tx.TransactionalSeatReservationQueryService;
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
                .build()
        );
    }

    @Bean
    public FlightCommandService flightCommandService(
            ProcessedMessageStore processedMessageStore,
            SeatReservationRepository seatReservationRepository,
            SagaReplyPort sagaReplyPort
    ) {
        return new FlightCommandService(processedMessageStore, seatReservationRepository, sagaReplyPort);
    }

    @Bean
    public SeatReservationQueryServiceImpl seatReservationQueryServiceImpl(
            SeatReservationRepository seatReservationRepository) {
        return new SeatReservationQueryServiceImpl(seatReservationRepository);
    }

    @Bean("transactionalFlightCommandService")
    public TransactionalFlightCommandService transactionalFlightCommandService(
            FlightCommandService flightCommandService) {
        return new TransactionalFlightCommandService(flightCommandService);
    }

    @Bean("transactionalSeatReservationQueryService")
    public TransactionalSeatReservationQueryService transactionalSeatReservationQueryService(
            SeatReservationQueryServiceImpl seatReservationQueryServiceImpl) {
        return new TransactionalSeatReservationQueryService(seatReservationQueryServiceImpl);
    }

}
