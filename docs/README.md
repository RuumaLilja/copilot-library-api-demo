この `docs/` は、設計資料・デモ手順・公開用に整形した AI ログの置き場です。  
「設計 → 実装 → テスト」の流れを、ドキュメントから追えるようにしています。

## Start here

- Spec（要件 → API設計 → 実装方針 → テスト）: [spec/README.md](spec/README.md)
- Demo guide（curl手順）: [demo-guides/README.md](demo-guides/README.md)
- AI logs: [ai-logs/README.md](ai-logs/README.md)

## 読む順番

1. [spec/10_requirements.md](spec/10_requirements.md)
2. [spec/20_api-design.md](spec/20_api-design.md)
3. [spec/30_springboot-implementation-plan.md](spec/30_springboot-implementation-plan.md)
4. [spec/40_implementation-todo.md](spec/40_implementation-todo.md)
5. [spec/50_test-cases_junit.md](spec/50_test-cases_junit.md)
6. [spec/51_test-cases_blackbox.md](spec/51_test-cases_blackbox.md)
7. [demo-guides/デモ操作手順\_curl.md](demo-guides/%E3%83%87%E3%83%A2%E6%93%8D%E4%BD%9C%E6%89%8B%E9%A0%86_curl.md)

## 旧ドキュメント名との対応（会話ログ用）

会話ログ（[ai-logs](ai-logs)）では、作業当時のドキュメント名が登場します。  
公開用に `docs/spec/` を「番号 + ASCII ファイル名」に整理しているため、読み替え用の対応表を載せています。

| 会話ログに出てくる名前                          | 現在の公開用ドキュメント                                                                                        |
| ----------------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| 要件.md                                         | [spec/10_requirements.md](spec/10_requirements.md)                                                              |
| API設計.md                                      | [spec/20_api-design.md](spec/20_api-design.md)                                                                  |
| SpringBoot実装案.md / SpringBoot実装案\_ver1.md | [spec/30_springboot-implementation-plan.md](spec/30_springboot-implementation-plan.md)                          |
| 実装TODO.md                                     | [spec/40_implementation-todo.md](spec/40_implementation-todo.md)                                                |
| JUnitテストケース一覧.md                        | [spec/50_test-cases_junit.md](spec/50_test-cases_junit.md)                                                      |
| ブラックボックステストケース一覧.md             | [spec/51_test-cases_blackbox.md](spec/51_test-cases_blackbox.md)                                                |
| デモ操作手順\_curl.md                           | [demo-guides/デモ操作手順\_curl.md](demo-guides/%E3%83%87%E3%83%A2%E6%93%8D%E4%BD%9C%E6%89%8B%E9%A0%86_curl.md) |

## 補足

- 公開用 AI ログは、ローカルパスや不要な内部ログを除去した上で掲載しています。
- ドキュメント名は公開時に整理していますが、内容の対応関係は上記表の通りです。
