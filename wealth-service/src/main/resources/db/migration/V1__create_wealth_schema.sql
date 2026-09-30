CREATE TABLE investor_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    country_code CHAR(2) NOT NULL,
    risk_profile VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_investor_profiles_user UNIQUE (user_id),
    CONSTRAINT ck_investor_profile_country
        CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_investor_profile_risk
        CHECK (risk_profile IN (
            'CONSERVATIVE',
            'MODERATE',
            'BALANCED',
            'GROWTH',
            'AGGRESSIVE'
        ))
);

CREATE TABLE portfolios (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    base_currency CHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT ck_portfolio_currency
        CHECK (base_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_portfolio_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_portfolios_user_id
    ON portfolios(user_id);

CREATE TABLE positions (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL REFERENCES portfolios(id),
    asset_id UUID NOT NULL,
    quantity NUMERIC(28, 10) NOT NULL,
    average_cost NUMERIC(28, 10) NOT NULL,
    currency CHAR(3) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_position_portfolio_asset
        UNIQUE (portfolio_id, asset_id),
    CONSTRAINT ck_position_quantity
        CHECK (quantity >= 0),
    CONSTRAINT ck_position_average_cost
        CHECK (average_cost >= 0),
    CONSTRAINT ck_position_currency
        CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_positions_portfolio_id
    ON positions(portfolio_id);

CREATE TABLE processed_wealth_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);