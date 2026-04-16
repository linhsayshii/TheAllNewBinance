package com.auction.client.unit.component.header;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.auction.client.component.shell.HeaderComponentController;

class HeaderComponentControllerTest {

    @Test
    void shouldCreateController() {
        Assertions.assertNotNull(new HeaderComponentController());
    }
}