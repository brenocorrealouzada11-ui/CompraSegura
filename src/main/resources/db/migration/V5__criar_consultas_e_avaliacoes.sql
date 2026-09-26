CREATE TABLE avaliacoes_confiabilidade (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    anuncio_id BIGINT NOT NULL,
    risco VARCHAR(20) NOT NULL,
    contem_simulacao BOOLEAN NOT NULL,
    versao_regra VARCHAR(80) NOT NULL,
    avaliada_em TIMESTAMP NOT NULL,
    atual BOOLEAN NOT NULL,
    motivos TEXT NOT NULL,
    pendencias TEXT NOT NULL,
    contexto TEXT NOT NULL,
    CONSTRAINT fk_avaliacao_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncios(id) ON DELETE CASCADE
);
CREATE INDEX idx_avaliacao_anuncio ON avaliacoes_confiabilidade(anuncio_id, id);
CREATE TABLE consultas_imei (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    avaliacao_id BIGINT NOT NULL,
    imei VARCHAR(15) NOT NULL,
    situacao VARCHAR(20) NOT NULL,
    origem VARCHAR(20) NOT NULL,
    provedor VARCHAR(100) NOT NULL,
    consultado_em TIMESTAMP NOT NULL,
    observacao VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_consulta_avaliacao FOREIGN KEY (avaliacao_id) REFERENCES avaliacoes_confiabilidade(id) ON DELETE CASCADE
);
