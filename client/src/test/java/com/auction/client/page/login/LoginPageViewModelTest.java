package com.auction.client.page.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auction.core.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests cho LoginPageViewModel.
 *
 * <p>Kiểm thử toàn bộ logic thuần túy (không cần JavaFX Runtime): - Xác thực thông tin đăng nhập
 * (validateCredentials) - Phân tích cú pháp phản hồi thành công từ Server (parseLoginResponse) -
 * Phân tích thông điệp lỗi từ phản hồi thất bại (parseErrorMessage)
 */
@DisplayName("LoginPageViewModel Unit Tests")
class LoginPageViewModelTest {

    private LoginPageViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new LoginPageViewModel();
    }

    // =========================================================================
    // Test Case 1.1.1: validateCredentials
    // =========================================================================

    @Test
    @DisplayName("1.1.1a - Username rỗng phải trả về false")
    void validateCredentials_emptyUsername_returnsFalse() {
        assertFalse(viewModel.validateCredentials("", "1234"));
    }

    @Test
    @DisplayName("1.1.1b - Username null phải trả về false")
    void validateCredentials_nullUsername_returnsFalse() {
        assertFalse(viewModel.validateCredentials(null, "1234"));
    }

    @Test
    @DisplayName("1.1.1c - Mật khẩu quá ngắn (< 4 ký tự) phải trả về false")
    void validateCredentials_passwordTooShort_returnsFalse() {
        assertFalse(viewModel.validateCredentials("validUser", "123"));
    }

    @Test
    @DisplayName("1.1.1d - Mật khẩu null phải trả về false")
    void validateCredentials_nullPassword_returnsFalse() {
        assertFalse(viewModel.validateCredentials("validUser", null));
    }

    @Test
    @DisplayName("1.1.1e - Thông tin hợp lệ (username không rỗng, password >= 4) phải trả về true")
    void validateCredentials_validCredentials_returnsTrue() {
        assertTrue(viewModel.validateCredentials("validUser", "1234"));
    }

    @Test
    @DisplayName("1.1.1f - Mật khẩu đúng 4 ký tự là ngưỡng hợp lệ tối thiểu")
    void validateCredentials_passwordExactlyFourChars_returnsTrue() {
        assertTrue(viewModel.validateCredentials("u", "1234"));
    }

    // =========================================================================
    // Test Case 1.1.2: parseLoginResponse - Phản hồi thành công
    // =========================================================================

    @Test
    @DisplayName("1.1.2a - JSON thành công phải parse ra đối tượng User không null")
    void parseLoginResponse_successJson_returnsUser() {
        // Role hợp lệ trong UserFactory: "STANDARD" hoặc "ADMIN" (không phải "USER")
        String successJson =
                "{\"success\":true,\"data\":{\"id\":1,\"username\":\"testUser\","
                        + "\"balance\":1000.0,\"role\":\"STANDARD\"}}";

        User result = viewModel.parseLoginResponse(successJson);

        assertNotNull(result, "User must not be null on successful response");
        assertEquals("testUser", result.getUsername());
    }

    @Test
    @DisplayName("1.1.2b - JSON có success=false phải trả về null (không ném exception)")
    void parseLoginResponse_failureJson_returnsNull() {
        String failureJson = "{\"success\":false,\"message\":\"Invalid credentials\"}";

        User result = viewModel.parseLoginResponse(failureJson);

        assertNull(result, "User must be null on failure response");
    }

    @Test
    @DisplayName("1.1.2c - JSON rỗng phải trả về null an toàn (không crash)")
    void parseLoginResponse_emptyJson_returnsNullSafely() {
        User result = viewModel.parseLoginResponse("");
        assertNull(result, "Must return null safely on empty JSON");
    }

    @Test
    @DisplayName("1.1.2d - JSON null phải trả về null an toàn (không crash)")
    void parseLoginResponse_nullJson_returnsNullSafely() {
        User result = viewModel.parseLoginResponse(null);
        assertNull(result, "Must return null safely on null input");
    }

    // =========================================================================
    // Test Case 1.1.3: parseErrorMessage - Phân tích thông điệp lỗi
    // =========================================================================

    @Test
    @DisplayName("1.1.3a - JSON lỗi có field 'message' phải trả về đúng thông điệp")
    void parseErrorMessage_jsonWithMessage_returnsCorrectMessage() {
        String errorJson = "{\"success\":false,\"message\":\"Invalid credentials\"}";

        String result = viewModel.parseErrorMessage(errorJson);

        assertEquals("Invalid credentials", result);
    }

    @Test
    @DisplayName("1.1.3b - JSON rỗng phải trả về thông điệp mặc định (không crash)")
    void parseErrorMessage_emptyJson_returnsDefaultMessage() {
        String result = viewModel.parseErrorMessage("");

        assertEquals(
                "Login failed. Please try again.",
                result,
                "Must return default message on empty JSON");
    }

    @Test
    @DisplayName("1.1.3c - JSON null phải trả về thông điệp mặc định (không crash)")
    void parseErrorMessage_nullJson_returnsDefaultMessage() {
        String result = viewModel.parseErrorMessage(null);

        assertEquals(
                "Login failed. Please try again.",
                result,
                "Must return default message on null input");
    }

    @Test
    @DisplayName("1.1.3d - JSON không hợp lệ phải trả về thông điệp mặc định (không crash)")
    void parseErrorMessage_malformedJson_returnsDefaultMessage() {
        String result = viewModel.parseErrorMessage("NOT_VALID_JSON");

        assertEquals(
                "Login failed. Please try again.",
                result,
                "Must return default message on malformed JSON");
    }
}
