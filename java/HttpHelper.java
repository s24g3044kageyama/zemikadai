package com.sweets.cake.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP通信およびシンプルなリクエスト解析用ユーティリティ
 */
public class HttpHelper {

    /**
     * リクエストボディの文字列を取得
     */
    public static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /**
     * JSON文字列またはx-www-form-urlencodedからキー・バリューのMapを抽出
     */
    public static Map<String, String> parseParams(String body) {
        Map<String, String> params = new HashMap<>();
        if (body == null || body.trim().isEmpty()) {
            return params;
        }

        String trimmed = body.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            // 簡易JSONパーサー (キー: "key", 値: "string" または 数値)
            Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9]+|true|false|null))");
            Matcher matcher = pattern.matcher(trimmed);
            while (matcher.find()) {
                String key = matcher.group(1);
                String val = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
                params.put(key, val);
            }
        } else {
            // Form URL-encoded (key=value&key2=value2)
            String[] pairs = trimmed.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    try {
                        String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                        String val = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        params.put(key, val);
                    } catch (Exception ignored) {
                    }
                } else if (kv.length == 1) {
                    try {
                        String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                        params.put(key, "");
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return params;
    }

    /**
     * JSONレスポンスを送信
     */
    public static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * リダイレクトレスポンスを送信 (303 See Other)
     */
    public static void sendRedirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1);
    }
}
