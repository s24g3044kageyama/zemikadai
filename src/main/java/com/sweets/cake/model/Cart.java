package com.sweets.cake.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ショッピングカートモデル（サーバーサイド管理）
 */
public class Cart {
    // 順序を保持するためにLinkedHashMapを使用
    private final Map<String, CartItem> items = new LinkedHashMap<>();

    /**
     * 商品をカートに追加（すでに存在する場合は個数を加算）
     */
    public synchronized void addItem(Product product, int quantity) {
        if (product == null || quantity <= 0) {
            return;
        }
        String productId = product.getId();
        if (items.containsKey(productId)) {
            CartItem existing = items.get(productId);
            int newQuantity = existing.getQuantity() + quantity;
            // 上限は99個とする
            existing.setQuantity(Math.min(99, newQuantity));
        } else {
            items.put(productId, new CartItem(product, Math.min(99, quantity)));
        }
    }

    /**
     * 商品の個数を変更
     */
    public synchronized void updateQuantity(String productId, int quantity) {
        if (productId == null) return;
        if (quantity <= 0) {
            items.remove(productId);
        } else if (items.containsKey(productId)) {
            items.get(productId).setQuantity(Math.min(99, quantity));
        }
    }

    /**
     * カートから商品を削除
     */
    public synchronized void removeItem(String productId) {
        if (productId != null) {
            items.remove(productId);
        }
    }

    /**
     * カートを空にする
     */
    public synchronized void clear() {
        items.clear();
    }

    /**
     * カート内の商品アイテム一覧
     */
    public synchronized List<CartItem> getItems() {
        return new ArrayList<>(items.values());
    }

    /**
     * カート内の合計数量
     */
    public synchronized int getTotalCount() {
        return items.values().stream().mapToInt(CartItem::getQuantity).sum();
    }

    /**
     * カート内の合計金額（税込）
     */
    public synchronized int getTotalPrice() {
        return items.values().stream().mapToInt(CartItem::getSubtotal).sum();
    }

    /**
     * JSON文字列化
     */
    public synchronized String toJson() {
        String itemsJson = items.values().stream()
                .map(CartItem::toJson)
                .collect(Collectors.joining(","));
        
        return String.format(
            "{\"items\":[%s],\"totalCount\":%d,\"totalPrice\":%d}",
            itemsJson,
            getTotalCount(),
            getTotalPrice()
        );
    }
}
