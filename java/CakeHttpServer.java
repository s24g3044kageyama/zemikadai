package com.sweets.cake.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sweets.cake.model.Cart;
import com.sweets.cake.model.Product;
import com.sweets.cake.service.ProductService;
import com.sweets.cake.service.SessionService;
import com.sweets.cake.util.HttpHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * ケーキ通販Webサーバー
 * 0.0.0.0:80 で待ち受けを行い、APIと静的Webコンテンツを配信します。
 */
public class CakeHttpServer {
    private final int port;
    private final String staticDir;
    private final ProductService productService;
    private final SessionService sessionService;
    private HttpServer server;

    public CakeHttpServer(int port, String staticDir) {
        this.port = port;
        this.staticDir = staticDir;
        this.productService = new ProductService();
        this.sessionService = new SessionService();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        // API エンドポイント
        server.createContext("/api/products", new ProductsApiHandler());
        server.createContext("/api/cart/add", new AddCartApiHandler());
        server.createContext("/api/cart/update", new UpdateCartApiHandler());
        server.createContext("/api/cart/remove", new RemoveCartApiHandler());
        server.createContext("/api/cart/clear", new ClearCartApiHandler());
        server.createContext("/api/cart", new GetCartApiHandler());

        // HTML フォーム送信用のPOSTエンドポイント (JSオフ環境や通常フォーム遷移にも対応)
        server.createContext("/cart/add", new FormAddToCartHandler());
        server.createContext("/cart/update", new FormUpdateCartHandler());
        server.createContext("/cart/remove", new FormRemoveCartHandler());
        server.createContext("/cart/clear", new FormClearCartHandler());

        // 静的ファイルおよびページルーティング
        server.createContext("/", new StaticFileHandler());

        // 仮想スレッド Executor を使用 (Java 21)
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        System.out.println("==================================================");
        System.out.println(" ケーキ通販Webサーバーが起動しました");
        System.out.println(" 待受アドレス: 0.0.0.0:" + port);
        System.out.println(" 静的ファイル配置: " + new File(staticDir).getAbsolutePath());
        System.out.println(" アクセスURL: http://localhost/");
        System.out.println("==================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            System.out.println("サーバーを停止しました。");
        }
    }

    // ----------------- Handlers -----------------

