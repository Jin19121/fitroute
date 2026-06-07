package com.fitroute.global.cache;

public final class CacheKeyConstants {

    private CacheKeyConstants() {
    } // 인스턴스화 방지

    public static final String TODAY_CACHE_PREFIX = "today:";
    public static final String REFRESH_TOKEN_PREFIX = "RT:"; // AuthService
    public static final String BLACKLIST_PREFIX = "BL:"; // JwtAuthenticationFilter
}