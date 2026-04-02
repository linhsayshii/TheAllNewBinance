package com.auction.client.unit.page.profile;

import com.auction.client.page.profile.ProfilePageViewModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProfilePageViewModelTest {

    @Test
    void shouldReturnDisplayName() {
        ProfilePageViewModel viewModel = new ProfilePageViewModel();
        Assertions.assertEquals("Guest", viewModel.displayName());
    }
}