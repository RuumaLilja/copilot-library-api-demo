# 図書貸出管理API デモ操作手順（curl）

前提:
- アプリ起動: `mvn spring-boot:run`
- URL（デフォルト）: `http://localhost:8080`

補足（初期データ）:
- 起動時に `data.sql` が自動実行され、最初から本が数件登録されています（例: `bookId=1` は `Readable Code`）。
- インメモリDBのため、アプリを再起動すると初期状態に戻ります。

---

## 1) 本の登録（POST /api/v1/books）

```bash
curl -i -X POST "http://localhost:8080/api/v1/books" \
  -H "Content-Type: application/json" \
  -d '{"title":"リーダブルコード","author":"Dustin Boswell"}'
```

期待:
- `201 Created`
- `status` は常に `AVAILABLE`

---

## 2) 一覧取得（GET /api/v1/books）

```bash
curl -i "http://localhost:8080/api/v1/books"
```

期待:
- `200 OK`
- JSON配列

---

## 3) 状態で絞り込み（GET /api/v1/books?status=...）

```bash
curl -i "http://localhost:8080/api/v1/books?status=AVAILABLE"
curl -i "http://localhost:8080/api/v1/books?status=LOANED"
```

期待:
- `200 OK`

### status不正（400）

```bash
curl -i "http://localhost:8080/api/v1/books?status=INVALID"
```

期待:
- `400 Bad Request`
- `error.code = INVALID_PARAMETER`

---

## 4) 貸出（POST /api/v1/books/{bookId}/lend）

```bash
curl -i -X POST "http://localhost:8080/api/v1/books/1/lend"
```

期待:
- `200 OK`
- `status = LOANED`

### 貸出中に再貸出（409）

```bash
curl -i -X POST "http://localhost:8080/api/v1/books/1/lend"
```

期待:
- `409 Conflict`
- `error.code = BOOK_ALREADY_LOANED`

### 存在しないbookId（404）

```bash
curl -i -X POST "http://localhost:8080/api/v1/books/9999/lend"
```

期待:
- `404 Not Found`
- `error.code = BOOK_NOT_FOUND`

---

## 5) 返却（POST /api/v1/books/{bookId}/return）

```bash
curl -i -X POST "http://localhost:8080/api/v1/books/1/return"
```

期待:
- `200 OK`
- `status = AVAILABLE`

### 返却済みに再返却（409）

```bash
curl -i -X POST "http://localhost:8080/api/v1/books/1/return"
```

期待:
- `409 Conflict`
- `error.code = BOOK_NOT_LOANED`

---

## 6) 競合（同時貸出など）の挙動（409）

実装は JPA の `@Version` による楽観ロックで競合を検知します。
2つのリクエストが「同じ本を同時に貸出/返却」しようとすると、どちらか片方が先に更新し、後から更新しようとした側が競合として扱われます。

競合時の期待:
- `409 Conflict`
- エラー形式: `ApiErrorResponse`
- `error.code = BOOK_CONFLICT`

レスポンス例:

```json
{
  "error": {
    "code": "BOOK_CONFLICT",
    "message": "他の更新と競合しました。再試行してください",
    "details": {}
  }
}
```

補足:
- curl単体で「完全に同時」を再現するのは難しいため、負荷ツールや2ターミナルで同時実行、またはデバッガでタイミングを揃えるのが確実です。
