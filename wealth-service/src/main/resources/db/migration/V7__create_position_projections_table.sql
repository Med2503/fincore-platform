CREATE TABLE position_projections (
    portfolio_id UUID NOT NULL,
    asset_id UUID NOT NULL,
    quantity NUMERIC(28, 10) NOT NULL,
    average_cost NUMERIC(28, 10) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    execution_sequence BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    PRIMARY KEY (portfolio_id, asset_id),

    CONSTRAINT chk_projection_quantity
        CHECK (quantity >= 0),

    CONSTRAINT chk_projection_average_cost
        CHECK (average_cost >= 0),

    CONSTRAINT chk_projection_sequence
        CHECK (execution_sequence > 0)
);

CREATE INDEX idx_position_projections_portfolio
    ON position_projections(portfolio_id);