package com.auction.client.unit.page.general;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.auction.client.page.general.GeneralPageViewModel;

class GeneralPageViewModelTest {

    @Test
    void shouldLoadFeaturedAuctions() {
        GeneralPageViewModel viewModel = new GeneralPageViewModel();
        Assertions.assertNotNull(viewModel.loadFeaturedAuctions());
    }
}