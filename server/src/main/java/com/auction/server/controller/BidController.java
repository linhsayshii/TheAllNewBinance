package com.auction.server.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.auction.core.auction.Bid;
import com.auction.core.dto.BidService.GetBidByAuctionID;
import com.auction.core.dto.BidService.GetBidByBidderID;
import com.auction.core.dto.BidService.PlaceBid;
import com.auction.core.services.IBidService;
import com.auction.core.utils.JsonMapper;

public class BidController {
	private final IBidService bidService;
	
	public BidController(IBidService bidService) {
		this.bidService = bidService;
	}

	public String placeBid(String request) {
		if (request == null) {
			return JsonMapper.toJson(errorResponse("Request payload is required"));
		}
		PlaceBid placeBidRequest = JsonMapper.fromJson(request, PlaceBid.class);
		Bid bid = bidService.placeBid(placeBidRequest);
		if (bid == null) {
			return JsonMapper.toJson(errorResponse("Failed to place bid"));
		}
		return JsonMapper.toJson(successResponse(bid));
	}

	public String getBidsByAuctionId(String request) {
		if (request == null) {
			return JsonMapper.toJson(errorResponse("Request payload is required"));
		}
		GetBidByAuctionID getBidByAuctionIDRequest = JsonMapper.fromJson(request, GetBidByAuctionID.class);
		List<Bid> bids = bidService.getBidsByAuctionId(getBidByAuctionIDRequest);
		if (bids == null) {
			return JsonMapper.toJson(errorResponse("Failed to get bids by auction id"));
		}
		return JsonMapper.toJson(successResponse(bids));
	}

	public String getBidsByBidderId(String request) {
		if (request == null) {
			return JsonMapper.toJson(errorResponse("Request payload is required"));
		}
		GetBidByBidderID getBidByBidderIDRequest = JsonMapper.fromJson(request, GetBidByBidderID.class);
		List<Bid> bids = bidService.getBidsByBidderId(getBidByBidderIDRequest);
		if (bids == null) {
			return JsonMapper.toJson(errorResponse("Failed to get bids by bidder id"));
		}
		return JsonMapper.toJson(successResponse(bids));
	}

	private Map<String, Object> successResponse(Bid bid) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", bid);
		return response;
	}
	private Map<String, Object> successResponse(List<Bid> bids) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", bids);
		return response;
	}
	private Map<String, Object> errorResponse(String message) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", false);
		response.put("message", message);
		return response;
	}
}
