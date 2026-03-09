package com.example.library;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
/**
 * Springコンテキストの起動を検証する。
 */
class LibraryApplicationTests {

    /**
     * 対象：Springコンテキスト起動
     * 観点：アプリケーションが起動可能であること（設定/Bean定義の回帰検知）
     * 結果：例外なくコンテキストがロードされる
     */
    @Test
    void contextLoads() {
    }
}
