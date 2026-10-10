-- Faccia su cui mostrare l'icona del punto (NORTH, SOUTH, EAST, WEST); NULL = tutte e quattro.
ALTER TABLE qrm_interaction_point ADD COLUMN face VARCHAR(8);
