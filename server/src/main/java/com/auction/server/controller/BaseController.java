package com.auction.server.controller;

import com.auction.core.exception.DomainException;
import com.auction.core.utils.JsonMapper;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Template for controller methods — eliminates repeated null-check → deserialize → call service →
 * wrap response → catch boilerplate.
 */
public abstract class BaseController {

    /**
     * Handles a synchronous request with the standard controller flow: 1. Null-check payload 2.
     * Deserialize JSON → DTO 3. Execute handler (business logic) 4. Wrap result in success/error
     * response
     *
     * @param request raw JSON string
     * @param requestType DTO class to deserialize into
     * @param handler business logic; may throw DomainException for business rule violations
     * @param fallbackError generic error message when an unexpected exception occurs
     */
    protected <T, R> String handleSync(
            String request, Class<T> requestType, Function<T, R> handler, String fallbackError) {
        if (request == null) {
            return ApiResponse.error("Request payload is required");
        }
        try {
            T dto = JsonMapper.fromJson(request, requestType);
            if (dto == null) {
                return ApiResponse.error("Invalid request payload");
            }
            R result = handler.apply(dto);
            if (result == null) {
                return ApiResponse.error(fallbackError);
            }
            return ApiResponse.success(result);
        } catch (DomainException ex) {
            System.err.printf(
                    "[DOMAIN ERROR] Code: %d | Type: %s | Message: %s%n",
                    ex.getErrorCode().getValue(), ex.getClass().getSimpleName(), ex.getMessage());
            return ApiResponse.error(ex.getErrorCode(), ex.getMessage());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResponse.error(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace(); // CRITICAL for debugging server-side errors
            return ApiResponse.error(fallbackError);
        }
    }

    /** Handles an asynchronous request: same flow as handleSync but returns CompletableFuture. */
    protected <T> CompletableFuture<String> handleAsync(
            String request,
            Class<T> requestType,
            Function<T, CompletableFuture<Object>> handler,
            String fallbackError) {
        if (request == null) {
            return CompletableFuture.completedFuture(
                    ApiResponse.error("Request payload is required"));
        }
        try {
            T dto = JsonMapper.fromJson(request, requestType);
            if (dto == null) {
                return CompletableFuture.completedFuture(
                        ApiResponse.error("Invalid request payload"));
            }
            return handler.apply(dto)
                    .thenApply(
                            result -> {
                                if (result == null) {
                                    return ApiResponse.error(fallbackError);
                                }
                                return ApiResponse.success(result);
                            })
                    .exceptionally(
                            ex -> {
                                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                                if (cause instanceof DomainException domainEx) {
                                    System.err.printf(
                                            "[DOMAIN ERROR] Code: %d | Type: %s | Message: %s%n",
                                            domainEx.getErrorCode().getValue(),
                                            domainEx.getClass().getSimpleName(),
                                            domainEx.getMessage());
                                    return ApiResponse.error(
                                            domainEx.getErrorCode(), domainEx.getMessage());
                                }
                                cause.printStackTrace(); // CRITICAL for debugging server-side
                                // errors
                                return ApiResponse.error(cause.getMessage());
                            });
        } catch (DomainException ex) {
            System.err.printf(
                    "[DOMAIN ERROR] Code: %d | Type: %s | Message: %s%n",
                    ex.getErrorCode().getValue(), ex.getClass().getSimpleName(), ex.getMessage());
            return CompletableFuture.completedFuture(
                    ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return CompletableFuture.completedFuture(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace(); // CRITICAL for debugging server-side errors
            return CompletableFuture.completedFuture(ApiResponse.error(fallbackError));
        }
    }
}
