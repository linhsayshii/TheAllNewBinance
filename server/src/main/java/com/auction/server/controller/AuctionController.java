package com.auction.server.controller;

import com.auction.core.auction.Auction;
import com.auction.core.dto.auction.CreateAuctionRequest;
import com.auction.core.dto.auction.GetAuctionBySellerIdRequest;
import com.auction.core.dto.auction.GetAuctionDetailsRequest;
import com.auction.core.dto.auction.GetPublicAuctionsRequest;
import com.auction.core.services.IAuctionService;

public class AuctionController extends BaseController {
	private final IAuctionService auctionService;

	public AuctionController(IAuctionService auctionService) {
		this.auctionService = auctionService;
	}

	public String createAuction(String payload) {
		return handleSync(payload, CreateAuctionRequest.class,
				req -> auctionService.createAuction(req).join(),
				"Internal server error");
	}

	public String getAuctionDetails(String payload) {
		return handleSync(payload, GetAuctionDetailsRequest.class, req -> {
			Auction auction = auctionService.getAuctionDetails(req.getAuctionId()).join();
			if (auction == null) {
				throw new IllegalArgumentException("Auction not found");
			}
			return auction;
		}, "Internal server error");
	}

	public String getAuctionsBySellerId(String payload) {
		return handleSync(payload, GetAuctionBySellerIdRequest.class,
				req -> auctionService.getAuctionsBySellerId(req).join(),
				"Internal server error");
	}

	public String getPublicAuctions(String payload) {
		if (payload == null || payload.isBlank() || payload.equals("null")) {
			return handleSync(
					com.auction.core.utils.JsonMapper.toJson(new GetPublicAuctionsRequest()),
					GetPublicAuctionsRequest.class,
					req -> auctionService.getPublicAuctions(req).join(),
					"Internal server error");
		}
		return handleSync(payload, GetPublicAuctionsRequest.class,
				req -> auctionService.getPublicAuctions(req).join(),
				"Internal server error");
	}
}