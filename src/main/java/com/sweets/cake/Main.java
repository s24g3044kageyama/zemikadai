package com.sweets.cake;

import com.sweets.cake.server.CakeHttpServer;

import java.io.File;

/**
 * ケーキ通販Webアプリケーションのエントリーポイント
 */
public class Main {
    public static void main(String[] args) {
        int port = 80;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        } else {
            String envPort = System.getenv("PORT");
            if (envPort != null && !envPort.isEmpty()) {
                try {
                    port = Integer.parseInt(envPort);
                } catch (NumberFormatException ignored) {}
            }
        }

        // 静的ファイルディレクトリの判定
        String staticDir = findStaticDir();
        System.out.println("Using static directory: " + staticDir);

        try {
            CakeHttpServer server = new CakeHttpServer(port, staticDir);
            server.start();

            // シャットダウンフックの登録
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nシャットダウン要求を受信しました。サーバーを終了します...");
                server.stop();
            }));

            // メインスレッドを待機
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("サーバー起動エラー: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String findStaticDir() {
        String[] candidates = {
            "static",
            "/static",
            "./static",
            "../static",
            "web",
            "/app/static"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.isDirectory() && new File(f, "index.html").exists()) {
                return f.getAbsolutePath();
            }
        }
        // デフォルト
        File f = new File("static");
        if (f.exists()) return f.getAbsolutePath();
        return new File("/static").getAbsolutePath();
    }
}
