package com.rzodeczko.domain.model;

import java.util.UUID;

/**
 * Wynik próby rezerwacji miejsca w rakiecie — zamknięty typ domenowy (sealed interface + zagnieżdżone recordy).

 * Modeluje dwa możliwe rezultaty reguły biznesowej, bez wyjątków:
 * - Success — rezerwacja została utworzona i niesie obiekt SeatReservation
 * - Rejected — rezerwacja została odrzucona i niesie powód biznesowy (reason)

 * Metoda attemptReserve zawiera logikę domenową tej operacji. W tym serwisie jest to reguła demo:
 * jeśli customerName zaczyna się od BLOCKED, zwracany jest Rejected zamiast tworzenia rezerwacji.
 * W przeciwnym razie tworzony jest SeatReservation i zwracany jest Success.

 * To nie jest wyjątek ani błąd techniczny — odrzucenie to poprawny wynik biznesowy, który w Saga
 * przekłada się na FAILURE i uruchamia kompensację po stronie orkiestratora.

 * Warstwa application (FlightCommandService) wywołuje attemptReserve, a następnie mapuje wynik
 * na CommandResult dla protokołu Sagi. Domena nie zna RabbitMQ, outboxa ani formatu odpowiedzi AMQP.

 * Sealed interface ogranicza możliwe warianty do Success i Rejected, dzięki czemu switch
 * w use case musi obsłużyć wszystkie przypadki — kompilator wymusza kompletność obsługi.

 * Zgodność z czystą architekturą: typ i reguła biznesowa żyją w domain, use case tylko orkiestruje
 * (idempotencja, wywołanie domeny, zapis przez port, publikacja odpowiedzi).
 */
public sealed interface ReservationOutcome permits ReservationOutcome.Success, ReservationOutcome.Rejected {
    record Success(SeatReservation reservation) implements ReservationOutcome {
    }

    record Rejected(String reason) implements ReservationOutcome {
    }

    static ReservationOutcome attemptReserve(UUID sagaId, String customerName, String destination) {
        if (customerName != null && customerName.toUpperCase().startsWith("BLOCKED")) {
            return new Rejected("No rocket seats available - passenger on watchlist");
        }

        return new Success(SeatReservation.reserve(sagaId, customerName, destination));
    }
}
