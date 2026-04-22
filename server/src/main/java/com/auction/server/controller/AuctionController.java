package com.auction.server.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.auction.core.auction.Auction;
import com.auction.core.dto.auction.CreateAuctionRequest;
import com.auction.core.dto.auction.GetAuctionBySellerIdRequest;
import com.auction.core.dto.auction.GetAuctionDetailsRequest;
import com.auction.core.services.IAuctionService;
import com.auction.core.utils.JsonMapper;

public class AuctionController {
	private final IAuctionService auctionService;

	public AuctionController(IAuctionService auctionService) {
		this.auctionService = auctionService;
	}

	public String createAuction(String payload) {
		try {
			CreateAuctionRequest createAuctionRequest = JsonMapper.fromJson(payload, CreateAuctionRequest.class);
			Auction auction = auctionService.createAuction(createAuctionRequest).join();
			return JsonMapper.toJson(successResponse(auction));
		} catch (IllegalArgumentException ex) {
			return JsonMapper.toJson(errorResponse(ex.getMessage()));
		} catch (Exception ex) {
			return JsonMapper.toJson(errorResponse("Internal server error"));
		}
	}

	public String getAuctionDetails(String payload) {
		try {
			GetAuctionDetailsRequest getAuctionDetailsRequest = JsonMapper.fromJson(payload,
					GetAuctionDetailsRequest.class);
			Auction auction = auctionService.getAuctionDetails(getAuctionDetailsRequest.getAuctionId()).join();
			if (auction == null) {
				return JsonMapper.toJson(errorResponse("Auction not found"));
			}
			return JsonMapper.toJson(successResponse(auction));
		} catch (IllegalArgumentException ex) {
			return JsonMapper.toJson(errorResponse(ex.getMessage()));
		} catch (Exception ex) {
			return JsonMapper.toJson(errorResponse("Internal server error"));
		}
	}

	public String getAuctionsBySellerId(String payload) {
		try {
			GetAuctionBySellerIdRequest getAuctionBySellerIdRequest = JsonMapper.fromJson(payload,
					GetAuctionBySellerIdRequest.class);
			List<Auction> auctions = auctionService.getAuctionsBySellerId(getAuctionBySellerIdRequest).join();
			if (auctions == null) {
				return JsonMapper.toJson(errorResponse("Failed to get auctions by seller id"));
			}
			return JsonMapper.toJson(successResponse(auctions));
		} catch (IllegalArgumentException ex) {
			return JsonMapper.toJson(errorResponse(ex.getMessage()));
		} catch (Exception ex) {
			return JsonMapper.toJson(errorResponse("Internal server error"));
		}
	}

	public String getPublicAuctions(String payload) {
		try {
			com.auction.core.dto.auction.GetPublicAuctionsRequest request;
			if (payload == null || payload.isBlank() || payload.equals("null")) {
				request = new com.auction.core.dto.auction.GetPublicAuctionsRequest();
			} else {
				request = JsonMapper.fromJson(payload, com.auction.core.dto.auction.GetPublicAuctionsRequest.class);
			}
			List<com.auction.core.dto.auction.PublicAuctionDto> auctions = auctionService.getPublicAuctions(request)
					.join();
			return JsonMapper.toJson(successResponse(auctions));
		} catch (IllegalArgumentException ex) {
			return JsonMapper.toJson(errorResponse(ex.getMessage()));
		} catch (Exception ex) {
			ex.printStackTrace();
			return JsonMapper.toJson(errorResponse("Internal server error"));
		}
	}

	private Map<String, Object> successResponse(Auction auction) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", auction);
		return response;
	}

	private Map<String, Object> successResponse(List<?> dataList) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", dataList);
		return response;
	}

	private Map<String, Object> errorResponse(String message) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", false);
		response.put("message", message);
		return response;
	}
}
