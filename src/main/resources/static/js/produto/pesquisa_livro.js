document.addEventListener("DOMContentLoaded", async () => {
    // 1. Captura as configurações injetadas pelo Thymeleaf no HTML
    const configElem = document.getElementById('config-pag');

    if (!configElem) {
        console.error("Erro: Elemento #config-pag não encontrado no HTML.");
        return;
    }

    const isbn = configElem.getAttribute('data-isbn');
    // Converte a string "true"/"false" do atributo para booleano real
    const unificado = configElem.getAttribute('data-unificado') === 'true';

    console.log("Iniciando carregamento:", { isbn, unificado });

    if (isbn && isbn.length >= 10) {
        // 2. Busca dados visuais em APIs externas (Google/OpenLibrary)
        await carregarDadosLivroAPI(isbn);

        // 3. Busca dados do seu banco local (Resumo oficial e Resenhas)
        await carregarDadosInternos(isbn, unificado);
    } else {
        document.getElementById('tituloLivro').innerText = "ISBN Inválido ou não informado";
    }
});

/**
 * BUSCA DADOS EXTERNOS (Capa, Título e Autor para preencher a página)
 */
async function carregarDadosLivroAPI(isbn) {
    let tituloEncontrado = "Título não encontrado";
    let autorEncontrado = "Autor Desconhecido";
    let resumoEncontrado = "";
    let capaUrl = "";

    try {
        // Tentativa 1: Open Library
        const resOL = await fetch(`https://openlibrary.org/api/books?bibkeys=ISBN:${isbn}&format=json&jscmd=data`);
        const dataOL = await resOL.json();
        const infoOL = dataOL[`ISBN:${isbn}`];

        if (infoOL) {
            tituloEncontrado = infoOL.title || tituloEncontrado;
            autorEncontrado = infoOL.authors ? infoOL.authors.map(a => a.name).join(", ") : autorEncontrado;
            capaUrl = infoOL.cover ? (infoOL.cover.large || infoOL.cover.medium) : "";
            resumoEncontrado = infoOL.description ? (typeof infoOL.description === 'string' ? infoOL.description : infoOL.description.value) : "";
        }

        // Tentativa 2: Google Books (para complementar resumo)
        const resGB = await fetch(`https://www.googleapis.com/books/v1/volumes?q=isbn:${isbn}`);
        if (resGB.ok) {
            const dataGB = await resGB.json();
            if (dataGB.totalItems > 0) {
                const vol = dataGB.items[0].volumeInfo;
                resumoEncontrado = vol.description || resumoEncontrado;
                if (tituloEncontrado.includes("não encontrado")) tituloEncontrado = vol.title;
                if (autorEncontrado === "Autor Desconhecido" && vol.authors) autorEncontrado = vol.authors.join(", ");
            }
        }
    } catch (err) {
        console.error("Erro ao consultar APIs externas:", err);
    }

    // Atualiza a interface básica
    document.getElementById('tituloLivro').innerText = tituloEncontrado;
    document.getElementById('autorLivro').innerText = autorEncontrado;

    if (resumoEncontrado) {
        document.getElementById('containerResumo').innerHTML = `<p>${resumoEncontrado}</p>`;
    }

    const capaElem = document.getElementById('containerCapa');
    capaElem.innerHTML = capaUrl
        ? `<img src="${capaUrl}" alt="Capa do Livro" style="max-width:200px; box-shadow: 5px 5px 15px rgba(0,0,0,0.2);">`
        : `<div style="padding:20px; border:1px solid #ccc; background:#eee;">Capa não disponível</div>`;
}

/**
 * BUSCA DADOS DO SEU BANCO DE DADOS (API LOCAL)
 */
async function carregarDadosInternos(isbn, unificado) {
    try {
        // Rota baseada na lógica de unificação
        const url = unificado
            ? `/api/avaliacoes/livro/unificado/${isbn}`
            : `/api/avaliacoes/livro/${isbn}`;

        console.log("Buscando dados internos em:", url);

        const response = await fetch(url);
        if (!response.ok) throw new Error("Falha na resposta da API local");

        const data = await response.json();
        const lista = document.getElementById('listaComentarios');

        // Se houver resumo oficial cadastrado pelo Admin, ele sobrescreve o da API externa
        if (data.resumoOficial && data.resumoOficial.trim() !== "" && !data.resumoOficial.includes("Conteúdo compartilhado")) {
            document.getElementById('containerResumo').innerHTML = `<p><strong>Sinopse Oficial:</strong><br>${data.resumoOficial}</p>`;
        }

        // Renderização das Avaliações
        if (data.avaliacoes && data.avaliacoes.length > 0) {
            lista.innerHTML = ""; // Limpa o "Nenhuma resenha"

            data.avaliacoes.forEach(av => {
                // Importante: usamos 'isbnOriginalNoAto' que é o nome do campo no seu Java/JSON
                const isbnEdicao = av.isbnOriginalNoAto || isbn;
                const nomeUsuario = av.avaliador ? av.avaliador.nome : "Leitor Anônimo";

                lista.innerHTML += `
                    <div class="resenha-item" style="border-bottom: 1px solid #ddd; padding: 15px 0;">
                        <div class="resenha-header" style="display:flex; justify-content:space-between;">
                            <strong>${nomeUsuario}</strong>
                            <span class="stars" style="color: #722f37;">
                                ${"★".repeat(av.nota)}${"☆".repeat(5 - av.nota)}
                            </span>
                        </div>
                        <p style="margin: 10px 0;">${av.comentario}</p>
                        <small style="color: #666;">
                            Postado em: ${new Date(av.dataAvaliacao).toLocaleDateString('pt-BR')}
                            ${unificado ? ` | <span style="color:#4a5d23">Edição: ${isbnEdicao}</span>` : ''}
                        </small>
                    </div>
                `;
            });
        } else {
            lista.innerHTML = `<p style="font-style: italic; color: #7a6e65;">Ainda não há resenhas para esta ${unificado ? 'obra' : 'edição'}. Seja o primeiro!</p>`;
        }

    } catch (err) {
        console.error("Erro ao carregar dados do banco local:", err);
    }
}

/**
 * ENVIA UMA NOVA RESENHA PARA O BANCO
 */
async function enviarResenha() {
    const texto = document.getElementById('textoComentario').value;
    const nota = document.getElementById('notaLivro').value;
    const titulo = document.getElementById('tituloLivro').innerText.trim();
    const autor = document.getElementById('autorLivro').innerText.trim();

    // Pega o ISBN da URL
    const pathParts = window.location.pathname.split('/').filter(Boolean);
    const isbn = pathParts[1];

    if (!texto.trim()) {
        alert("Por favor, escreva sua resenha antes de publicar.");
        return;
    }

    const autorTratado = (autor === "Autor Desconhecido" || !autor) ? "Não informado" : autor;

    // Payload ÚNICO e corrigido
    const payload = {
        isbn: isbn,
        titulo: titulo,
        autor: autorTratado,
        comentario: texto,
        nota: parseInt(nota)
    };

    try {
        const response = await fetch('/api/avaliacoes/salvar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            alert("Sua resenha foi eternizada com sucesso!");
            location.reload();
        } else {
            const msg = await response.text();
            alert("Não foi possível salvar: " + msg);
        }
    } catch (err) {
        alert("Erro de conexão com o servidor.");
    }
}