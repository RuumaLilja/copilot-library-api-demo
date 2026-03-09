# 図書貸出管理API ブラックボックステストケース一覧（APIテスター向け）

対象: 図書貸出管理API
- `POST /api/v1/books`
- `GET /api/v1/books`
- `POST /api/v1/books/{bookId}/lend`
- `POST /api/v1/books/{bookId}/return`

前提:
- Base URL: `http://localhost:8080`
- 送受信: JSON（`Content-Type: application/json`）
- エラー形式（想定）:

```json
{
  "error": {
    "code": "...",
    "message": "...",
    "details": {}
  }
}
```

---

## 1. 共通観点（全エンドポイント）

### 共通-01 Content-Type不足/不正
- 対象: JSONボディを持つリクエスト（`POST /api/v1/books`）
- 入力:
  - `Content-Type` なし
  - `Content-Type: text/plain`
- 期待:
  - `415 Unsupported Media Type`
  - `error.code = UNSUPPORTED_MEDIA_TYPE`

### 共通-02 Accept不正
- 入力: `Accept: text/plain` 等
- 期待:
  - `406 Not Acceptable`
  - `error.code = NOT_ACCEPTABLE`

### 共通-03 不正JSON
- 対象: `POST /api/v1/books`
- 入力: ボディが `{` で終わる等
- 期待:
  - `400 Bad Request`
  - `error.code = MALFORMED_JSON`

### 共通-04 Method不正
- 入力:
  - `PUT /api/v1/books`
  - `GET /api/v1/books/{id}/lend` など
- 期待:
  - `405 Method Not Allowed`
  - `error.code = METHOD_NOT_ALLOWED`

### 共通-05 予期しないサーバエラー時の情報露出
- 目的: 500が起きても内部情報（stack trace等）を返さない
- 期待:
  - `500 Internal Server Error`
  - `error.code = INTERNAL_SERVER_ERROR`（返している場合）
  - 例外クラス名/SQL/スタックトレースがレスポンスに含まれない

---

## 2. 本の登録（POST /api/v1/books）

### 登録-01 正常系（最小入力）
- 入力: `{"title":"リーダブルコード","author":"Dustin Boswell"}`
- 期待:
  - `201 Created`
  - `bookId` が数値で払い出される（1以上。0は発行されない）
  - `status` は常に `AVAILABLE`

### 登録-02 バリデーション（空文字）
- 入力: `{"title":"","author":""}`
- 期待:
  - `400 Bad Request`
  - `error.code = VALIDATION_ERROR`
  - `error.details.fields` に `title`/`author` が含まれる

### 登録-03 バリデーション（キー欠落）
- 入力:
  - `{"author":"a"}`
  - `{"title":"t"}`
- 期待:
  - `400 Bad Request`
  - `error.code = VALIDATION_ERROR`
  - `error.details.fields` に欠落した項目（`title`/`author`）が含まれる

### 登録-04 型不正
- 入力:
  - `{"title":123,"author":"a"}`
  - `{"title":"t","author":true}`
- 期待:
  - `201 Created`（現状: 数値/booleanは文字列へ変換されて受理される）
  - レスポンス例（代表）:
    - `title` は `"123"` として扱われる
    - `author` は `"true"` として扱われる

### 登録-05 文字数/文字種境界（任意）
- 入力:
  - 非ASCII（日本語）
  - 長い文字列（例: 1,000文字）
- 期待:
  - 受理される/拒否されるの「現状挙動」を記録（仕様が無ければ将来仕様化候補）

---

## 3. 一覧取得（GET /api/v1/books）

### 一覧-01 正常系（空配列）
- 前提: 未登録
- 期待:
  - `200 OK`
  - `[]`

### 一覧-02 正常系（複数件）
- 前提: 複数冊登録
- 期待:
  - `200 OK`
  - JSON配列で複数件

### 一覧-03 status絞り込み（AVAILABLE）
- 前提: AVAILABLE/LOANED混在
- 入力: `GET /api/v1/books?status=AVAILABLE`
- 期待:
  - `200 OK`
  - 全件の `status` が `AVAILABLE`

