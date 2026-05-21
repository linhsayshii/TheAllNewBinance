package com.auction.core.dto.item;

public class GetUploadSignatureRequest {
    private String folder;

    public GetUploadSignatureRequest() {
    }

    public GetUploadSignatureRequest(String folder) {
        this.folder = folder;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}
