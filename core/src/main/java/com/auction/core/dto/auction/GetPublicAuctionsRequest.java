package com.auction.core.dto.auction;

public class GetPublicAuctionsRequest {
    private int page = 1;
    private int size = 20;
    private String status = "ACTIVE";
    private boolean includeEndingSoon = true;
    private boolean includeTrending = false;

    public GetPublicAuctionsRequest() {}

    public GetPublicAuctionsRequest(int page, int size, String status, boolean includeEndingSoon, boolean includeTrending) {
        this.page = page;
        this.size = size;
        this.status = status;
        this.includeEndingSoon = includeEndingSoon;
        this.includeTrending = includeTrending;
    }

    public int getPage() {return page; }
    public void setPage(int page) {this.page = page; }

    public int getSize() {return size;}
    public void setSize(int size) {this.size = size; }

    public String getStatus() {return status; }
    public void setStatus(String status) {this.status = status;}

    public boolean isIncludeEndingSoon() {return includeEndingSoon; }
    public void setIncludeEndingSoon(boolean includeEndingSoon) {this.includeEndingSoon = includeEndingSoon;}

    public boolean isIncludeTrending() {return includeTrending; }
    public void setIncludeTrending(boolean includeTrending) {this.includeTrending = includeTrending; }
}
