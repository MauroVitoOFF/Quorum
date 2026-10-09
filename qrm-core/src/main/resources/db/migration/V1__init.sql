CREATE TABLE qrm_player (
    id VARCHAR(36) PRIMARY KEY,
    first_seen BIGINT NOT NULL,
    last_seen BIGINT NOT NULL,
    character_slots INT NOT NULL,
    active_character VARCHAR(36)
);

CREATE TABLE qrm_character (
    id VARCHAR(36) PRIMARY KEY,
    player_id VARCHAR(36) NOT NULL REFERENCES qrm_player(id),
    first_name VARCHAR(64) NOT NULL,
    last_name VARCHAR(64) NOT NULL,
    name_key VARCHAR(160) NOT NULL,
    birth_date VARCHAR(10) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX idx_qrm_character_player ON qrm_character(player_id);
CREATE INDEX idx_qrm_character_name ON qrm_character(name_key);

CREATE TABLE qrm_account (
    id VARCHAR(36) PRIMARY KEY,
    owner_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(36) NOT NULL,
    status VARCHAR(16) NOT NULL,
    overdraft_allowed INT NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX idx_qrm_account_owner ON qrm_account(owner_type, owner_id);

CREATE TABLE qrm_account_balance (
    account_id VARCHAR(36) NOT NULL REFERENCES qrm_account(id),
    currency VARCHAR(16) NOT NULL,
    amount BIGINT NOT NULL,
    PRIMARY KEY (account_id, currency)
);

CREATE TABLE qrm_transaction (
    id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) NOT NULL,
    from_account VARCHAR(36) NOT NULL REFERENCES qrm_account(id),
    to_account VARCHAR(36) NOT NULL REFERENCES qrm_account(id),
    amount BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL,
    reason VARCHAR(128) NOT NULL,
    created_at BIGINT NOT NULL,
    metadata TEXT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE
);
CREATE INDEX idx_qrm_tx_from ON qrm_transaction(from_account);
CREATE INDEX idx_qrm_tx_to ON qrm_transaction(to_account);

CREATE TABLE qrm_mod_data (
    mod_ns VARCHAR(64) NOT NULL,
    owner_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(36) NOT NULL,
    data_key VARCHAR(64) NOT NULL,
    data_value TEXT NOT NULL,
    PRIMARY KEY (mod_ns, owner_type, owner_id, data_key)
);
