package com.rzodeczko.application.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandResultTest {

    @Test
    @DisplayName("success() creates result with succeeded=true and no reason")
    void success() {
        var result = CommandResult.success();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.reason()).isNull();
        assertThat(result.statusString()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("failure() creates result with succeeded=false and reason")
    void failure() {
        var result = CommandResult.failure("no seats");

        assertThat(result.succeeded()).isFalse();
        assertThat(result.reason()).isEqualTo("no seats");
        assertThat(result.statusString()).isEqualTo("FAILURE");
    }
}
