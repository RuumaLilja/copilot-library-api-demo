# 図書貸出管理API（Spring Boot）実装TODO

対象仕様: [20_api-design.md](20_api-design.md) / [30_springboot-implementation-plan.md](30_springboot-implementation-plan.md)

---

## 0. 実装順の考え方（最短で動かす）
- 先に「データを保存して読み出せる」状態（H2 + JPA）を作る
- 次に「ユースケース（貸出/返却のルール）」を Service で固める
- 最後に「HTTP（Controller）」を薄く載せる
- テストは **Service中心**（ルールの網羅が価値大） + Controllerは最低限

---

## 1. プロジェクトセットアップ（Spring Boot）

- [x] Javaバージョンを決める（Java 17）
- [x] ビルドツールを決める（Maven）
- [x] Spring Initializr 相当の構成でプロジェクト作成
  - [x] Group/Artifact、パッケージ名を決定（`com.example.library`）
  - [x] Dependencies を追加
    - [x] Spring Web
    - [x] Spring Data JPA
    - [x] H2 Database
    - [x] Validation
    - [x] Spring Boot Test

---

## 2. アプリ設定（H2 / JPA）

- [x] `application.yml` を作成
  - [x] H2接続設定（インメモリ）
  - [x] JPA設定（デモ向け: `ddl-auto=create-drop`）
  - [x] SQLログ出力（`show-sql: true`）
- [x] H2 Console を有効化（`/h2-console`）

動作確認（この時点のゴール）
- [x] アプリが起動する（`mvn test` の `contextLoads` で起動を確認）
- [x] H2に接続できる（Consoleを有効化した場合）
  - [x] `http://localhost:8080/h2-console` にHTTPで到達できる（302で `/h2-console/` にリダイレクト）

---

## 3. ドメイン定義（最小）

- [x] `BookStatus` enum を作成
  - [x] `AVAILABLE`
  - [x] `LOANED`
- [x] ルール（貸出/返却可否）をどこに置くか決める
  - [x] 方針: まずは Service 内で状態チェック（最短）
  - [ ] 余力があれば Domain（`Book`）に `lend()` / `returnBook()` を寄せる

---

## 4. 永続化（JPA Entity / Repository）

- [x] `BookEntity` を作成
  - [x] `bookId: Long`（PK、サーバ採番）
    - [x] シーケンス採番（H2対応）
  - [x] `title: String`
  - [x] `author: String`
  - [x] `status: BookStatus`
  - [x] `version: Long`（`@Version`：楽観ロック）
- [x] `BookRepository extends JpaRepository<BookEntity, Long>` を作成
  - [x] `findByStatus(BookStatus status)` を追加

動作確認（この時点のゴール）
- [x] `@DataJpaTest` で保存→取得ができる

---

## 5. アプリケーション層（Service：ユースケース実装）

- [x] `BookService` を作成（`@Service`）
- [x] 追加するメソッド
  - [x] `createBook(title, author)`
    - [x] 登録時 `status` は常に `AVAILABLE`
    - [x] `bookId` はDB採番
  - [x] `listBooks(Optional<BookStatus> status)`
    - [x] `status` 指定時は絞り込み
  - [x] `lend(bookId)`
    - [x] 対象が存在しなければ `404` 相当
    - [x] `AVAILABLE` のときだけ `LOANED` へ更新
    - [x] それ以外は `409` 相当
  - [x] `returnBook(bookId)`
    - [x] 対象が存在しなければ `404` 相当
    - [x] `LOANED` のときだけ `AVAILABLE` へ更新
    - [x] それ以外は `409` 相当
- [x] トランザクションを付与
  - [x] `lend` / `returnBook` は `@Transactional`

---

## 6. 例外設計（HTTPに変換しやすくする）

- [x] 例外クラスを用意（最低限）
  - [x] `BookNotFoundException`（404）
  - [x] `BookAlreadyLoanedException`（409）
  - [x] `BookNotLoanedException`（409）
  - [x] 競合（楽観ロック）用の扱い（409に寄せる）
- [x] APIエラー形式（`ApiError`）を決める
- [x] `@RestControllerAdvice` で例外→HTTPレスポンス変換

---

## 7. Web/API層（Controller / DTO）

- [x] DTOを作成
  - [x] `CreateBookRequest`（`title`, `author`）
    - [x] Bean Validation（`@NotBlank`）
  - [x] `BookResponse`（`bookId`, `title`, `author`, `status`）
- [x] `BookController` を作成
  - [x] `POST /api/v1/books`
  - [x] `GET /api/v1/books?status=AVAILABLE|LOANED`
  - [x] `POST /api/v1/books/{bookId}/lend`
  - [x] `POST /api/v1/books/{bookId}/return`

動作確認（この時点のゴール）
- [ ] curl/Postman で登録→一覧→貸出→返却が一通り動く

---

## 8. JUnit（テスト作成）

### 8.1 Serviceテスト（優先度: 高）
- [x] `@SpringBootTest` もしくは `@DataJpaTest` + Service構築でテスト方針を決める
- [x] `createBook`
  - [x] `status` が必ず `AVAILABLE`
  - [x] `bookId` が採番される
- [x] `lend`
  - [x] `AVAILABLE` → `LOANED` に遷移できる
  - [x] すでに `LOANED` は 409相当になる
  - [x] 存在しない `bookId` は 404相当になる
- [x] `returnBook`
  - [x] `LOANED` → `AVAILABLE` に遷移できる
  - [x] すでに `AVAILABLE` は 409相当になる
  - [x] 存在しない `bookId` は 404相当になる
- [x] `listBooks`
  - [x] フィルタなしは全件
  - [x] `status` で絞り込める

### 8.2 Controllerテスト（優先度: 中）
- [x] `@WebMvcTest` でHTTP層の最低限を確認
  - [x] `POST /books` が 201 とJSONを返す
  - [x] `GET /books?status=...` の `status` 不正で 400
  - [x] 例外が `ApiError` 形式で返る

---

## 9. 最終チェック（デモ運用）

- [x] 起動手順をREADMEに1〜2行で残す（任意）
- [x] デモ用の操作手順（curl例）を docs に追記（任意）
- [x] 競合（同時貸出）時の挙動（409）を説明できる状態にする
