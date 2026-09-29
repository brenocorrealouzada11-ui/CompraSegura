CREATE TABLE conversas (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 anuncio_id BIGINT NULL,
 anuncio_titulo VARCHAR(120) NOT NULL,
 comprador_id BIGINT NOT NULL,
 vendedor_id BIGINT NOT NULL,
 criada_em TIMESTAMP(6) NOT NULL,
 atualizada_em TIMESTAMP(6) NOT NULL,
 ultima_previa VARCHAR(160) NOT NULL,
 lida_comprador BIGINT NOT NULL DEFAULT 0,
 lida_vendedor BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_conversa_anuncio_comprador UNIQUE (anuncio_id, comprador_id),
 CONSTRAINT fk_conversa_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncios(id) ON DELETE SET NULL,
 CONSTRAINT fk_conversa_comprador FOREIGN KEY (comprador_id) REFERENCES usuarios(id) ON DELETE CASCADE,
 CONSTRAINT fk_conversa_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuarios(id) ON DELETE CASCADE,
 CONSTRAINT ck_conversa_participantes CHECK (comprador_id <> vendedor_id)
);
CREATE INDEX ix_conversa_comprador ON conversas (comprador_id, atualizada_em, id);
CREATE INDEX ix_conversa_vendedor ON conversas (vendedor_id, atualizada_em, id);
CREATE TABLE mensagens (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 conversa_id BIGINT NOT NULL,
 remetente_id BIGINT NOT NULL,
 texto VARCHAR(2000) NOT NULL,
 chave_envio VARCHAR(36) NOT NULL,
 enviada_em TIMESTAMP(6) NOT NULL,
 CONSTRAINT fk_mensagem_conversa FOREIGN KEY (conversa_id) REFERENCES conversas(id) ON DELETE CASCADE,
 CONSTRAINT uk_mensagem_envio UNIQUE (conversa_id, remetente_id, chave_envio)
);
CREATE INDEX ix_mensagem_conversa ON mensagens (conversa_id, id, remetente_id);
