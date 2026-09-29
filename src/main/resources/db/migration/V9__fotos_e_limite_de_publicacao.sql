ALTER TABLE usuarios ADD COLUMN exclusoes_rapidas INT NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN bloqueado_ate TIMESTAMP NULL;
CREATE TABLE fotos_anuncio (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 anuncio_id BIGINT NOT NULL,
 conteudo MEDIUMBLOB NOT NULL,
 tipo_conteudo VARCHAR(30) NOT NULL,
 CONSTRAINT fk_foto_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncios(id) ON DELETE CASCADE
);
CREATE INDEX ix_fotos_anuncio ON fotos_anuncio (anuncio_id, id);
CREATE TABLE fotos_perfil (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 usuario_id BIGINT NOT NULL UNIQUE,
 conteudo MEDIUMBLOB NOT NULL,
 tipo_conteudo VARCHAR(30) NOT NULL,
 CONSTRAINT fk_foto_perfil FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);
