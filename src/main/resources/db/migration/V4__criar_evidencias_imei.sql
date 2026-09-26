CREATE TABLE evidencias_imei (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    anuncio_id BIGINT NOT NULL,
    tipo_conteudo VARCHAR(20) NOT NULL,
    tamanho_bytes INTEGER NOT NULL,
    enviada_em TIMESTAMP NOT NULL,
    conteudo MEDIUMBLOB NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_evidencia_anuncio UNIQUE (anuncio_id),
    CONSTRAINT fk_evidencia_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncios(id) ON DELETE CASCADE
);
