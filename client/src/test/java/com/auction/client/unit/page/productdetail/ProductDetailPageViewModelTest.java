package com.auction.client.unit.page.productdetail;

import com.auction.client.page.productdetail.ProductDetailPageViewModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProductDetailPageViewModelTest {

    @Test
    void shouldReturnPlaceholderTitle() {
        ProductDetailPageViewModel viewModel = new ProductDetailPageViewModel();
        Assertions.assertFalse(viewModel.productTitle().isBlank());
    }
}