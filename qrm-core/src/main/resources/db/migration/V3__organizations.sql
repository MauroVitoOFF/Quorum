-- QRM v0.3: organizzazioni, rank e membri.
CREATE TABLE qrm_organization (
    id VARCHAR(32) PRIMARY KEY,
    label VARCHAR(64) NOT NULL,
    status VARCHAR(8) NOT NULL,
    account_id VARCHAR(36) NOT NULL REFERENCES qrm_account(id),
    created_at BIGINT NOT NULL
);

CREATE TABLE qrm_org_rank (
    org_id VARCHAR(32) NOT NULL REFERENCES qrm_organization(id),
    rank_id VARCHAR(32) NOT NULL,
    label VARCHAR(64) NOT NULL,
    level INT NOT NULL,
    permissions TEXT NOT NULL,
    PRIMARY KEY (org_id, rank_id),
    UNIQUE (org_id, level)
);

CREATE TABLE qrm_org_member (
    org_id VARCHAR(32) NOT NULL REFERENCES qrm_organization(id),
    character_id VARCHAR(36) NOT NULL REFERENCES qrm_character(id),
    rank_id VARCHAR(32) NOT NULL,
    since BIGINT NOT NULL,
    PRIMARY KEY (org_id, character_id)
);
CREATE INDEX idx_qrm_org_member_char ON qrm_org_member(character_id);
