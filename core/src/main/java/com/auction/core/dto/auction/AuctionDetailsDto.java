package com.auction.core.dto.auction;

import com.auction.core.auction.Auction;
import com.auction.core.products.Item;
import com.auction.core.users.User;

public class AuctionDetailsDto {
    private Auction auction;
    private Item item;
    private User seller;

    public AuctionDetailsDto() {
    }

    public AuctionDetailsDto(Auction auction, Item item, User seller) {
        this.auction = auction;
        this.item = item;
        this.seller = seller;
    }

    public Auction getAuction() {
        return auction;
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public User getSeller() {
        return seller;
    }

    public void setSeller(User seller) {
        this.seller = seller;
    }
}