    /**
     * 商品一覧API
     * GET /api/products
     */
    private class ProductsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            sessionService.getOrCreateCart(exchange); // セッション確認・維持
            String json = productService.getAllProductsJson();
            HttpHelper.sendJsonResponse(exchange, 200, json);
        }
    }

    /**
     * カート情報取得API
     * GET /api/cart
     */
    private class GetCartApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            Cart cart = sessionService.getOrCreateCart(exchange);
            HttpHelper.sendJsonResponse(exchange, 200, cart.toJson());
        }
    }

    /**
     * カート追加API
     * POST /api/cart/add (Body: productId, quantity)
     */
    private class AddCartApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            Cart cart = sessionService.getOrCreateCart(exchange);
            String body = HttpHelper.readRequestBody(exchange);
            Map<String, String> params = HttpHelper.parseParams(body);

            String productId = params.get("productId");
            int quantity = 1;
            try {
                if (params.containsKey("quantity")) {
                    quantity = Integer.parseInt(params.get("quantity"));
                }
            } catch (NumberFormatException ignored) {
            }

            Product product = productService.getProductById(productId);
            if (product != null && quantity > 0) {
                cart.addItem(product, quantity);
                HttpHelper.sendJsonResponse(exchange, 200, cart.toJson());
            } else {
                HttpHelper.sendJsonResponse(exchange, 400, "{\"error\":\"Invalid product or quantity\"}");
            }
        }
    }

    /**
     * カート数量変更API
     * POST /api/cart/update (Body: productId, quantity)
     */
    private class UpdateCartApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            Cart cart = sessionService.getOrCreateCart(exchange);
            String body = HttpHelper.readRequestBody(exchange);
            Map<String, String> params = HttpHelper.parseParams(body);

            String productId = params.get("productId");
            try {
                int quantity = Integer.parseInt(params.get("quantity"));
                cart.updateQuantity(productId, quantity);
                HttpHelper.sendJsonResponse(exchange, 200, cart.toJson());
            } catch (Exception e) {
                HttpHelper.sendJsonResponse(exchange, 400, "{\"error\":\"Invalid parameters\"}");
            }
        }
    }

    /**
     * カート商品削除API
     * POST /api/cart/remove (Body: productId)
     */
    private class RemoveCartApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            Cart cart = sessionService.getOrCreateCart(exchange);
            String body = HttpHelper.readRequestBody(exchange);
            Map<String, String> params = HttpHelper.parseParams(body);

            String productId = params.get("productId");
            if (productId != null) {
                cart.removeItem(productId);
                HttpHelper.sendJsonResponse(exchange, 200, cart.toJson());
            } else {
                HttpHelper.sendJsonResponse(exchange, 400, "{\"error\":\"Missing productId\"}");
            }
        }
    }

    /**
     * カートクリアAPI
     * POST /api/cart/clear
     */
    private class ClearCartApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            Cart cart = sessionService.getOrCreateCart(exchange);
            cart.clear();
            HttpHelper.sendJsonResponse(exchange, 200, cart.toJson());
        }
    }

    // --- HTML フォーム POST用ハンドラー（リダイレクト対応） ---

    private class FormAddToCartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Cart cart = sessionService.getOrCreateCart(exchange);
                String body = HttpHelper.readRequestBody(exchange);
                Map<String, String> params = HttpHelper.parseParams(body);
                String productId = params.get("productId");
                int quantity = 1;
                try {
                    quantity = Integer.parseInt(params.get("quantity"));
                } catch (Exception ignored) {}
                Product product = productService.getProductById(productId);
                if (product != null && quantity > 0) {
                    cart.addItem(product, quantity);
                }
                String redirect = params.getOrDefault("redirect", "/cart.html");
                HttpHelper.sendRedirect(exchange, redirect);
            } else {
                HttpHelper.sendRedirect(exchange, "/");
            }
        }
    }

    private class FormUpdateCartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Cart cart = sessionService.getOrCreateCart(exchange);
                String body = HttpHelper.readRequestBody(exchange);
                Map<String, String> params = HttpHelper.parseParams(body);
                String productId = params.get("productId");
                try {
                    int quantity = Integer.parseInt(params.get("quantity"));
                    cart.updateQuantity(productId, quantity);
                } catch (Exception ignored) {}
                HttpHelper.sendRedirect(exchange, "/cart.html");
            } else {
                HttpHelper.sendRedirect(exchange, "/cart.html");
            }
        }
    }

    private class FormRemoveCartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Cart cart = sessionService.getOrCreateCart(exchange);
                String body = HttpHelper.readRequestBody(exchange);
                Map<String, String> params = HttpHelper.parseParams(body);
                String productId = params.get("productId");
                if (productId != null) {
                    cart.removeItem(productId);
                }
                HttpHelper.sendRedirect(exchange, "/cart.html");
            } else {
                HttpHelper.sendRedirect(exchange, "/cart.html");
            }
        }
    }

    private class FormClearCartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Cart cart = sessionService.getOrCreateCart(exchange);
                cart.clear();
                HttpHelper.sendRedirect(exchange, "/cart.html");
            } else {
                HttpHelper.sendRedirect(exchange, "/cart.html");
            }
        }
    }

    /**
     * 静的ファイル配信ハンドラー
     */
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            sessionService.getOrCreateCart(exchange); // セッション維持

            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            } else if (path.equals("/cart")) {
                path = "/cart.html";
            }

            // ディレクトリトラバーサル防止
            Path targetFile = Paths.get(staticDir, path).normalize();
            Path basePath = Paths.get(staticDir).normalize();
            if (!targetFile.startsWith(basePath)) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            File file = targetFile.toFile();
            if (!file.exists() || file.isDirectory()) {
                // 404 Not Found
                byte[] notFoundBytes = "<h1>404 Not Found</h1><p>お探しのページは見つかりませんでした。<a href=\"/\">トップへ戻る</a></p>".getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(404, notFoundBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFoundBytes);
                }
                return;
            }

            String method = exchange.getRequestMethod();
            String contentType = probeContentType(targetFile);
            exchange.getResponseHeaders().set("Content-Type", contentType);

            if ("HEAD".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            exchange.sendResponseHeaders(200, file.length());
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            }
        }

        private String probeContentType(Path path) {
            String fileName = path.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".html")) return "text/html; charset=UTF-8";
            if (fileName.endsWith(".css")) return "text/css; charset=UTF-8";
            if (fileName.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (fileName.endsWith(".svg")) return "image/svg+xml";
            if (fileName.endsWith(".png")) return "image/png";
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
            if (fileName.endsWith(".gif")) return "image/gif";
            if (fileName.endsWith(".json")) return "application/json; charset=UTF-8";
            if (fileName.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }
}
