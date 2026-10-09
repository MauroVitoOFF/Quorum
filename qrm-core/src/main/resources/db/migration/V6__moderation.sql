CREATE TABLE qrm_mute (
    player_id VARCHAR(36) PRIMARY KEY,
    player_name VARCHAR(64) NOT NULL,
    staff_id VARCHAR(36),
    staff_name VARCHAR(64) NOT NULL,
    reason VARCHAR(128) NOT NULL,
    at BIGINT NOT NULL,
    expires_at BIGINT
);
CREATE INDEX idx_qrm_mute_at ON qrm_mute(at);

CREATE TABLE qrm_warning (
    seq BIGINT PRIMARY KEY,
    player_id VARCHAR(36) NOT NULL,
    player_name VARCHAR(64) NOT NULL,
    staff_id VARCHAR(36),
    staff_name VARCHAR(64) NOT NULL,
    reason VARCHAR(128) NOT NULL,
    at BIGINT NOT NULL
);
CREATE INDEX idx_qrm_warning_player ON qrm_warning(player_id);

CREATE TABLE qrm_staff_note (
    seq BIGINT PRIMARY KEY,
    player_id VARCHAR(36) NOT NULL,
    player_name VARCHAR(64) NOT NULL,
    staff_id VARCHAR(36),
    staff_name VARCHAR(64) NOT NULL,
    note VARCHAR(200) NOT NULL,
    at BIGINT NOT NULL
);
CREATE INDEX idx_qrm_staff_note_player ON qrm_staff_note(player_id);
