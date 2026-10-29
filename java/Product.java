package com.sweets.cake.model;

import java.util.Objects;

/**
 * ケーキ商品のモデルクラス
 */
public class Product {
    private final String id;
    private final String name;
    private final int price;
    private final String description;
    private final String image;
    private final String category;

    public Product(String id, String name, int price, String description, String image, String category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.image = image;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public String getImage() {
        return image;
    }

    public String getCategory() {
        return category;
    }

    public String toJson() {
        return String.format(
            "{\"id\":\"%s\",\"name\":\"%s\",\"price\":%d,\"description\":\"%s\",\"image\":\"%s\",\"category\":\"%s\"}",
            escapeJson(id),
            escapeJson(name),
            price,
            escapeJson(description),
            escapeJson(image),
            escapeJson(category)
        );
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
