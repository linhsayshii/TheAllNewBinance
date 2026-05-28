package com.auction.core.utils;

import static org.assertj.core.api.Assertions.*;

import com.auction.core.users.Admin;
import com.auction.core.users.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JsonMapperTest {

    @Test
    @DisplayName("Should correctly serialize and parse LocalDateTime with custom format")
    void testLocalDateTimeSerialization() {
        LocalDateTime time = LocalDateTime.of(2026, 5, 28, 15, 30, 0);

        String json = JsonMapper.toJson(time);
        assertThat(json).isEqualTo("\"2026-05-28 15:30:00\"");

        LocalDateTime parsedTime = JsonMapper.fromJson(json, LocalDateTime.class);
        assertThat(parsedTime).isEqualTo(time);
    }

    @Test
    @DisplayName("Should handle null LocalDateTime gracefully")
    void testLocalDateTimeSerialization_Null() {
        String json = JsonMapper.toJson(null);
        assertThat(json).isNull();

        LocalDateTime parsedTime = JsonMapper.fromJson("null", LocalDateTime.class);
        assertThat(parsedTime).isNull();
    }

    @Test
    @DisplayName(
            "Should dynamically deserialize correct User subclass based on JSON payload (ADMIN)")
    void testPolymorphicUserDeserialization_Admin() {
        String json = "{\"role\":\"ADMIN\",\"username\":\"superadmin\",\"isActive\":true}";

        User parsedUser = JsonMapper.fromJson(json, User.class);

        assertThat(parsedUser).isInstanceOf(Admin.class);
        assertThat(parsedUser.getUsername()).isEqualTo("superadmin");
    }

    @Test
    @DisplayName("Should throw Exception when deserializing User with invalid Role")
    void testPolymorphicUser_InvalidRole_ShouldThrow() {
        String json = "{\"role\":\"HACKER\",\"username\":\"attacker\",\"isActive\":true}";

        assertThatThrownBy(() -> JsonMapper.fromJson(json, User.class))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should fallback to STANDARD role when discriminator is missing")
    void testPolymorphicUser_MissingRole_ShouldFallbackToStandard() {
        String json = "{\"username\":\"attacker\",\"isActive\":true}";

        User parsedUser = JsonMapper.fromJson(json, User.class);

        assertThat(parsedUser).isInstanceOf(com.auction.core.users.StandardUser.class);
        assertThat(parsedUser.getUsername()).isEqualTo("attacker");
    }
}
