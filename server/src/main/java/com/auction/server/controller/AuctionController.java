package com.auction.server.controller;

import java.util.HashMap;
import java.util.Map;

import com.auction.core.utils.JsonMapper;

public class AuctionController {
	public String getAuction(String payload) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", false);
		response.put("message", "Feature GET_AUCTION is not implemented yet");
		return JsonMapper.toJson(response);
	}
}
