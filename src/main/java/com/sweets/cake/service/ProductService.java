package com.sweets.cake.service;

import com.sweets.cake.model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ケーキ商品のカタログ管理サービス
 */
public class ProductService {
    private final Map<String, Product> productCatalog = new LinkedHashMap<>();

    public ProductService() {
        initProducts();
    }

    private void initProducts() {
        addProduct(new Product(
            "strawberry-shortcake",
            "王道 苺のプレミアムショートケーキ",
            680,
            "しっとり焼き上げた極上スポンジと北海道産濃原生クリームに、甘酸っぱい厳選国産苺を贅沢に飾りました。",
            "/images/strawberry-shortcake.svg",
            "ショートケーキ"
        ));

        addProduct(new Product(
            "sacher-torte",
            "濃厚ショコラ クラシック・ザッハトルテ",
            720,
            "オーストリア伝統製法を受け継ぎ、ベルギー産最高級クーベルチュールチョコレートとアプリコットジャムを合わせた芳醇な味わい。",
            "/images/sacher-torte.svg",
            "チョコレート"
        ));

        addProduct(new Product(
            "matcha-opera",
            "京都宇治抹茶の極みオペラ",
            700,
            "挽きたての宇治抹茶ガナッシュとアーモンド香るビスキュイを何層にも重ねた、大人のための和洋折衷スイーツ。",
            "/images/matcha-opera.svg",
            "和風・抹茶"
        ));

        addProduct(new Product(
            "mango-tart",
            "完熟マンゴーと旬果実の贅沢タルト",
            750,
            "サクサクの自家製パートシュクレに濃厚なクレームダマンドとカスタード、太陽を浴びた完熟マンゴーを華やかにトッピング。",
            "/images/mango-tart.svg",
            "フルーツタルト"
        ));

        addProduct(new Product(
            "newyork-cheesecake",
            "ベイクド・ニューヨークチーズケーキ",
            590,
            "北海道産クリームチーズとサワークリームを低温でじっくり湯煎焼き。なめらかな口溶けと豊かなコクが自慢の逸品。",
            "/images/newyork-cheesecake.svg",
            "チーズケーキ"
        ));

        addProduct(new Product(
            "mont-blanc",
            "和栗のプレミアムモンブラン",
            730,
            "国産和栗の風味をダイレクトに感じるマロンペーストを絞り、中には渋皮栗と無糖の軽やかなシャンティクリームを包みました。",
            "/images/mont-blanc.svg",
            "モンブラン"
        ));

        addProduct(new Product(
            "pistachio-berry",
            "ピスタチオと木苺のルージュムース",
            760,
            "シチリア産ピスタチオの濃厚ババロワに、キリッとしたフランボワーズジュレを合わせた美しいグラデーションケーキ。",
            "/images/pistachio-berry.svg",
            "ムース"
        ));

        addProduct(new Product(
            "mille-crepe",
            "焦がしバター香る極上ミルクレープ",
            650,
            "一枚一枚手焼きした極薄クレープ生地と、マダガスカル産バニラビーンズ香るカスタードクリームの至高の20層ハーモニー。",
            "/images/mille-crepe.svg",
            "クレープ"
        ));
    }

    private void addProduct(Product product) {
        productCatalog.put(product.getId(), product);
    }

    public List<Product> getAllProducts() {
        return Collections.unmodifiableList(new ArrayList<>(productCatalog.values()));
    }

    public Product getProductById(String id) {
        return productCatalog.get(id);
    }

    public String getAllProductsJson() {
        String items = productCatalog.values().stream()
                .map(Product::toJson)
                .collect(Collectors.joining(","));
        return "[" + items + "]";
    }
}
