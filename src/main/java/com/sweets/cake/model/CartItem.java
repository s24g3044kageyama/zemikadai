package com.sweets.cake.model;

/**
 * カート内商品明細モデル
 */
public class CartItem {
    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = Math.max(1, quantity);
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void addQuantity(int count) {
        this.quantity += count;
    }

    /**
     * 小計金額（単価 × 個数）
     */
    public int getSubtotal() {
        return product.getPrice() * quantity;
    }

    public String toJson() {
        return String.format(
            "{\"product\":%s,\"quantity\":%d,\"subtotal\":%d}",
            product.toJson(),
            quantity,
            getSubtotal()
        );
    }
}
