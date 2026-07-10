package com.rzodeczko.application.command;

import java.math.BigDecimal;

public record StartTripBookingCommand(String customerName, String destination, BigDecimal amount) {
}
