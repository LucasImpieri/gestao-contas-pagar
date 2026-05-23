-- V1: initial schema for fornecedor and conta

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE fornecedor (
                            id UUID PRIMARY KEY,
                            nome VARCHAR(255) NOT NULL
);

CREATE TABLE conta (
                       id UUID PRIMARY KEY,
                       data_vencimento DATE NOT NULL,
                       data_pagamento DATE,
                       valor NUMERIC(15,2) NOT NULL,
                       descricao TEXT,
                       situacao VARCHAR(20) NOT NULL,
                       fornecedor_id UUID NOT NULL,

                       CONSTRAINT fk_conta_fornecedor
                           FOREIGN KEY (fornecedor_id)
                               REFERENCES fornecedor(id)
);

CREATE INDEX idx_conta_data_vencimento
    ON conta (data_vencimento);

CREATE INDEX idx_conta_fornecedor_id
    ON conta (fornecedor_id);

CREATE INDEX idx_conta_descricao_trgm
    ON conta USING gin (lower(coalesce(descricao, '')) gin_trgm_ops);