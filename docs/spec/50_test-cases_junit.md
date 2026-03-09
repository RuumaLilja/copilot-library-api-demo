# 図書貸出管理API JUnitテストケース一覧

対象: 図書貸出管理API（登録/一覧/貸出/返却）

参照仕様:
- [10_requirements.md](10_requirements.md)
- [20_api-design.md](20_api-design.md)
- 実装: `BookController` / `BookService` / `GlobalExceptionHandler` / `BookRepository`

---

## 1. 目的
- 要件のコア（貸出/返却ルール、存在チェック、エラー変換）をテストで固定化する
- バリデーション・不正パラメータ・競合（楽観ロック）の振る舞いを明示する

---

## 2. 優先度の定義
- 高: 要件のビジネスルール/状態遷移に直結し、バグが出るとAPIが成立しないもの（Service中心）
- 中: HTTP層の契約（ステータスコード、JSON形、例外→エラー応答）が崩れると利用者影響が大きいもの（Controller中心）
- 低: 永続化の細部、フレームワーク設定、運用上の安全性・回帰防止（Repository/設定/ログ等）

---

## 3. テスト方針（推奨）
- Service（高）: DBを使った統合テストを推奨
  - 方式A: `@DataJpaTest` + `@Import(BookService.class)`（軽量・高速）
  - 方式B: `@SpringBootTest`（構成を広く確認したい場合）
- Controller（中）: `@WebMvcTest(BookController.class)` + `MockMvc`
  - `BookService` は `@MockBean` で契約（HTTP/例外変換/Validation）を確認
- Repository（低）: `@DataJpaTest`

※ 例外→エラー応答は `GlobalExceptionHandler` を `@Import` して確認する。

---

## 4. テストケース一覧（高）Service（ユースケース/ルール）

### 高-SVC-01 createBook: 登録直後は常に AVAILABLE
- 前提: title/author を指定
- 実行: `createBook(title, author)`
- 期待:
  - `bookId` が採番されている（1以上）
  - `status == AVAILABLE`
  - `title/author` が保存されている

### 高-SVC-02 listBooks: フィルタなしは全件かつ安定順序
- 前提: 複数冊登録（bookIdが異なる）
- 実行: `listBooks(Optional.empty())`
- 期待:
  - 全件が返る
  - `bookId` 昇順（実装が `Sort.by("bookId")` のため）

### 高-SVC-03 listBooks: status絞り込みが効く
- 前提: AVAILABLE と LOANED を混在させて保存
- 実行: `listBooks(Optional.of(AVAILABLE))` / `listBooks(Optional.of(LOANED))`
- 期待:
  - 指定 status のみ返る

### 高-SVC-04 lend: AVAILABLE → LOANED に遷移できる
- 前提: status=AVAILABLE の本が存在
- 実行: `lend(bookId)`
- 期待:
  - 返り値 `status == LOANED`
  - DB上も LOANED に更新されている

### 高-SVC-05 lend: LOANED は 409相当（BookAlreadyLoanedException）
- 前提: status=LOANED の本が存在
- 実行: `lend(bookId)`
- 期待:
  - `BookAlreadyLoanedException`
  - DB上の status が変化しない

### 高-SVC-06 lend: 存在しない bookId は 404相当（BookNotFoundException）
- 前提: 対象IDが存在しない
- 実行: `lend(bookId)`
- 期待: `BookNotFoundException(bookId)`

### 高-SVC-07 returnBook: LOANED → AVAILABLE に遷移できる
- 前提: status=LOANED の本が存在
- 実行: `returnBook(bookId)`
- 期待:
  - 返り値 `status == AVAILABLE`
  - DB上も AVAILABLE に更新

### 高-SVC-08 returnBook: AVAILABLE は 409相当（BookNotLoanedException）
- 前提: status=AVAILABLE の本が存在
- 実行: `returnBook(bookId)`
- 期待:
  - `BookNotLoanedException`
  - DB上の status が変化しない

### 高-SVC-09 returnBook: 存在しない bookId は 404相当（BookNotFoundException）
- 前提: 対象IDが存在しない
- 実行: `returnBook(bookId)`
- 期待: `BookNotFoundException(bookId)`

### 高-SVC-10 競合（楽観ロック）: 同一bookを同時更新すると競合を検知できる
- 目的: `@Version` による競合検知が機能すること
- 推奨実装アプローチ例:
  - 同じ `bookId` を2つの別トランザクションで読み出す
  - 片方で `lend()` をコミット
  - もう片方で更新（`lend()`/`returnBook()` 相当の更新）を試みて `OptimisticLockingFailureException`（もしくはJPA例外）を期待
- 期待: 競合例外が発生し、更新が取り消される

---

## 5. テストケース一覧（中）Controller（HTTP契約/例外→エラー応答）

