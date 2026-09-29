ALTER TABLE anuncios ADD COLUMN publicado_em TIMESTAMP NULL;
CREATE INDEX ix_anuncios_status_publicacao ON anuncios (status, publicado_em, id);
CREATE INDEX ix_anuncios_vendedor_status ON anuncios (vendedor_id, status);
