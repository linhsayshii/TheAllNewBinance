package com.auction.server.controller;

import java.util.HashMap;
import java.util.Map;

import com.auction.core.dto.item.GetUploadSignatureRequest;
import com.auction.core.dto.item.GetUploadSignatureResponse;
import com.auction.core.utils.JsonMapper;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

public class ItemController {
    // Cloudinary instance
    private final Cloudinary cloudinary;

    public ItemController() {
        // Cấu hình Cloudinary từ thông tin API (Môi trường/Variables)
        // Hard-code ở đây theo instruction (Thực tế nên đưa vào Config/Env)
        cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dpclmah9p",
            "api_key", "453152866822858",
            "api_secret", "6u0v6ubiLwNtEHIkACjr55HW5a4"
        ));
    }

    public String getUploadSignature(String requestRaw) {
        if (requestRaw == null) {
            return JsonMapper.toJson(errorResponse("Request payload is required"));
        }

        try {
            GetUploadSignatureRequest request = JsonMapper.fromJson(requestRaw, GetUploadSignatureRequest.class);
            if (request == null) {
                return JsonMapper.toJson(errorResponse("Invalid payload"));
            }

            long timestamp = System.currentTimeMillis() / 1000L;
            String folder = request.getFolder() != null && !request.getFolder().isBlank() ? request.getFolder() : "auction_items";

            Map<String, Object> paramsToSign = new HashMap<>();
            paramsToSign.put("timestamp", timestamp);
            paramsToSign.put("folder", folder);

            // Băm tạo chữ ký từ Cloudinary SDK
            String signature = cloudinary.apiSignRequest(paramsToSign, cloudinary.config.apiSecret);

            GetUploadSignatureResponse responseDto = new GetUploadSignatureResponse(
                signature,
                timestamp,
                cloudinary.config.apiKey
            );

            return JsonMapper.toJson(successResponse(responseDto));
        } catch (Exception ex) {
            System.err.println("Error generating cloudinary signature: " + ex.getMessage());
            return JsonMapper.toJson(errorResponse("Could not generate upload signature"));
        }
    }

    private Map<String, Object> successResponse(GetUploadSignatureResponse res) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "success");
        map.put("data", res);
        return map;
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "error");
        map.put("message", message);
        return map;
    }
}
