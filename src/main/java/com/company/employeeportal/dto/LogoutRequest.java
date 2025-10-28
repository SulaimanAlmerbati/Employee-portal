package com.company.employeeportal.dto;

/**
 * Data Transfer Object for logout requests.
 * Optional DTO to handle logout requests that include refresh tokens.
 */
public class LogoutRequest {

    private String refreshToken;

    public LogoutRequest() {}

    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public String toString() {
        return "LogoutRequest{" +
                "refreshToken='[PROTECTED]'" +
                '}';
    }
}