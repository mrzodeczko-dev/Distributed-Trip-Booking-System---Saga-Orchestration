package com.rzodeczko.infrastructure.persistence.adapter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rzodeczko.TestcontainersConfiguration;
import com.rzodeczko.application.dto.PageQuery;
import com.rzodeczko.application.dto.PageResult;
import com.rzodeczko.domain.model.saga.SagaInstance;
import com.rzodeczko.domain.model.saga.SagaStatus;
import com.rzodeczko.infrastructure.persistence.mapper.SagaInstanceMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Import({TestcontainersConfiguration.class, SagaInstanceRepositoryAdapter.class, SagaInstanceMapper.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class SagaInstanceRepositoryAdapterTest {

    @Autowired
    private SagaInstanceRepositoryAdapter adapter;

    private ListAppender<ILoggingEvent> hibernateLogAppender;
    private Logger hibernateLogger;

    @BeforeEach
    void attachLogAppender() {
        hibernateLogger = (Logger) LoggerFactory.getLogger("org.hibernate");
        hibernateLogAppender = new ListAppender<>();
        hibernateLogAppender.start();
        hibernateLogger.addAppender(hibernateLogAppender);
    }

    @AfterEach
    void detachLogAppender() {
        hibernateLogger.detachAppender(hibernateLogAppender);
    }

    private void saveSagas(int count) {
        IntStream.range(0, count)
                .forEachOrdered(i -> adapter.save(SagaInstance.start("User-" + i, "City-" + i, BigDecimal.valueOf(100 + i))));
    }

    @Test
    @DisplayName("should paginate at SQL level without Hibernate in-memory pagination warning")
    void shouldPaginateAtSqlLevelWithoutInMemoryWarning() {
        saveSagas(5);
        hibernateLogAppender.list.clear();

        PageResult<SagaInstance> page = adapter.findAll(new PageQuery(0, 2));

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(2);

        // HHH90003004 is the warning Hibernate emits when it falls back to in-memory pagination
        boolean hasInMemoryPaginationWarning = hibernateLogAppender.list.stream()
                .filter(e -> e.getLevel() == Level.WARN)
                .anyMatch(e -> e.getFormattedMessage().contains("HHH90003004")
                        || e.getFormattedMessage().contains("firstResult/maxResults specified with collection fetch"));

        assertThat(hasInMemoryPaginationWarning)
                .as("Hibernate should NOT fall back to in-memory pagination")
                .isFalse();
    }

    @Test
    @DisplayName("steps should be eagerly loaded - no LazyInitializationException outside transaction")
    void stepsShouldBeEagerlyLoaded() {
        saveSagas(5);

        PageResult<SagaInstance> page = adapter.findAll(new PageQuery(0, 2));

        // findAll runs in its own @Transactional scope inside the adapter;
        // accessing steps here (outside that scope) would fail if they were lazy-loaded only.
        for (SagaInstance saga : page.content()) {
            assertThat(saga.getSteps()).hasSize(3);
            assertThat(saga.getSteps()).allSatisfy(step ->
                    assertThat(step.getName()).isNotNull()
            );
        }
    }

    @Test
    @DisplayName("second page should return remaining elements")
    void shouldReturnSecondPageCorrectly() {
        saveSagas(5);

        PageResult<SagaInstance> page = adapter.findAll(new PageQuery(1, 2));

        assertThat(page.content()).hasSize(2);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.totalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("last page should contain only remaining elements")
    void lastPageShouldContainRemainder() {
        saveSagas(5);

        PageResult<SagaInstance> page = adapter.findAll(new PageQuery(2, 2));

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("pages should not contain duplicate sagas despite steps join")
    void shouldNotContainDuplicatesFromStepsJoin() {
        saveSagas(4);

        PageResult<SagaInstance> page = adapter.findAll(new PageQuery(0, 4));

        List<java.util.UUID> ids = page.content().stream()
                .map(SagaInstance::getId)
                .toList();

        assertThat(ids).doesNotHaveDuplicates();
        assertThat(page.content()).hasSize(4);
    }

    @Test
    @DisplayName("empty database should return empty page")
    void emptyDatabaseShouldReturnEmptyPage() {
        PageResult<SagaInstance> page = adapter.findAll(new PageQuery(0, 10));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    @DisplayName("all sagas on every page should have status and complete step data")
    void everyPagedSagaShouldHaveCompleteData() {
        saveSagas(3);

        PageResult<SagaInstance> page = adapter.findAll(new PageQuery(0, 10));

        for (SagaInstance saga : page.content()) {
            assertThat(saga.getStatus()).isEqualTo(SagaStatus.IN_PROGRESS);
            assertThat(saga.getCustomerName()).startsWith("User-");
            assertThat(saga.getSteps()).hasSize(3);
        }
    }
}
