package com.auction.core.dto.auction;

/**
 * Strongly-typed network payload for LuxuryCollectible items (WATCHES, FASHION, COLLECTIBLES,
 * WINE). All fields are declared with their precise Java target types to prevent Gson-induced
 * ClassCastException at the Server's deserialization boundary.
 */
public class LuxuryCollectiblePayload extends ItemAttributesPayload {

    private String brand;
    private String condition;
    private boolean hasCertificate;

    /** Bottle volume in litres — applicable to WINE category only. */
    private Double bottleSize;

    /** Movement type (e.g. "Automatic", "Quartz") — applicable to WATCHES category only. */
    private String watchMovement;

    /** Size label (e.g. "M", "L", "42") — applicable to FASHION category only. */
    private String fashionSize;

    public LuxuryCollectiblePayload() {}

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public boolean isHasCertificate() {
        return hasCertificate;
    }

    public void setHasCertificate(boolean hasCertificate) {
        this.hasCertificate = hasCertificate;
    }

    public Double getBottleSize() {
        return bottleSize;
    }

    public void setBottleSize(Double bottleSize) {
        this.bottleSize = bottleSize;
    }

    public String getWatchMovement() {
        return watchMovement;
    }

    public void setWatchMovement(String watchMovement) {
        this.watchMovement = watchMovement;
    }

    public String getFashionSize() {
        return fashionSize;
    }

    public void setFashionSize(String fashionSize) {
        this.fashionSize = fashionSize;
    }
}
