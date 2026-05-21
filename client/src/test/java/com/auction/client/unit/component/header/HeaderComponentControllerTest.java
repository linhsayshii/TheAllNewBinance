package com.auction.client.unit.component.header;

import com.auction.client.component.shell.HeaderComponentController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HeaderComponentControllerTest {

    @Test
    void shouldCreateController() {
        Assertions.assertNotNull(new HeaderComponentController());
    }
}
