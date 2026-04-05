-- ================================================================
-- data.sql — Bibliotroca · SQLite
-- Senhas: admin123  (BCrypt gerado com strength 12)
-- Para trocar: acesse https://bcrypt-generator.com, rounds = 12
-- ================================================================

-- ----------------------------------------------------------------
-- 1. LIMPEZA (ordem respeita FK: filhos antes dos pais)
-- ----------------------------------------------------------------
DELETE FROM pontuacao_usuario;
DELETE FROM pedido;
DELETE FROM livro_anuncio;
DELETE FROM lote_livros;
DELETE FROM cliente_cartao;
DELETE FROM cliente_endereco;
DELETE FROM users;
DELETE FROM admins;

-- ----------------------------------------------------------------
-- 2. ADMINISTRADOR
--    Tabela: admins  |  Colunas: id, nome, email, password
-- ----------------------------------------------------------------
INSERT INTO admins (id, nome, email, password)
VALUES (1, 'Admin Master', 'admin@admin.com',
        '$2a$12$ucclBvrrh8e9hc8nD9EuyuO7zpYe2seLLiebOs.UU4n5DKb4aKkda');

-- ----------------------------------------------------------------
-- 3. CLIENTES DE TESTE
--    Tabela: users
--    Colunas: id, nome, email, senha, saldo_tokens, bloqueada,
--             cpf, data_criacao, datanasc, gen, tentativas
-- ----------------------------------------------------------------
INSERT INTO users (id, nome, email, senha, saldo_tokens, bloqueada, cpf, data_criacao, datanasc, gen, tentativas)
VALUES
    (1, 'Cliente Teste',  'cliente@teste.com',
     '$2a$12$ucclBvrrh8e9hc8nD9EuyuO7zpYe2seLLiebOs.UU4n5DKb4aKkda',
     5000.0, 0, '12345678900', '2024-01-01 10:00:00', '1990-01-01', 'M', 0),

    (2, 'Maria Leitora', 'maria@teste.com',
     '$2a$12$ucclBvrrh8e9hc8nD9EuyuO7zpYe2seLLiebOs.UU4n5DKb4aKkda',
     200.0, 0, '98765432100', '2024-01-05 09:00:00', '1995-06-15', 'F', 0),

    (3, 'Pedro Leitor',  'pedro@teste.com',
     '$2a$12$ucclBvrrh8e9hc8nD9EuyuO7zpYe2seLLiebOs.UU4n5DKb4aKkda',
     80.0,  0, '11122233344', '2024-02-10 14:00:00', '1988-03-22', 'M', 0);

-- ----------------------------------------------------------------
-- 4. LOTE PENDENTE (dono: Cliente Teste)
--    Tabela: lote_livros
--    Colunas: id, codigo_protocolo, data_criacao, status, cliente_id
-- ----------------------------------------------------------------
INSERT INTO lote_livros (id, codigo_protocolo, data_criacao, status, cliente_id)
VALUES (1, 'lote-system-001', '2024-01-02 12:00:00', 'PENDENTE', 1);

-- ----------------------------------------------------------------
-- 5. LIVROS DO LOTE — aguardando aprovação do admin
--    Tabela: livro_anuncio
--    Colunas: id, titulo, autor, isbn, fotos_urls, lote_id,
--             data_anuncio, aprovado, preco_aprovado,
--             estado_aprovado, admin_aprovador_id, data_aprovacao
-- ----------------------------------------------------------------
INSERT INTO livro_anuncio (titulo, autor, isbn, fotos_urls, lote_id, data_anuncio, aprovado)
VALUES
    ('1984',          'George Orwell', '978-0060918111',
     '["/uploads/orwell1.jpg","/uploads/orwell2.jpg"]',
     1, '2024-01-02 12:01:00', 0),

    ('Harry Potter e a Pedra Filosofal', 'J.K. Rowling', '978-0439708180',
     '["/uploads/hp1.jpg","/uploads/hp2.jpg"]',
     1, '2024-01-02 12:02:00', 0),

    ('O Senhor dos Anéis', 'J.R.R. Tolkien', '978-0618640157',
     '["/uploads/lotr1.jpg"]',
     1, '2024-01-02 12:03:00', 0);

-- ----------------------------------------------------------------
-- 6. LIVROS APROVADOS — visíveis na vitrine
-- ----------------------------------------------------------------
INSERT INTO livro_anuncio (titulo, autor, isbn, fotos_urls, data_anuncio, aprovado,
                           preco_aprovado, estado_aprovado, admin_aprovador_id, data_aprovacao)
VALUES
    ('Dom Casmurro', 'Machado de Assis', '978-8535902778',
     '["/uploads/domcasmurro.jpg"]',
     '2024-01-03 10:00:00', 1, 30.0, 'BOM', 1, '2024-01-03 10:30:00'),

    ('O Alquimista', 'Paulo Coelho', '978-0062315007',
     '["/uploads/alquimista.jpg"]',
     '2024-01-03 11:00:00', 1, 40.0, 'OTIMO', 1, '2024-01-03 11:30:00'),

    ('Sapiens', 'Yuval Noah Harari', '978-0062316097',
     '["/uploads/sapiens.jpg"]',
     '2024-01-04 09:00:00', 1, 50.0, 'NOVO', 1, '2024-01-04 09:30:00');

-- ----------------------------------------------------------------
-- 7. PONTUAÇÃO INICIAL (gamificação)
--    Tabela: pontuacao_usuario
--    Colunas: id, cliente_id, xp_total, xp_livros_aprovados,
--             xp_compras, xp_avaliacoes, ultima_atualizacao
-- ----------------------------------------------------------------
INSERT INTO pontuacao_usuario (id, cliente_id, xp_total, xp_livros_aprovados, xp_compras, xp_avaliacoes, ultima_atualizacao)
VALUES
    (1, 1, 150, 100, 30, 20, '2024-01-10 10:00:00'),
    (2, 2,  90,  50, 30, 10, '2024-01-11 10:00:00'),
    (3, 3,  40,   0, 30, 10, '2024-01-12 10:00:00');

-- ================================================================
-- RESUMO DE ACESSO
-- ================================================================
-- Admin  : admin@admin.com   / admin123  → /admin/login
-- User 1 : cliente@teste.com / admin123  → saldo 500 T$, XP 150
-- User 2 : maria@teste.com   / admin123  → saldo 200 T$, XP 90
-- User 3 : pedro@teste.com   / admin123  → saldo 80 T$,  XP 40
-- ================================================================
