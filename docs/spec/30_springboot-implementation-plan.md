# 図書貸出管理API（Spring Boot）実装案

## 目的
- [20_api-design.md](20_api-design.md) のREST APIを Spring Boot で実装する際の、責務分離（レイヤ設計）とパッケージ構成のたたき台
- 最小構成で要件を満たしつつ、拡張（借り手情報・履歴・認可など）を後から足しやすくする

---

## 責務分離案（レイヤ）

### 1) Presentation（Web/API）層
**責務**
- HTTPリクエスト/レスポンス（JSON）
- 入力バリデーション（Bean Validation）
- ステータスコードの確定
- DTO ↔ ドメインモデル変換

**主な要素**
- `@RestController`
- Request/Response DTO
- `@ControllerAdvice`（例外→エラー応答）

**持たないもの（NG）**
- 永続化の詳細（JPA操作）
- ビジネスルール（貸出可否判定など）の実装

---

### 2) Application（ユースケース）層 / Service
**責務**
- ユースケースのオーケストレーション
  - 本登録
  - 一覧取得（フィルタ）
  - 貸出
  - 返却
- トランザクション境界
- 競合時の扱い（貸出/返却の排他・整合性）

**主な要素**
- `@Service`
- `@Transactional`

---

### 3) Domain（ドメイン）層
**責務**
- ルールの中心（状態遷移、禁止ルール）
  - `AVAILABLE` のみ貸出可能
  - `LOANED` のみ返却可能

**主な要素**
- `BookStatus`（enum）
- （必要なら）ドメインメソッド `lend()` / `returnBook()`

---

### 4) Infrastructure（永続化）層
**責務**
- DBとのやり取り（JPA/SQL）
- 楽観ロック/排他の仕組み

**主な要素**
- `Spring Data JPA` の `Repository`
- `@Entity`

---

## ディレクトリ（パッケージ）構成案

### 推奨: パッケージは機能単位 + レイヤ
要件が小さくても、機能別にまとめると増やしやすいです。

```
src/main/java/com/example/library
  Application.java
  book
    api
      BookController.java
      dto
        CreateBookRequest.java
        BookResponse.java
    application
      BookService.java
    domain
      Book.java                (ドメインモデル)
      BookStatus.java
    infra
      BookEntity.java          (@Entity)
      BookRepository.java      (Spring Data JPA)
  common
    error
      ApiError.java
      GlobalExceptionHandler.java
```

- `book/` 配下に「登録/一覧/貸出/返却」を閉じ込める
- `common/error` はAPI全体で共有するエラー形式・例外ハンドリング

> 注: 本プロジェクトは `Book.java`（ドメインモデル）と `BookEntity`（永続化）を分離する方針。
> Controller/Service が `BookEntity` に依存しないようにし、infra の詳細は Service 内に閉じ込めます。

---

## クラス設計（最小）

### `BookController`（API層）
**責務**
- エンドポイント定義とDTO変換

**エンドポイント（例）**
- `POST /api/v1/books`
- `GET /api/v1/books?status=...`
- `POST /api/v1/books/{bookId}/lend`
- `POST /api/v1/books/{bookId}/return`

---

### DTO（api/dto）
- `CreateBookRequest`
  - `title`（必須）
  - `author`（必須）
- `BookResponse`
  - `bookId`, `title`, `author`, `status`

**バリデーション例**
- `@NotBlank`（`title`, `author`）

**補足（方針反映）**
- `bookId` はサーバ採番のためリクエストでは受け取らない
- 登録時の `status` は常に `AVAILABLE` で開始する（リクエストでは受け取らない）

---

### `BookService`（application層）
**責務**
- トランザクション内で状態を読み、ルールに従い更新

**想定メソッド**
- `createBook(CreateBookCommand)`
- `listBooks(Optional<BookStatus> status)`
- `lend(long bookId)`
- `returnBook(long bookId)`

---

### 永続化（infra層）
- `BookEntity`
  - `bookId`（PK、サーバ採番）
  - `title`
  - `author`
  - `status`
  - `version`（楽観ロック用 `@Version` を推奨）

- `BookRepository extends JpaRepository<BookEntity, Long>`
  - `findByStatus(BookStatus status)`

**採番方式（方針反映）**
- DBシーケンスで採番する（H2でも動作）
- 実装例（イメージ）
  - `@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq")`
  - `@SequenceGenerator(name = "book_seq", sequenceName = "book_seq", allocationSize = 1)`

---

## 例外/エラー設計案

### 方針
- ルール違反は `409 Conflict`
  - 例: 貸出中に貸出、返却済み（AVAILABLE）に返却
- 存在しない `bookId` は `404 Not Found`
- 入力不正は `400 Bad Request`

### 例外クラス（例）
- `BookNotFoundException` → 404
- `BookAlreadyLoanedException` → 409
- `BookNotLoanedException` → 409
- `DuplicateBookIdException` → 409

`GlobalExceptionHandler`（`@RestControllerAdvice`）で `ApiError` に変換して返す。

---

## 競合（同時貸出）への考え方
- **最低限**: `@Transactional` + 楽観ロック（`@Version`）で更新競合を検知
- 競合検知時は `409 Conflict` として「先に他の人が操作した」扱いにする
- より強い排他が必要なら `PESSIMISTIC_WRITE` なども検討（ただしサンプルとしては過剰になりがち）

---

## テスト方針（任意）
- `BookService` を中心にユースケーステスト
  - 貸出可否/返却可否の境界条件
- `BookController` は `@WebMvcTest` でステータスコードとJSON形を軽く確認

---

## 次に決めると良いこと（実装開始前）
## 実装方針（決定）
- `bookId` は **サーバ採番**（DBシーケンス）
- 登録時の `status` は **常に `AVAILABLE`**（リクエストでは受け取らない）
- DBはデモ用途のため **H2**
- マイグレーション（Flyway/Liquibase）は **導入しない**

**補足（H2運用）**
- JPAでテーブル自動生成する前提なら `spring.jpa.hibernate.ddl-auto=create-drop`（デモ向け）などを採用
