-- Punti di interazione: blocchi marcati dallo staff, appartenenti a un job o a un'organizzazione.
CREATE TABLE qrm_interaction_point (
    id INT PRIMARY KEY,
    type_id VARCHAR(32) NOT NULL,
    owner_kind VARCHAR(16) NOT NULL,
    owner_id VARCHAR(32) NOT NULL,
    dimension VARCHAR(128) NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    z INT NOT NULL,
    min_grade_id VARCHAR(32),
    public_visible INT NOT NULL,
    name VARCHAR(64),
    created_at BIGINT NOT NULL,
    UNIQUE (dimension, x, y, z)
);
CREATE INDEX idx_qrm_interaction_point_owner ON qrm_interaction_point(owner_kind, owner_id);
