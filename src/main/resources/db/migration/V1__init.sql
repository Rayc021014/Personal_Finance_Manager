CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'TWD',
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'TWD',
    initial_balance NUMERIC(15,2) NOT NULL DEFAULT 0,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    parent_id UUID REFERENCES categories(id),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    icon VARCHAR(100),
    color CHAR(7),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    category_id UUID NOT NULL REFERENCES categories(id),
    transfer_pair_id UUID REFERENCES transactions(id),
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL DEFAULT 'TWD',
    transaction_date DATE NOT NULL,
    note TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE budgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    category_id UUID NOT NULL REFERENCES categories(id),
    budget_year SMALLINT NOT NULL CHECK (budget_year BETWEEN 2000 AND 2100),
    budget_month SMALLINT NOT NULL CHECK (budget_month BETWEEN 1 AND 12),
    amount_limit NUMERIC(15,2) NOT NULL CHECK (amount_limit > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, category_id, budget_year, budget_month)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_categories_user_id ON categories(user_id);
CREATE INDEX idx_categories_parent_id ON categories(parent_id);
CREATE INDEX idx_tx_user_date ON transactions(user_id, transaction_date DESC);
CREATE INDEX idx_tx_account ON transactions(account_id);
CREATE INDEX idx_tx_category ON transactions(category_id);
CREATE INDEX idx_tx_user_type ON transactions(user_id, type);
CREATE INDEX idx_budgets_user_ym ON budgets(user_id, budget_year, budget_month);
CREATE INDEX idx_rt_user_id ON refresh_tokens(user_id);

INSERT INTO categories (id, user_id, parent_id, name, type, icon, color, is_system, is_deleted)
VALUES
    ('11111111-1111-1111-1111-111111111111', NULL, NULL, '餐飲', 'EXPENSE', 'restaurant', '#FF7043', TRUE, FALSE),
    ('11111111-1111-1111-1111-111111111112', NULL, '11111111-1111-1111-1111-111111111111', '早餐', 'EXPENSE', 'free_breakfast', '#FFAB91', TRUE, FALSE),
    ('11111111-1111-1111-1111-111111111113', NULL, '11111111-1111-1111-1111-111111111111', '午餐', 'EXPENSE', 'lunch_dining', '#FFAB91', TRUE, FALSE),
    ('11111111-1111-1111-1111-111111111114', NULL, '11111111-1111-1111-1111-111111111111', '晚餐', 'EXPENSE', 'dinner_dining', '#FFAB91', TRUE, FALSE),
    ('11111111-1111-1111-1111-111111111121', NULL, NULL, '交通', 'EXPENSE', 'directions_bus', '#42A5F5', TRUE, FALSE),
    ('11111111-1111-1111-1111-111111111122', NULL, '11111111-1111-1111-1111-111111111121', '大眾運輸', 'EXPENSE', 'train', '#90CAF9', TRUE, FALSE),
    ('11111111-1111-1111-1111-111111111131', NULL, NULL, '娛樂', 'EXPENSE', 'sports_esports', '#AB47BC', TRUE, FALSE),
    ('11111111-1111-1111-1111-111111111141', NULL, NULL, '醫療', 'EXPENSE', 'medical_services', '#66BB6A', TRUE, FALSE),
    ('11111111-1111-1111-1111-111111111151', NULL, NULL, '購物', 'EXPENSE', 'shopping_bag', '#FFA726', TRUE, FALSE),
    ('22222222-2222-2222-2222-222222222221', NULL, NULL, '工作', 'INCOME', 'work', '#26A69A', TRUE, FALSE),
    ('22222222-2222-2222-2222-222222222222', NULL, '22222222-2222-2222-2222-222222222221', '薪資', 'INCOME', 'payments', '#80CBC4', TRUE, FALSE),
    ('22222222-2222-2222-2222-222222222231', NULL, NULL, '投資', 'INCOME', 'trending_up', '#5C6BC0', TRUE, FALSE),
    ('22222222-2222-2222-2222-222222222241', NULL, NULL, '其他收入', 'INCOME', 'redeem', '#8D6E63', TRUE, FALSE),
    ('33333333-3333-3333-3333-333333333331', NULL, NULL, '轉帳', 'BOTH', 'swap_horiz', '#78909C', TRUE, FALSE);
