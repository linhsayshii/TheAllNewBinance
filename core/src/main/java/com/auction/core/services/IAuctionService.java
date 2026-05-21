package com.auction.core.services;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.auction.CreateAuctionRequest;
import com.auction.core.dto.auction.GetAuctionBySellerIdRequest;
import com.auction.core.dto.auction.GetFeaturedAuctionsRequest;
import com.auction.core.dto.auction.PromoteAuctionRequest;
import com.auction.core.dto.auction.PublicAuctionDto;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IAuctionService {
    CompletableFuture<Void> processBid(Bid bid, Auction auction);

    CompletableFuture<Auction> createAuction(CreateAuctionRequest request);

    CompletableFuture<Auction> deleteAuction(Integer auctionId);

    CompletableFuture<Boolean> validateBid(Integer auctionId, Double amount);

    CompletableFuture<Boolean> applySnipeExtension(Auction auction);

    CompletableFuture<com.auction.core.dto.auction.AuctionDetailsDto> getAuctionDetails(
            Integer auctionId);

    CompletableFuture<Integer> getSellerId(Integer auctionId);

    CompletableFuture<List<Auction>> getAuctionsBySellerId(GetAuctionBySellerIdRequest request);

    CompletableFuture<List<PublicAuctionDto>> getPublicAuctions(
            com.auction.core.dto.auction.GetPublicAuctionsRequest request);

    /** Promote một Auction lên Star Auction. Trả về true nếu thành công. */
    CompletableFuture<Boolean> promoteAuction(PromoteAuctionRequest request);

    /** Lấy danh sách Auctions đang Featured, xáo trộn ngẫu nhiên, giới hạn số kết quả. */
    CompletableFuture<List<PublicAuctionDto>> getFeaturedAuctions(
            GetFeaturedAuctionsRequest request);

    /** Admin: Lấy tất cả auctions theo status (cho trang Listing Management). */
    CompletableFuture<List<PublicAuctionDto>> getAllAuctionsForAdmin(
            String status, int page, int size);
}
