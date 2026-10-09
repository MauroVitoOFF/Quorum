CREATE TABLE qrm_staff_grant (
    account VARCHAR(36) NOT NULL,
    node VARCHAR(128) NOT NULL,
    PRIMARY KEY (account, node)
);
