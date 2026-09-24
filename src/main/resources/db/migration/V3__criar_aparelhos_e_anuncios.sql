CREATE TABLE aparelhos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL,
    marca VARCHAR(80) NOT NULL,
    modelo VARCHAR(100) NOT NULL,
    condicao VARCHAR(1000) NOT NULL,
    alteracoes VARCHAR(1000) NOT NULL
);
CREATE TABLE aparelho_imeis (
    aparelho_id BIGINT NOT NULL,
    ordem INTEGER NOT NULL,
    numero VARCHAR(15) NOT NULL,
    PRIMARY KEY (aparelho_id, ordem),
    CONSTRAINT uk_aparelho_imei UNIQUE (aparelho_id, numero),
    CONSTRAINT fk_imeis_aparelho FOREIGN KEY (aparelho_id) REFERENCES aparelhos(id)
);
CREATE TABLE anuncios (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    vendedor_id BIGINT NOT NULL,
    aparelho_id BIGINT NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    descricao VARCHAR(3000) NOT NULL,
    preco DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_anuncio_aparelho UNIQUE (aparelho_id),
    CONSTRAINT fk_anuncio_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuarios(id),
    CONSTRAINT fk_anuncio_aparelho FOREIGN KEY (aparelho_id) REFERENCES aparelhos(id)
);
CREATE INDEX idx_anuncios_vendedor ON anuncios(vendedor_id, id);
