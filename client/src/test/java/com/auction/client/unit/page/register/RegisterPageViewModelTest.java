package com.auction.client.unit.page.register;

import com.auction.client.page.register.RegisterPageViewModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RegisterPageViewModelTest {

    @Test
    void shouldValidateRegistration() {
        RegisterPageViewModel viewModel = new RegisterPageViewModel();
        Assertions.assertTrue(viewModel.validateRegistration("demo", "demo@mail.com", "123456"));
    }
}
