# 個人財務管理系統 — 系統規格與資料庫設計

> **版本**：v1.1  
> **技術棧**：Spring Boot 3 · Spring Data JPA · PostgreSQL · Redis · JWT · RESTful API · Swagger · Nuxt 3 · Vue 3 · TypeScript  
> **難度**：⭐⭐⭐ 中等

---

## 目錄

1. [系統概述](#1-系統概述)
2. [功能需求規格](#2-功能需求規格)
3. [非功能需求](#3-非功能需求)
4. [API 設計概覽](#4-api-設計概覽)
5. [ER Model](#5-er-model)
6. [資料表定義](#6-資料表定義)
7. [Redis 快取策略](#7-redis-快取策略)
8. [安全設計](#8-安全設計)
9. [模組架構](#9-模組架構)
10. [開放問題與建議](#10-開放問題與建議)

---

## 1. 系統概述

個人財務管理系統（Personal Finance Manager，PFM）協助個人用戶記錄日常收支、設定預算目標、分析消費趨勢，並產生財務報表，目標是讓用戶對自身財務狀況有清晰的全貌。

自 v1.1 起，系統定義調整為前後端分離架構：後端提供 REST API，前端以 Nuxt 3 單頁應用（SPA）承接登入、儀表板、交易管理、預算管理與資料設定流程。

### 1.1 使用者角色

| 角色 | 說明 |
|------|------|
| **一般用戶（User）** | 系統主要使用者，管理自己的帳戶、交易與預算 |
| **管理員（Admin）** | 管理分類字典、系統設定（未來擴充） |

> 本期 v1.0 以**單一用戶**為核心設計，多用戶（家庭帳）列為 v2.0 擴充。

### 1.2 核心使用情境

```
用戶登入 → 建立資金帳戶（現金/銀行卡）
         → 新增交易（支出 / 收入 / 轉帳）
         → 設定月度預算（按類別）
         → 查看儀表板（本月收支摘要）
         → 檢視趨勢圖表（週/月/年）
         → 匯出 CSV / PDF 報告
```

### 1.3 系統邊界

```text
Nuxt 3 SPA（frontend/）
  ├─ 提供登入/註冊、儀表板、交易、預算、設定等操作介面
  ├─ 管理前端篩選狀態、表單輸入與視覺化結果
  └─ 透過 REST API 呼叫 Spring Boot 後端

Spring Boot API（src/main/java/...）
  ├─ 負責認證授權、商業邏輯、資料驗證
  ├─ 對 PostgreSQL 進行持久化
  └─ 使用 Redis 處理 token 黑名單與快取
```

---

## 2. 功能需求規格

### 2.1 用戶認證（Auth）

| # | 功能 | 說明 |
|---|------|------|
| A-01 | 註冊 | Email + 密碼；密碼 bcrypt 加密 |
| A-02 | 登入 | 回傳 Access Token（15 min）+ Refresh Token（7 day） |
| A-03 | 刷新 Token | 使用 Refresh Token 換取新 Access Token |
| A-04 | 登出 | 將 Refresh Token 加入 Redis 黑名單 |
| A-05 | 修改密碼 | 需驗證舊密碼 |

### 2.2 資金帳戶管理（Account）

| # | 功能 | 說明 |
|---|------|------|
| AC-01 | 建立帳戶 | 名稱、類型（現金/銀行/信用卡/電子錢包）、幣別、初始餘額 |
| AC-02 | 查詢帳戶列表 | 含各帳戶即時餘額 |
| AC-03 | 更新帳戶資訊 | 名稱、備註 |
| AC-04 | 封存帳戶 | 軟刪除，不刪歷史交易 |
| AC-05 | 帳戶餘額計算 | `初始餘額 + Σ收入 − Σ支出` （由交易推算，非直接儲存） |

> **設計決策**：餘額以**計算欄位**呈現（由交易聚合），避免資料不一致；若效能有瓶頸再改為 Redis 快取快照。

### 2.3 交易記錄（Transaction）

| # | 功能 | 說明 |
|---|------|------|
| T-01 | 新增交易 | 類型（INCOME / EXPENSE / TRANSFER）、金額、日期、分類、帳戶、備註、附件圖片（可選） |
| T-02 | 查詢交易 | 分頁、篩選（日期區間、類型、分類、帳戶）、關鍵字搜尋（備註） |
| T-03 | 更新交易 | 除類型外欄位均可修改 |
| T-04 | 刪除交易 | 軟刪除 |
| T-05 | 轉帳交易 | 來源帳戶 → 目標帳戶，自動產生兩筆關聯交易 |
| T-06 | 批次匯入 | 上傳 CSV，解析並寫入；失敗筆數回傳錯誤明細 |
| T-07 | 附件上傳 | 發票/收據圖片，儲存至本地或 S3（可配置） |

### 2.4 分類管理（Category）

| # | 功能 | 說明 |
|---|------|------|
| C-01 | 系統預設分類 | 食/衣/住/行/娛樂/醫療/薪資/投資⋯（種子資料） |
| C-02 | 自訂分類 | 用戶可新增子分類；支援二層（父/子）結構 |
| C-03 | 分類圖示 | icon code（使用 Material Icons 名稱） |
| C-04 | 刪除自訂分類 | 若有關聯交易則禁止刪除，需先重新分類 |

### 2.5 預算管理（Budget）

| # | 功能 | 說明 |
|---|------|------|
| B-01 | 設定月度預算 | 依分類設定當月上限金額 |
| B-02 | 預算執行率查詢 | 即時回傳已用金額 / 預算上限 |
| B-03 | 超支警告 | 達 80% 時標記 WARNING；達 100% 標記 EXCEEDED |
| B-04 | 預算複製 | 將上月預算複製至本月（快速設定） |
| B-05 | 年度預算總覽 | 12 個月各分類執行率熱力圖資料 |

### 2.6 報表與分析（Report）

| # | 功能 | 說明 |
|---|------|------|
| R-01 | 月度收支摘要 | 總收入、總支出、淨儲蓄、儲蓄率 |
| R-02 | 支出分類佔比 | 圓餅圖資料（按分類聚合） |
| R-03 | 趨勢折線圖 | 按週/月/年的收支走勢 |
| R-04 | 分類明細排行 | 當期各分類支出由多到少排序 |
| R-05 | 匯出 CSV | 交易明細依篩選條件匯出 |
| R-06 | 匯出 PDF | 月度財務報告（含圖表截圖） |

### 2.7 前端介面（Frontend）

| # | 功能 | 說明 |
|---|------|------|
| F-01 | 登入 / 註冊介面 | 提供登入與註冊切換，登入成功後保存 token 與偏好幣別 |
| F-02 | 儀表板總覽 | 顯示帳戶數、交易數、本期淨額、儲蓄率、分類占比與預算進度 |
| F-03 | 交易工作區 | 支援新增收入/支出、轉帳、分頁查詢、條件篩選、編輯與刪除 |
| F-04 | 預算工作區 | 支援月切換、建立/修改/刪除預算、顯示使用率與從前一期複製 |
| F-05 | 初始資料設定 | 支援建立帳戶、建立分類、匯入 CSV 交易資料 |
| F-06 | API 整合 | 所有資料操作均透過 `/api/v1/*` 完成，不直接存取資料庫 |
| F-07 | 執行模式 | 前端採 Nuxt 3 SPA，預設開發位址 `http://localhost:3000`，API Base 可由環境變數覆寫 |

---

## 3. 非功能需求

| 類別 | 指標 |
|------|------|
| **效能** | 一般 API 回應 < 300ms；報表聚合 < 1s（Redis 快取輔助） |
| **安全** | HTTPS only；JWT 短效 Token；密碼 bcrypt cost=12；防止 SQL Injection（JPA Parameterized Query） |
| **可用性** | 單節點 Side Project，無 HA 需求，但需優雅重啟 |
| **前端體驗** | 首屏需可於桌機與手機瀏覽；主要流程應在單頁內完成，避免依賴多頁跳轉 |
| **資料完整性** | 交易刪除為軟刪除；帳戶封存不影響歷史交易 |
| **擴充性** | 分類/幣別設計預留多語系欄位；帳戶預留多幣別換算介面 |
| **測試覆蓋** | Service 層 Unit Test ≥ 70%；Controller 層 Integration Test 覆蓋主要 API |

---

## 4. API 設計概覽

Base URL：`/api/v1`

### 認證

```
POST   /auth/register
POST   /auth/login
POST   /auth/refresh
POST   /auth/logout
PUT    /auth/password
```

### 帳戶

```
GET    /accounts
POST   /accounts
GET    /accounts/{id}
PUT    /accounts/{id}
DELETE /accounts/{id}          (軟刪除)
GET    /accounts/{id}/balance
```

### 交易

```
GET    /transactions            (分頁+篩選)
POST   /transactions
GET    /transactions/{id}
PUT    /transactions/{id}
DELETE /transactions/{id}
POST   /transactions/transfer
POST   /transactions/import     (CSV 上傳)
POST   /transactions/{id}/attachment
```

### 分類

```
GET    /categories
POST   /categories
PUT    /categories/{id}
DELETE /categories/{id}
```

### 預算

```
GET    /budgets?year=&month=
POST   /budgets
PUT    /budgets/{id}
DELETE /budgets/{id}
GET    /budgets/status?year=&month=   (執行率+警告)
POST   /budgets/copy?from=&to=
```

### 報表

```
GET    /reports/summary?year=&month=
GET    /reports/trend?period=weekly|monthly|yearly&start=&end=
GET    /reports/category-breakdown?year=&month=
GET    /reports/export/csv?start=&end=&...
GET    /reports/export/pdf?year=&month=
```

---

## 5. ER Model

```
┌───────────────────┐
│       users       │
│─────────────────  │
│ PK id (UUID)      │
│    email          │
│    password_hash  │
│    display_name   │
│    currency       │◄──────────────────────────────────────┐
│    created_at     │                                       │
└────────┬──────────┘                                       │
         │ 1                                                │
         │                                                  │
         ├─────────────────────┐                            │
         │ N                   │ N                          │
         ▼                     ▼                            │
┌────────────────┐    ┌─────────────────────┐              │
│   accounts     │    │   categories        │              │
│────────────────│    │─────────────────────│              │
│ PK id          │    │ PK id               │              │
│ FK user_id     │    │ FK user_id (NULL=系統)│             │
│    name        │    │ FK parent_id (自參照) │             │
│    type        │    │    name             │              │
│    currency    │    │    type (IN/EX/BOTH)│              │
│    init_balance│    │    icon             │              │
│    is_archived │    │    color            │              │
│    note        │    │    is_system        │              │
│    created_at  │    │    created_at       │              │
└────────┬───────┘    └──────────┬──────────┘              │
         │ 1                     │ 1                        │
         │                       │                          │
         │ N                     │ N                        │
         ▼                       ▼                          │
┌──────────────────────────────────────────────┐           │
│               transactions                   │           │
│──────────────────────────────────────────────│           │
│ PK id (UUID)                                 │           │
│ FK user_id ──────────────────────────────────┼───────────┘
│ FK account_id                                │
│ FK category_id                               │
│ FK transfer_pair_id (自參照, NULL if not xfer)│
│    type  (INCOME | EXPENSE | TRANSFER)       │
│    amount (NUMERIC 15,2)                     │
│    currency                                  │
│    transaction_date                          │
│    note                                      │
│    is_deleted                                │
│    created_at                                │
│    updated_at                                │
└──────────────────────────────────────────────┘
         │ 1
         │
         │ 0..1
         ▼
┌─────────────────────┐
│   attachments        │
│─────────────────────│
│ PK id               │
│ FK transaction_id   │
│    file_name        │
│    file_path        │
│    mime_type        │
│    file_size        │
│    created_at       │
└─────────────────────┘

┌──────────────────────────────────────────────┐
│                  budgets                     │
│──────────────────────────────────────────────│
│ PK id                                        │
│ FK user_id                                   │
│ FK category_id                               │
│    year        (INT)                         │
│    month       (INT, 1-12)                   │
│    amount_limit (NUMERIC 15,2)               │
│    created_at                                │
│    updated_at                                │
│ UNIQUE (user_id, category_id, year, month)   │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│             refresh_tokens                   │
│──────────────────────────────────────────────│
│ PK id                                        │
│ FK user_id                                   │
│    token_hash                                │
│    expires_at                                │
│    revoked    (BOOLEAN)                      │
│    created_at                                │
└──────────────────────────────────────────────┘
```

---

## 6. 資料表定義

### 6.1 `users`

```sql
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    currency      CHAR(3)      NOT NULL DEFAULT 'TWD',  -- ISO 4217
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

### 6.2 `accounts`

```sql
CREATE TYPE account_type AS ENUM ('CASH', 'BANK', 'CREDIT_CARD', 'E_WALLET', 'INVESTMENT', 'OTHER');

CREATE TABLE accounts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL REFERENCES users(id),
    name           VARCHAR(100) NOT NULL,
    type           account_type NOT NULL,
    currency       CHAR(3)      NOT NULL DEFAULT 'TWD',
    initial_balance NUMERIC(15,2) NOT NULL DEFAULT 0,
    is_archived    BOOLEAN      NOT NULL DEFAULT FALSE,
    note           TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
```

### 6.3 `categories`

```sql
CREATE TYPE category_type AS ENUM ('INCOME', 'EXPENSE', 'BOTH');

CREATE TABLE categories (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID          REFERENCES users(id),   -- NULL = 系統預設分類
    parent_id  UUID          REFERENCES categories(id),
    name       VARCHAR(100)  NOT NULL,
    type       category_type NOT NULL,
    icon       VARCHAR(100),                          -- Material Icon name
    color      CHAR(7),                               -- HEX color, e.g. #FF5733
    is_system  BOOLEAN       NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_user_id ON categories(user_id);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);
```

### 6.4 `transactions`

```sql
CREATE TYPE transaction_type AS ENUM ('INCOME', 'EXPENSE', 'TRANSFER');

CREATE TABLE transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID             NOT NULL REFERENCES users(id),
    account_id        UUID             NOT NULL REFERENCES accounts(id),
    category_id       UUID             NOT NULL REFERENCES categories(id),
    transfer_pair_id  UUID             REFERENCES transactions(id),  -- 轉帳對應筆
    type              transaction_type NOT NULL,
    amount            NUMERIC(15,2)    NOT NULL CHECK (amount > 0),
    currency          CHAR(3)          NOT NULL DEFAULT 'TWD',
    transaction_date  DATE             NOT NULL,
    note              TEXT,
    is_deleted        BOOLEAN          NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tx_user_date   ON transactions(user_id, transaction_date DESC);
CREATE INDEX idx_tx_account     ON transactions(account_id);
CREATE INDEX idx_tx_category    ON transactions(category_id);
CREATE INDEX idx_tx_user_type   ON transactions(user_id, type);
```

### 6.5 `attachments`

```sql
CREATE TABLE attachments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID         NOT NULL REFERENCES transactions(id),
    file_name      VARCHAR(255) NOT NULL,
    file_path      VARCHAR(500) NOT NULL,
    mime_type      VARCHAR(100) NOT NULL,
    file_size      BIGINT       NOT NULL,  -- bytes
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

### 6.6 `budgets`

```sql
CREATE TABLE budgets (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID          NOT NULL REFERENCES users(id),
    category_id  UUID          NOT NULL REFERENCES categories(id),
    year         SMALLINT      NOT NULL CHECK (year BETWEEN 2000 AND 2100),
    month        SMALLINT      NOT NULL CHECK (month BETWEEN 1 AND 12),
    amount_limit NUMERIC(15,2) NOT NULL CHECK (amount_limit > 0),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, category_id, year, month)
);

CREATE INDEX idx_budgets_user_ym ON budgets(user_id, year, month);
```

### 6.7 `refresh_tokens`

```sql
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256 of raw token
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rt_user_id ON refresh_tokens(user_id);
```

---

## 7. Redis 快取策略

| Key Pattern | 內容 | TTL | 失效時機 |
|-------------|------|-----|----------|
| `balance:{accountId}` | 帳戶餘額快照 | 5 min | 新增/刪除交易時 |
| `report:summary:{userId}:{year}:{month}` | 月度摘要 JSON | 1 hr | 當月有新交易時 |
| `budget:status:{userId}:{year}:{month}` | 預算執行率 | 10 min | 新增支出交易時 |
| `blacklist:token:{jti}` | 登出 Token 黑名單 | Access Token TTL | — |

---

## 8. 安全設計

### JWT 設計

```
Access Token  (15 min)：payload = { sub: userId, email, role, jti }
Refresh Token (7 day)  ：隨機 UUID，SHA-256 後存入 refresh_tokens 表
```

### 安全要點

| 項目 | 做法 |
|------|------|
| 密碼儲存 | BCrypt, cost factor = 12 |
| Token 傳遞 | Authorization: Bearer header（禁止 URL 參數） |
| Refresh Token 輪換 | 每次 refresh 後舊 token 立即撤銷（Rotation） |
| 資源隔離 | 所有查詢強制 `WHERE user_id = :currentUserId`，防止 IDOR |
| CSRF | 純 REST API + Bearer Token，無 session cookie，CSRF 風險低 |
| Rate Limiting | 登入端點 5 次/分鐘（Spring 攔截器 + Redis 計數器） |
| 前端憑證保存 | 現行 SPA 以前端狀態 + `localStorage` 保存 token；若未來提升安全等級，可改為 HttpOnly Cookie 架構 |

---

## 9. 模組架構

```
frontend/
│
├── pages/                 頁面容器與工作區進入點
├── components/dashboard/  Overview / Activity / Planning / Setup UI 元件
├── composables/           API 存取、token 續期、session 邏輯
├── types/                 API 與儀表板型別定義
└── assets/css/            全域樣式

src/main/java/com/yourname/pfm/
│
├── auth/
│   ├── controller/   AuthController
│   ├── service/      AuthService, JwtService, TokenBlacklistService
│   └── dto/          LoginRequest, TokenResponse, RegisterRequest
│
├── account/
│   ├── controller/   AccountController
│   ├── service/      AccountService, BalanceService
│   ├── repository/   AccountRepository
│   └── entity/       Account
│
├── transaction/
│   ├── controller/   TransactionController
│   ├── service/      TransactionService, TransferService, CsvImportService
│   ├── repository/   TransactionRepository
│   └── entity/       Transaction
│
├── category/
│   └── ...
│
├── budget/
│   ├── service/      BudgetService, BudgetAlertService
│   └── ...
│
├── report/
│   ├── controller/   ReportController
│   ├── service/      ReportService, CsvExportService, PdfExportService
│   └── dto/          SummaryResponse, TrendResponse, CategoryBreakdownResponse
│
└── common/
    ├── config/       SecurityConfig, SwaggerConfig, RedisConfig
    ├── exception/    GlobalExceptionHandler, BusinessException
    ├── security/     JwtAuthFilter, CurrentUserResolver
    └── util/         DateUtils, CurrencyUtils
```

---

## 10. 開放問題與建議

以下是規格中尚待決定的事項，建議在開發前確認：

| # | 問題 | 建議選項 |
|---|------|----------|
| Q1 | **多幣別支援**：是否需要匯率換算？ | v1 只支援單一本位幣（TWD），v2 再加匯率 API（Frankfurter） |
| Q2 | **PDF 報表實作**：用哪個 Library？ | 推薦 **iText 7**（功能完整）或 **JasperReports**（模板驅動） |
| Q3 | **附件儲存**：本地 or 雲端？ | 本地 File System 起步，介面設計成 `StorageService` 方便後換 S3 |
| Q4 | **Email 通知**：預算超支要寄信嗎？ | 可加 Spring Mail + Gmail SMTP，非核心功能先留 stub |
| Q5 | **定期交易**：如房租、訂閱服務？ | 建議 v2 實作，需加 `recurring_rules` 表 + Scheduler |
| Q6 | **測試資料種子**：本地開發需要嗎？ | 建議加 `data.sql` / Flyway migration，含 10 筆測試交易 |
| Q7 | **Soft Delete 查詢**：JPA 如何處理？ | 使用 `@Where(clause = "is_deleted = false")` 或 Spring Data Specification |
| Q8 | **前端認證保存策略**：是否改為 HttpOnly Cookie？ | 若系統將對外公開或提升安全等級，建議評估 Cookie + SameSite + CSRF 防護 |

---

## 附錄：預設分類種子資料

```
支出類別：
  🍜 餐飲  ├─ 早餐 / 午餐 / 晚餐 / 飲料 / 外送
  👔 衣物  ├─ 服裝 / 鞋子 / 配件
  🏠 住家  ├─ 租金 / 水電 / 瓦斯 / 網路 / 物業
  🚌 交通  ├─ 大眾運輸 / 計程車 / 汽車油費 / 停車費
  🎮 娛樂  ├─ 電影 / 遊戲 / 旅遊 / 運動
  🏥 醫療  ├─ 掛號 / 藥品 / 保健食品
  📚 教育  ├─ 書籍 / 課程 / 文具
  🛒 購物  ├─ 日用品 / 電器
  💰 金融  ├─ 貸款還款 / 保險費

收入類別：
  💼 工作  ├─ 薪資 / 獎金 / 兼職
  📈 投資  ├─ 股利 / 利息 / 基金贖回
  🎁 其他  ├─ 禮金 / 退稅 / 政府補助
```
