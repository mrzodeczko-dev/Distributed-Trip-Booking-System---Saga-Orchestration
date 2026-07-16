package com.rzodeczko.infrastructure.configuration;

import com.rzodeczko.application.port.out.CabinReservationRepository;
import com.rzodeczko.common.idempotency.ProcessedMessageStore;
import com.rzodeczko.application.port.out.SagaReplyPort;
import com.rzodeczko.application.service.CabinReservationQueryServiceImpl;
import com.rzodeczko.application.service.HotelCommandService;
import com.rzodeczko.infrastructure.tx.TransactionalCabinReservationQueryService;
import com.rzodeczko.infrastructure.tx.TransactionalHotelCommandService;
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
    public HotelCommandService hotelCommandService(
            ProcessedMessageStore processedMessageStore,
            CabinReservationRepository cabinReservationRepository,
            SagaReplyPort sagaReplyPort
    ) {
        return new HotelCommandService(processedMessageStore, cabinReservationRepository, sagaReplyPort);
    }

    @Bean
    public CabinReservationQueryServiceImpl cabinReservationQueryServiceImpl(CabinReservationRepository repository) {
        return new CabinReservationQueryServiceImpl(repository);
    }

    @Bean("transactionalHotelCommandService")
    public TransactionalHotelCommandService transactionalHotelCommandService(HotelCommandService hotelCommandService) {
        return new TransactionalHotelCommandService(hotelCommandService);
    }

    @Bean("transactionalCabinReservationQueryService")
    public TransactionalCabinReservationQueryService transactionalCabinReservationQueryService(
            CabinReservationQueryServiceImpl cabinReservationQueryServiceImpl) {
        return new TransactionalCabinReservationQueryService(cabinReservationQueryServiceImpl);
    }
}
