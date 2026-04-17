package com.auction.server.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.auction.core.auction.Bid;
import com.auction.core.dto.bid.GetBidByAuctionIdRequest;
import com.auction.core.dto.bid.GetBidByBidderIdRequest;
import com.auction.core.dto.bid.PlaceBid;
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
		try {
			PlaceBid placeBidRequest = JsonMapper.fromJson(request, PlaceBid.class);
			Bid bid = bidService.placeBid(placeBidRequest).join();
			if (bid == null) {
				return JsonMapper.toJson(errorResponse("Failed to place bid"));
			}
			return JsonMapper.toJson(successResponse(bid));
		} catch (IllegalArgumentException | IllegalStateException ex) {
			return JsonMapper.toJson(errorResponse(ex.getMessage()));
		} catch (Exception ex) {
			return JsonMapper.toJson(errorResponse("Internal server error while placing bid"));
		}
	}

	public String getBidsByAuctionId(String request) {
		if (request == null) {
			return JsonMapper.toJson(errorResponse("Request payload is required"));
		}
		try {
			GetBidByAuctionIdRequest getBidByAuctionIDRequest = JsonMapper.fromJson(request, GetBidByAuctionIdRequest.class);
			List<Bid> bids = bidService.getBidsByAuctionId(getBidByAuctionIDRequest).join();
			if (bids == null) {
				return JsonMapper.toJson(errorResponse("Failed to get bids by auction id"));
			}
			return JsonMapper.toJson(successResponse(bids));
		} catch (IllegalArgumentException ex) {
			return JsonMapper.toJson(errorResponse(ex.getMessage()));
		} catch (Exception ex) {
			return JsonMapper.toJson(errorResponse("Internal server error"));
		}
	}

	public String getBidsByBidderId(String request) {
		if (request == null) {
			return JsonMapper.toJson(errorResponse("Request payload is required"));
		}
		try {
			GetBidByBidderIdRequest getBidByBidderIDRequest = JsonMapper.fromJson(request, GetBidByBidderIdRequest.class);
			List<Bid> bids = bidService.getBidsByBidderId(getBidByBidderIDRequest).join();
			if (bids == null) {
				return JsonMapper.toJson(errorResponse("Failed to get bids by bidder id"));
			}
			return JsonMapper.toJson(successResponse(bids));
		} catch (IllegalArgumentException ex) {
			return JsonMapper.toJson(errorResponse(ex.getMessage()));
		} catch (Exception ex) {
			return JsonMapper.toJson(errorResponse("Internal server error"));
		}
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
