package com.rzodeczko.application.event;

public record CommandResult(boolean succeeded, String reason) {
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
