package com.auction.core.utils;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DateFormatterTest {

    @Test
    @DisplayName("Should format LocalDateTime to string correctly")
    void testFormat() {
        LocalDateTime time = LocalDateTime.of(2026, 5, 28, 15, 30, 45);
        String formatted = DateFormatter.format(time);

        assertThat(formatted).isEqualTo("2026-05-28 15:30:45");
    }

    @Test
    @DisplayName("Should handle null LocalDateTime gracefully")
    void testFormat_Null() {
        String formatted = DateFormatter.format(null);
        assertThat(formatted).isNull();
    }

    @Test
    @DisplayName("Should parse string to LocalDateTime correctly")
    void testParse() {
        String dateStr = "2026-05-28 15:30:45";
        LocalDateTime parsed = DateFormatter.parse(dateStr);

        assertThat(parsed).isEqualTo(LocalDateTime.of(2026, 5, 28, 15, 30, 45));
    }
}
