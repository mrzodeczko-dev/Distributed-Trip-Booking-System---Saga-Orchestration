package com.rzodeczko.common.messaging;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;

public class SagaIdMdcInterceptor implements MethodInterceptor {

    private static final String MDC_KEY = "sagaId";

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String sagaId = extractSagaId(invocation.getArguments());
        if (sagaId != null) {
            MDC.put(MDC_KEY, sagaId);
        }
        try {
            return invocation.proceed();
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String extractSagaId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Message message) {
                Object header = message.getMessageProperties().getHeader(MDC_KEY);
                return header != null ? header.toString() : null;
            }
        }
        return null;
    }
}
