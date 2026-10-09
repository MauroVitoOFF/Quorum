CREATE TABLE qrm_staff_log (
    seq BIGINT PRIMARY KEY,
    at BIGINT NOT NULL,
    staff_id VARCHAR(36),
    staff_name VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_id VARCHAR(36),
    target_name VARCHAR(64),
    detail VARCHAR(256) NOT NULL,
    outcome VARCHAR(8) NOT NULL
);
CREATE INDEX idx_qrm_staff_log_at ON qrm_staff_log(at);
CREATE INDEX idx_qrm_staff_log_staff ON qrm_staff_log(staff_id);
CREATE INDEX idx_qrm_staff_log_target ON qrm_staff_log(target_id);