### 中-CTL-01 POST /api/v1/books: 201 Created + レスポンス形
- 前提: `BookService.createBook` をモックして `Book`（ドメインモデル）を返す
- 実行: JSON（title, author）をPOST
- 期待:
  - HTTP 201
  - JSONに `bookId,title,author,status` がある

### 中-CTL-02 GET /api/v1/books: 200 OK + 配列
- 前提: `BookService.listBooks` をモックして複数件返す
- 実行: GET
- 期待:
  - HTTP 200
  - JSON配列で返る

### 中-CTL-03 GET /api/v1/books?status=AVAILABLE|LOANED: status指定がServiceへ渡る
- 前提: `BookService.listBooks(Optional.of(status))` が呼ばれることを verify
- 実行: GET with query
- 期待:
  - HTTP 200
  - `BookService` が期待引数で呼ばれる

### 中-CTL-04 GET /api/v1/books?status=不正: 400 + ApiErrorResponse
- 実行: `status=INVALID` 等
- 期待:
  - HTTP 400
  - `error.code == INVALID_PARAMETER`

### 中-CTL-05 POST /api/v1/books: Validation不正（空/未指定）で 400
- 実行: `title`/`author` が空や欠落
- 期待:
  - HTTP 400
  - `error.code == VALIDATION_ERROR`
  - `error.details.fields.title` / `author` が含まれる（メッセージは実装依存）

### 中-CTL-06 POST /api/v1/books: 不正JSONで 400
- 実行: JSONが壊れている
- 期待:
  - HTTP 400
  - `error.code == MALFORMED_JSON`

### 中-CTL-07 POST /api/v1/books/{bookId}/lend: 成功で 200
- 前提: `BookService.lend` が `Book(status=LOANED)` を返す
- 実行: POST
- 期待:
  - HTTP 200
  - `status == LOANED`

### 中-CTL-08 POST /api/v1/books/{bookId}/return: 成功で 200
- 前提: `BookService.returnBook` が `Book(status=AVAILABLE)` を返す
- 実行: POST
- 期待:
  - HTTP 200
  - `status == AVAILABLE`

### 中-CTL-09 lend: 存在しないIDは 404 + BOOK_NOT_FOUND
- 前提: `BookService.lend` が `BookNotFoundException` を投げる
- 実行: POST
- 期待:
  - HTTP 404
  - `error.code == BOOK_NOT_FOUND`
  - `error.details.bookId == {bookId}`

### 中-CTL-10 lend: 貸出中は 409 + BOOK_ALREADY_LOANED
- 前提: `BookService.lend` が `BookAlreadyLoanedException` を投げる
- 期待:
  - HTTP 409
  - `error.code == BOOK_ALREADY_LOANED`

### 中-CTL-11 return: 未貸出は 409 + BOOK_NOT_LOANED
- 前提: `BookService.returnBook` が `BookNotLoanedException` を投げる
- 期待:
  - HTTP 409
  - `error.code == BOOK_NOT_LOANED`

### 中-CTL-12 bookIdが数値でない: 400 + INVALID_PARAMETER
- 実行: `/api/v1/books/abc/lend` 等
- 期待:
  - HTTP 400
  - `error.code == INVALID_PARAMETER`

### 中-CTL-13 競合（OptimisticLockingFailureException）: 409 + BOOK_CONFLICT
- 前提: Service層が `OptimisticLockingFailureException` を投げる
- 期待:
  - HTTP 409
  - `error.code == BOOK_CONFLICT`

---

## 6. テストケース一覧（低）Repository / 設定 / 回帰防止

### 低-REP-01 findByStatus: 指定statusのみ返る
- 前提: AVAILABLE/LOANED を混在保存
- 実行: `findByStatus(AVAILABLE)`
- 期待: AVAILABLEのみ

### 低-REP-02 @Version: 更新で version が進む
- 前提: entityを保存
- 実行: 更新→再取得
- 期待: `version` が増える（厳密な値は実装依存なので「変化した」を見る）

### 低-REP-03 null制約: title/author/status がnullで保存できない
- 実行: nullをセットして保存/flush
- 期待: 例外（DataIntegrityViolationException等）

### 低-APP-01 contextLoads: Spring起動確認（既存）
- 目的: 依存関係/設定の回帰検知

### 低-ERR-01 想定外RuntimeException: 500 + INTERNAL_SERVER_ERROR（情報露出しない）
- 前提: Controller経由で `RuntimeException` を投げるモック
- 期待:
  - HTTP 500
  - `error.code == INTERNAL_SERVER_ERROR`
  - stack trace / 内部例外メッセージをレスポンスに含めない

---

## 7. 既存テストとギャップ
- 既存:
  - Repositoryの基本永続化（BookRepositoryTest）
  - Web層の400系（BookControllerWebMvcTest）
  - Service層のルール網羅（BookServiceTest）
- ギャップ（最優先で追加推奨）:
  - Controllerの成功系（201/200）・404/409・競合409は実装済みのため、残りは低優先度（Repository細部、500の情報露出防止など）
