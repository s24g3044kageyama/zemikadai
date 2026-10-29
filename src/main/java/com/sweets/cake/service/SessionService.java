package com.sweets.cake.service;

import com.sun.net.httpserver.HttpExchange;
import com.sweets.cake.model.Cart;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * サーバー側セッション管理サービス
 * クライアントのCookieと紐づけてCartオブジェクトを保持します。
 */
public class SessionService {
    private static final String COOKIE_NAME = "CAKE_SESSION_ID";
    private final Map<String, Cart> sessions = new ConcurrentHashMap<>();

    /**
     * リクエストからSessionIdを取得、または新規発行してCartを取得する
     * 新規発行またはセッション更新時はレスポンスヘッダにSet-Cookieを設定する
     */
    public Cart getOrCreateCart(HttpExchange exchange) {
        String sessionId = getSessionIdFromExchange(exchange);
        boolean isNew = false;

        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
            isNew = true;
        }

        Cart cart = sessions.computeIfAbsent(sessionId, k -> new Cart());

        if (isNew) {
            // Set-Cookieヘッダーを付与 (有効期限24時間 = 86400秒)
            String cookieHeader = String.format("%s=%s; Path=/; Max-Age=86400; HttpOnly; SameSite=Lax", COOKIE_NAME, sessionId);
            exchange.getResponseHeaders().add("Set-Cookie", cookieHeader);
        }

        return cart;
    }

    /**
     * HttpExchangeのリクエストヘッダーからCookieを解析してSessionIdを取り出す
     */
    private String getSessionIdFromExchange(HttpExchange exchange) {
        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");
        if (cookieHeaders == null) {
            return null;
        }

        for (String header : cookieHeaders) {
            String[] cookies = header.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && COOKIE_NAME.equals(parts[0].trim())) {
                    return parts[1].trim();
                }
            }
        }
        return null;
    }
}
