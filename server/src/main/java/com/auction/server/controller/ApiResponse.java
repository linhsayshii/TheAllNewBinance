package com.auction.server.controller;

import com.auction.core.exception.ErrorCode;
import com.auction.core.utils.JsonMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized API response builder. Eliminates duplicated errorResponse/successResponse across all
 * controllers.
 */
public final class ApiResponse {

    private ApiResponse() {}

    public static String success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return JsonMapper.toJson(response);
    }

    public static String successMessage(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        return JsonMapper.toJson(response);
    }

    /** Legacy generic error without a typed error code. Used for infrastructure-level failures. */
    public static String error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return JsonMapper.toJson(response);
    }

    /**
     * Typed domain error response. Serializes the numeric ErrorCode value so the client can
     * perform type-safe dispatch via ClientExceptionFactory without brittle string matching.
     */
    public static String error(ErrorCode errorCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", errorCode.getValue());
        response.put("message", message);
        return JsonMapper.toJson(response);
    }
}
