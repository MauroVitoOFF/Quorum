CREATE TABLE qrm_character_job (
    character_id VARCHAR(36) PRIMARY KEY REFERENCES qrm_character(id),
    job_id VARCHAR(32) NOT NULL,
    grade_id VARCHAR(32) NOT NULL,
    on_duty INT NOT NULL,
    since BIGINT NOT NULL
);
CREATE INDEX idx_qrm_character_job_job ON qrm_character_job(job_id);

CREATE TABLE qrm_permission_override (
    character_id VARCHAR(36) NOT NULL REFERENCES qrm_character(id),
    node VARCHAR(128) NOT NULL,
    effect VARCHAR(8) NOT NULL,
    PRIMARY KEY (character_id, node)
);