### 一覧-04 status絞り込み（LOANED）
- 入力: `GET /api/v1/books?status=LOANED`
- 期待:
  - `200 OK`
  - 全件の `status` が `LOANED`

### 一覧-05 status不正
- 入力:
  - `status=INVALID`
  - `status=available`（大小文字違い）
- 期待:
  - `status=INVALID` / `status=available` の場合:
    - `400 Bad Request`
    - `error.code = INVALID_PARAMETER`
    - `error.details` に `name=status`, `value=<入力値>`, `expectedType=BookStatus` が含まれる
  - `status=`（空）の場合（現状）:
    - 未指定扱いとなり `200 OK`

### 一覧-06 クエリの重複
- 入力: `?status=AVAILABLE&status=LOANED`
- 期待:
  - どちらを採用するか/400にするか、現状挙動を記録

---

## 4. 貸出（POST /api/v1/books/{bookId}/lend）

### 貸出-01 正常系（AVAILABLE→LOANED）
- 前提: `status=AVAILABLE` の本が存在
- 期待:
  - `200 OK`
  - `status = LOANED`

### 貸出-02 ルール違反（LOANEDを再貸出）
- 前提: すでに `LOANED`
- 期待:
  - `409 Conflict`
  - `error.code = BOOK_ALREADY_LOANED`

### 貸出-03 未存在bookId
- 入力: 存在しないID
- 期待:
  - `404 Not Found`
  - `error.code = BOOK_NOT_FOUND`

### 貸出-04 bookId型不正
- 入力: `/api/v1/books/abc/lend`
- 期待:
  - `400 Bad Request`
  - `error.code = INVALID_PARAMETER`

### 貸出-05 bookId境界
- 入力: `/api/v1/books/0/lend`、`/api/v1/books/-1/lend`
- 期待:
  - `404 Not Found`
  - `error.code = BOOK_NOT_FOUND`

---

## 5. 返却（POST /api/v1/books/{bookId}/return）

### 返却-01 正常系（LOANED→AVAILABLE）
- 前提: `status=LOANED` の本が存在
- 期待:
  - `200 OK`
  - `status = AVAILABLE`

### 返却-02 ルール違反（AVAILABLEを再返却）
- 前提: すでに `AVAILABLE`
- 期待:
  - `409 Conflict`
  - `error.code = BOOK_NOT_LOANED`

### 返却-03 未存在bookId
- 期待:
  - `404 Not Found`
  - `error.code = BOOK_NOT_FOUND`

### 返却-04 bookId型不正
- 入力: `/api/v1/books/abc/return`
- 期待:
  - `400 Bad Request`
  - `error.code = INVALID_PARAMETER`

### 返却-05 bookId境界
- 入力: `/api/v1/books/0/return`、`/api/v1/books/-1/return`
- 期待:
  - `404 Not Found`
  - `error.code = BOOK_NOT_FOUND`

---

## 6. 競合（同時操作）

### 競合-01 同時貸出（2リクエストをほぼ同時に実行）
- 前提: 同じ `bookId` を対象に2つのクライアントで同時に `lend` を実行
- 期待（代表例）:
  - 片方: `200 OK`（LOANED）
  - もう片方: `409 Conflict`
    - `error.code = BOOK_CONFLICT`（更新競合として）または `BOOK_ALREADY_LOANED`（先にLOANEDが反映されて見える場合）

### 競合-02 同時返却（2リクエストをほぼ同時に実行）
- 前提: `LOANED` にしてから、同時に `return` を実行
- 期待:
  - 片方: `200 OK`（AVAILABLE）
  - もう片方: `409 Conflict`（`BOOK_CONFLICT` または `BOOK_NOT_LOANED`）

※ 厳密に再現するには2ターミナル同時実行、もしくは負荷ツール（JMeter等）や並列実行機能を使う。

---

## 7. 観測ポイント（記録しておくと良いもの）
- 返却/貸出/登録のレスポンスJSONが仕様どおり（フィールド名、型、enum文字列）
- エラー時に `error.code` が一貫していること
- `details` の内容（bookIdやfields）が期待どおり含まれること
- 仕様で未定な挙動（重複クエリ等）は「現状仕様」としてログに残す
