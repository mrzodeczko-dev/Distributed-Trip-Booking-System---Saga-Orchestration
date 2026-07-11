package com.rzodeczko.application.event;

/**
 * Wynik wykonania komendy biznesowej (nie wyjatek - to poprawny rezultat Sagi).
 * Porazka biznesowa (np. brak miejsc) konczy sie odpowiedzia FAILURE,
 * ktora uruchamia kompensacje po stronie orkiestratora.
 */
public record CommandResult(
        boolean succeeded,
        String reason
) {
    public static CommandResult success() {
        return new CommandResult(true, null);
    }

    public static CommandResult failure(String reason) {
        return new CommandResult(false, reason);
    }

    public String statusString() {
        return succeeded ? "SUCCESS" : "FAILURE";
    }
}
