package com.auction.server.controller;

import com.auction.core.dto.item.GetUploadSignatureRequest;
import com.auction.core.dto.item.GetUploadSignatureResponse;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.util.HashMap;
import java.util.Map;

public class ItemController extends BaseController {
    // Cloudinary instance
    private final Cloudinary cloudinary;

    public ItemController() {
        // Cấu hình Cloudinary từ thông tin API (Môi trường/Variables)
        // Hard-code ở đây theo instruction (Thực tế nên đưa vào Config/Env)
        cloudinary =
                new Cloudinary(
                        ObjectUtils.asMap(
                                "cloud_name", "dpclmah9p",
                                "api_key", "453152866822858",
                                "api_secret", "6u0v6ubiLwNtEHIkACjr55HW5a4"));
    }

    public String getUploadSignature(String requestRaw) {
        return handleSync(
                requestRaw,
                GetUploadSignatureRequest.class,
                request -> {
                    long timestamp = System.currentTimeMillis() / 1000L;
                    String folder =
                            request.getFolder() != null && !request.getFolder().isBlank()
                                    ? request.getFolder()
                                    : "auction_items";

                    Map<String, Object> paramsToSign = new HashMap<>();
                    paramsToSign.put("timestamp", timestamp);
                    paramsToSign.put("folder", folder);

                    try {
                        // Băm tạo chữ ký từ Cloudinary SDK
                        String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);
                        return new GetUploadSignatureResponse(signature, timestamp, cloudinary.config.apiKey);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                "Could not generate upload signature");
    }
}
