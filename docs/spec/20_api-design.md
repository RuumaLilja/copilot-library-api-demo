# 図書貸出管理API（REST）設計メモ

## 前提
- 画面なし、REST APIのみ
- 送受信は JSON（`Content-Type: application/json`）
- ここでの「本」は社内共有図書を指す
- 貸出状態は以下の2値
  - `AVAILABLE`：貸出可能
  - `LOANED`：貸出中

> 注: 要件にないため「借りた人」「貸出日」などは本設計では扱いません（必要なら拡張）。

---

## データとして持つ項目（本）

### Book
| 項目     |                      型 | 必須  | 説明                                   |
| -------- | ----------------------: | :---: | -------------------------------------- |
| `bookId` |                  number |  Yes  | 書籍ID（サーバ採番、システム内で一意） |
| `title`  |                  string |  Yes  | タイトル                               |
| `author` |                  string |  Yes  | 著者名                                 |
| `status` | `AVAILABLE` \| `LOANED` |  Yes  | 貸出状態                               |

### 状態遷移
- `AVAILABLE` → `LOANED` : 貸出
- `LOANED` → `AVAILABLE` : 返却

---

## API一覧

### 1) 本の登録
- **POST** `/api/v1/books`

**Request body**
```json
{
  "title": "リーダブルコード",
  "author": "Dustin Boswell"
}
```

- `bookId` はサーバ側で採番して払い出す
- 登録直後の `status` は常に `AVAILABLE`

**Responses**
- `201 Created`
```json
{
  "bookId": 1,
  "title": "リーダブルコード",
  "author": "Dustin Boswell",
  "status": "AVAILABLE"
}
```
- `400 Bad Request`（必須項目不足、JSON不正など）
  - `title` / `author` が空、またはキー欠落: `VALIDATION_ERROR`
  - JSON構文不正（パース不可）など: `MALFORMED_JSON`
  - ※ 現状実装では `title`/`author` が数値/booleanでも文字列へ変換され、`201` で受理される（厳密な型チェックはしていない）

---

### 2) 本の一覧取得（貸出状態で絞り込み可能）
- **GET** `/api/v1/books`

**Query parameters**
| 名前     |     型 | 必須  | 例          | 説明                               |
| -------- | -----: | :---: | ----------- | ---------------------------------- |
| `status` | string |  No   | `AVAILABLE` | 貸出状態で絞り込み（未指定は全件） |

**Response**
- `200 OK`
```json
[
  {
    "bookId": 1,
    "title": "リーダブルコード",
    "author": "Dustin Boswell",
    "status": "AVAILABLE"
  },
  {
    "bookId": 2,
    "title": "Clean Architecture",
    "author": "Robert C. Martin",
    "status": "LOANED"
  }
]
```

- `400 Bad Request`（`status` が `AVAILABLE`/`LOANED` 以外など）
  - ※ `status=`（空）は未指定扱いとなり `200 OK`（全件）

---

### 3) 貸出（貸出可能な本のみ）
- **POST** `/api/v1/books/{bookId}/lend`

**Path parameters**
| 名前     |     型 | 必須  | 説明   |
| -------- | -----: | :---: | ------ |
| `bookId` | number |  Yes  | 書籍ID |

**Request body**
- なし（要件に借り手情報がないため）

**Responses**
- `200 OK`
```json
{
  "bookId": 1,
  "title": "リーダブルコード",
  "author": "Dustin Boswell",
  "status": "LOANED"
}
```
- `404 Not Found`（対象の本が存在しない）
- `409 Conflict`（すでに `LOANED` のため貸出不可）

---

### 4) 返却（貸出中の本のみ）
- **POST** `/api/v1/books/{bookId}/return`

**Path parameters**
| 名前     |     型 | 必須  | 説明   |
| -------- | -----: | :---: | ------ |
| `bookId` | number |  Yes  | 書籍ID |

**Request body**
- なし

**Responses**
- `200 OK`
```json
{
  "bookId": 1,
  "title": "リーダブルコード",
  "author": "Dustin Boswell",
  "status": "AVAILABLE"
}
```
- `404 Not Found`（対象の本が存在しない）
- `409 Conflict`（すでに `AVAILABLE` のため返却不可）

---

## エラー応答フォーマット（例）
要件には形式指定がないため、本サンプル実装では以下の形式で統一します。

```json
{
  "error": {
    "code": "BOOK_ALREADY_LOANED",
    "message": "貸出中のため貸出できません",
    "details": {
      "bookId": 1
    }
  }
}
```

- `code` は機械判定用、`message` は表示/ログ用

---

## 実装上の注意（要件を満たすための制約）
- 貸出は **現在の `status` が `AVAILABLE` の場合のみ** 成功させる
- 返却は **現在の `status` が `LOANED` の場合のみ** 成功させる
- 競合（同時貸出など）を避けるため、更新は原子的（トランザクション/楽観ロック等）に扱う
