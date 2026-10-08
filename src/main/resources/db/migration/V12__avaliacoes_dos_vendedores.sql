CREATE TABLE avaliacoes_vendedor (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 vendedor_id BIGINT NOT NULL,
 autor_id BIGINT NOT NULL,
 nota INTEGER NOT NULL,
 comentario VARCHAR(1000) NOT NULL,
 criada_em TIMESTAMP NOT NULL,
 atualizada_em TIMESTAMP NOT NULL,
 CONSTRAINT uk_avaliacao_vendedor_autor UNIQUE (vendedor_id, autor_id),
 CONSTRAINT fk_avaliacao_perfil_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuarios(id) ON DELETE CASCADE,
 CONSTRAINT fk_avaliacao_perfil_autor FOREIGN KEY (autor_id) REFERENCES usuarios(id) ON DELETE CASCADE,
 CONSTRAINT ck_avaliacao_perfil_nota CHECK (nota BETWEEN 1 AND 5),
 CONSTRAINT ck_avaliacao_perfil_autor CHECK (vendedor_id <> autor_id)
);
CREATE INDEX ix_avaliacao_perfil_data ON avaliacoes_vendedor(vendedor_id, atualizada_em, id);
