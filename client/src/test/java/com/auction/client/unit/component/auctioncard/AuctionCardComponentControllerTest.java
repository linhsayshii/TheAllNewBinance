package com.auction.client.unit.component.auctioncard;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.auction.client.component.item.AuctionCardComponentController;

class AuctionCardComponentControllerTest {

    @Test
    void shouldCreateController() {
        Assertions.assertNotNull(new AuctionCardComponentController());
    }
}