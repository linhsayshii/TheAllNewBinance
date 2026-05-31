package com.auction.client.scene;

import java.util.Map;

/**
 * Interface for controllers that can receive navigation parameters. Implement this in any page
 * controller that needs data passed from the previous screen (e.g. auctionId).
 */
public interface DataReceivable {
    void onDataReceived(Map<String, Object> data);
}
