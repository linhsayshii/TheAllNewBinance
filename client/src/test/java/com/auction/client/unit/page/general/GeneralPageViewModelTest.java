package com.auction.client.unit.page.general;

import com.auction.client.page.general.GeneralPageViewModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GeneralPageViewModelTest {

    @Test
    void shouldLoadFeaturedAuctions() {
        GeneralPageViewModel viewModel = new GeneralPageViewModel();
        Assertions.assertNotNull(viewModel.loadFeaturedAuctions());
    }
}
