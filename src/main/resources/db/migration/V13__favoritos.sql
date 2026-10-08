CREATE TABLE favoritos (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 usuario_id BIGINT NOT NULL,
 anuncio_id BIGINT NULL,
 titulo_salvo VARCHAR(120) NOT NULL,
 salvo_em TIMESTAMP NOT NULL,
 CONSTRAINT uk_favorito_usuario_anuncio UNIQUE (usuario_id, anuncio_id),
 CONSTRAINT fk_favorito_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
 CONSTRAINT fk_favorito_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncios(id) ON DELETE SET NULL
);
CREATE INDEX ix_favorito_usuario_data ON favoritos(usuario_id, salvo_em, id);
