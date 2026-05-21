package com.auction.server.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.auction.core.auction.Auction;
import com.auction.core.dto.auction.CreateAuctionRequest;
import com.auction.core.dto.auction.GetAuctionBySellerIdRequest;
import com.auction.core.dto.auction.GetAuctionDetailsRequest;
import com.auction.core.dto.auction.GetFeaturedAuctionsRequest;
import com.auction.core.dto.auction.PromoteAuctionRequest;
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
			com.auction.core.dto.auction.AuctionDetailsDto details = auctionService.getAuctionDetails(getAuctionDetailsRequest.getAuctionId()).join();
			if (details == null) {
				return JsonMapper.toJson(errorResponse("Auction not found"));
			}
			return JsonMapper.toJson(successResponse(details));
		} catch (IllegalArgumentException ex) {
			return JsonMapper.toJson(errorResponse(ex.getMessage()));
		} catch (Exception ex) {
			return JsonMapper.toJson(errorResponse("Internal server error"));
		}
	}

	private Map<String, Object> successResponse(com.auction.core.dto.auction.AuctionDetailsDto details) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		response.put("data", details);
		return response;
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
			List<com.auction.core.dto.auction.PublicAuctionDto> auctions = auctionService.getPublicAuctions(request).join();
			return JsonMapper.toJson(successResponse(auctions));
		} catch (IllegalArgumentException ex) {
			return JsonMapper.toJson(errorResponse(ex.getMessage()));
		} catch (Exception ex) {
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

	private Map<String, Object> successResponse(List<?> dataList, String message) {
		Map<String, Object> response = new HashMap<>();
		response.put("success", true);
		if (message != null) response.put("message", message);
		if (dataList != null) response.put("data", dataList);
		return response;
	}

	public String promoteAuction(String payload) {
		try {
			PromoteAuctionRequest request = JsonMapper.fromJson(payload, PromoteAuctionRequest.class);
			Boolean success = auctionService.promoteAuction(request).join();
			if (Boolean.TRUE.equals(success)) {
				return JsonMapper.toJson(successResponse(null, "Promote thành công!"));
			}
			return JsonMapper.toJson(errorResponse("Promote thất bại, vui lòng thử lại."));
		} catch (SecurityException ex) {
			return JsonMapper.toJson(errorResponse(ex.getMessage()));
		} catch (IllegalArgumentException ex) {
			return JsonMapper.toJson(errorResponse(ex.getMessage()));
		} catch (Exception ex) {
			return JsonMapper.toJson(errorResponse("Internal server error"));
		}
	}

	public String getFeaturedAuctions(String payload) {
		try {
			GetFeaturedAuctionsRequest request = (payload == null || payload.isBlank() || payload.equals("null"))
					? new GetFeaturedAuctionsRequest()
					: JsonMapper.fromJson(payload, GetFeaturedAuctionsRequest.class);
			var auctions = auctionService.getFeaturedAuctions(request).join();
			return JsonMapper.toJson(successResponse(auctions));
		} catch (Exception ex) {
			return JsonMapper.toJson(errorResponse("Internal server error"));
		}
	}

	public String getAllAuctionsForAdmin(String payload) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> params = (payload == null || payload.isBlank() || payload.equals("null"))
					? Map.of()
					: JsonMapper.fromJson(payload, Map.class);
			String status = (String) params.getOrDefault("status", null);
			int page = params.containsKey("page") ? ((Number) params.get("page")).intValue() : 1;
			int size = params.containsKey("size") ? ((Number) params.get("size")).intValue() : 20;
			var auctions = auctionService.getAllAuctionsForAdmin(status, page, size).join();
			return JsonMapper.toJson(successResponse(auctions));
		} catch (Exception ex) {
			return JsonMapper.toJson(errorResponse("Internal server error"));
		}
	}
}