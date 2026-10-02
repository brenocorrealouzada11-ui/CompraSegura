ALTER TABLE anuncios ADD COLUMN vendido_em TIMESTAMP NULL;
CREATE INDEX idx_anuncios_vendedor_status ON anuncios(vendedor_id, status, id);
