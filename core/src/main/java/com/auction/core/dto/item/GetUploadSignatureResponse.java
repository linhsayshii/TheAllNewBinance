package com.auction.core.dto.item;

public class GetUploadSignatureResponse {
    private String signature;
    private long timestamp;
    private String apiKey;

    public GetUploadSignatureResponse() {
    }

    public GetUploadSignatureResponse(String signature, long timestamp, String apiKey) {
        this.signature = signature;
        this.timestamp = timestamp;
        this.apiKey = apiKey;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
