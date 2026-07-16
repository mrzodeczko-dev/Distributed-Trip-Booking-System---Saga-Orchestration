package com.rzodeczko.common.messaging;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SagaIdMdcInterceptorTest {

    private final SagaIdMdcInterceptor interceptor = new SagaIdMdcInterceptor();

    @Test
    void shouldPutSagaIdInMdcDuringInvocationAndRemoveAfter() throws Throwable {
        MessageProperties props = new MessageProperties();
        props.setHeader("sagaId", "saga-123");
        Message message = new Message(new byte[0], props);
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[]{message});
        when(invocation.proceed()).thenAnswer(inv -> {
            assertThat(MDC.get("sagaId")).isEqualTo("saga-123");
            return "result";
        });

        Object result = interceptor.invoke(invocation);

        assertThat(result).isEqualTo("result");
        assertThat(MDC.get("sagaId")).isNull();
    }

    @Test
    void shouldRemoveMdcEvenWhenInvocationThrows() throws Throwable {
        MessageProperties props = new MessageProperties();
        props.setHeader("sagaId", "saga-fail");
        Message message = new Message(new byte[0], props);
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[]{message});
        when(invocation.proceed()).thenThrow(new RuntimeException("boom"));

        try {
            interceptor.invoke(invocation);
        } catch (RuntimeException expected) {
            // ok
        }

        assertThat(MDC.get("sagaId")).isNull();
    }

    @Test
    void shouldSkipMdcWhenNoSagaIdHeaderPresent() throws Throwable {
        Message message = new Message(new byte[0], new MessageProperties());
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[]{message});
        when(invocation.proceed()).thenReturn("ok");

        interceptor.invoke(invocation);

        assertThat(MDC.get("sagaId")).isNull();
    }

    @Test
    void shouldSkipMdcWhenNoMessageArgument() throws Throwable {
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getArguments()).thenReturn(new Object[]{"not-a-message", 42});
        when(invocation.proceed()).thenReturn("ok");

        Object result = interceptor.invoke(invocation);

        assertThat(result).isEqualTo("ok");
        assertThat(MDC.get("sagaId")).isNull();
    }
}
