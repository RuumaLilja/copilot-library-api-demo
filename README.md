# copilot-library-api-demo

Copilot を使って、要件整理 → API設計 → 実装 → テスト までを  
小さな単位で進める流れを再現できるようにした、図書貸出管理 REST API のデモです。  
画面は持たず、成果物と進め方をリポジトリ上で追える構成にしています。

## What this repository shows

- 要件から API を整理する流れ
- md で仕様を固定してから実装する流れ
- 小さく実装して差分確認する流れ
- テスト観点を先に整理してから JUnit に落とす流れ

## Structure

- `app/` : Spring Boot (Maven) アプリ
- `docs/` : 要件 / API設計 / 実装方針 / テストケース / デモ手順 / AIログ
- `slides/` : 発表スライド置き場

## Requirements

- Java 17
- Maven

## Run

```bash
cd app
mvn spring-boot:run
```

起動時に `app/src/main/resources/data.sql` を自動実行し、  
H2 にデモ用データを投入します（テスト実行時は無効化）。

- API base URL: `http://localhost:8080/api/v1/books`
- H2 Console: `http://localhost:8080/h2-console`

## Example APIs

- `POST /api/v1/books`
- `GET /api/v1/books`
- `GET /api/v1/books?status=AVAILABLE`
- `POST /api/v1/books/{bookId}/lend`
- `POST /api/v1/books/{bookId}/return`

## Test

```
cd app
mvn test
```

## Docs

- Docs index: docs/README.md

特に最初に見る想定の資料:

- Spec index: docs/spec/README.md
- Demo guide: docs/demo-guides/README.md
- AI logs: docs/ai-logs/README.md

## Notes

- このリポジトリは「完成品の大規模サンプル」ではなく、AI を使った進め方のデモを目的としています。
- AIログは公開向けに整形し、個人情報やローカル環境依存情報を除去しています。
