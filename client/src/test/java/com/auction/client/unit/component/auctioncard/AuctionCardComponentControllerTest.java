package com.auction.client.unit.component.auctioncard;

import com.auction.client.component.item.AuctionCardComponentController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AuctionCardComponentControllerTest {

    @Test
    void shouldCreateController() {
        Assertions.assertNotNull(new AuctionCardComponentController());
    }
}
