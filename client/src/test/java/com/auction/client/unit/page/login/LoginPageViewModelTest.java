package com.auction.client.unit.page.login;

import com.auction.client.page.login.LoginPageViewModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LoginPageViewModelTest {

    @Test
    void shouldValidateCredentials() {
        LoginPageViewModel viewModel = new LoginPageViewModel();
        Assertions.assertTrue(viewModel.validateCredentials("demo", "1234"));
    }
}
