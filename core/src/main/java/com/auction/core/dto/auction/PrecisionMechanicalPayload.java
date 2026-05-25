package com.auction.core.dto.auction;

/**
 * Strongly-typed network payload for PrecisionMechanical items (SPORTS, CAMERAS). All fields are
 * declared with their precise Java target types to prevent Gson-induced ClassCastException at the
 * Server's deserialization boundary.
 */
public class PrecisionMechanicalPayload extends ItemAttributesPayload {

    private String model;

    /**
     * Warranty duration in months. Declared as Integer (not int) to allow null from optional UI.
     */
    private Integer warrantyMonths;

    public PrecisionMechanicalPayload() {}

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(Integer warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }
}
