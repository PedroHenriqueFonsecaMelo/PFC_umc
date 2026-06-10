/* ================================================================
   lista_desejos.js — Lista de Desejos Híbrida · Bibliotroca
   ================================================================ */

const authHeaders = { "Content-Type": "application/json" };

// Guarda IDs salvos (pode ser Google ID ou OpenLibrary Work ID)
let idsSalvos = new Set();
const metaCache = {};

function mostrarToast(msg, tipo) {
    const t = document.getElementById("toast");
    if (!t) return;
    t.textContent = msg;
    t.className = "show " + tipo;
    setTimeout(() => {
        t.className = "";
    }, 3200);
}

async function carregarPerfil() {
    try {
        const res = await fetch("/clientes/meu-perfil-json", {
            headers: authHeaders,
            credentials: "include",
        });
        if (res.ok) {
            const c = await res.json();
            const navSaldo = document.getElementById("navSaldo");
            if (navSaldo) {
                navSaldo.textContent = "T$ " + (c.saldoTokens || 0).toFixed(2);
            }
        }
    } catch (_) {}
}

function fmtData(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    return d.toLocaleDateString("pt-BR", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    });
}

function capaUrlOpenLibrary(isbn) {
    if (!isbn) return null;
    const isbn13 = isbn.replace(/[^0-9X]/g, "");
    if (isbn13.length === 10 || isbn13.length === 13) {
        return "https://covers.openlibrary.org/b/isbn/" + isbn13 + "-M.jpg";
    }
    return null;
}

function escolherMelhorIsbn(isbns) {
    if (!isbns || isbns.length === 0) return null;
    const norm = isbns.map((i) => i.replace(/[^0-9X]/gi, ""));
    return norm.find((i) => i.startsWith("97885")) ||
        norm.find((i) => i.startsWith("978972")) ||
        norm.find((i) => i.length === 13) ||
        norm.find((i) => i.length === 10) ||
        norm[0];
}

async function buscarTituloPorIsbn(isbn) {
    try {
        const res = await fetch(
            `https://openlibrary.org/api/books?bibkeys=ISBN:${isbn}&format=json&jscmd=data`,
        );
        const data = await res.json();
        const info = data[`ISBN:${isbn}`];
        if (!info) return null;
        return {
            titulo: info.title || null,
            autores: info.authors
                ? info.authors.slice(0, 2).map((a) => a.name).join(", ")
                : null,
        };
    } catch (_) {
        return null;
    }
}

/* ================================================================
   MOTORES DE BUSCA (OpenLibrary & Google Books)
   ================================================================ */

async function buscarOpenLibrary(query) {
    try {
        const url = "https://openlibrary.org/search.json?title=" +
            encodeURIComponent(query) +
            "&limit=10&fields=title,author_name,isbn,cover_i,key";
        const res = await fetch(url);
        if (!res.ok) return null;
        const data = await res.json();

        const docs = (data.docs || []).slice(0, 8);

        return await Promise.all(docs.map(async (doc) => {
            const isbn = escolherMelhorIsbn(doc.isbn || []);
            const edicao = isbn ? await buscarTituloPorIsbn(isbn) : null;
            const capa = doc.cover_i
                ? `https://covers.openlibrary.org/b/id/${doc.cover_i}-M.jpg`
                : capaUrlOpenLibrary(isbn);

            return {
                googleBookId: "",
                openLibraryWorkId: doc.key, // Ex: /works/OL12345W
                isbn: isbn || "",
                titulo: (edicao && edicao.titulo) || doc.title || "Sem título",
                autores: (edicao && edicao.autores) ||
                    (doc.author_name
                        ? doc.author_name.slice(0, 2).join(", ")
                        : "Autor desconhecido"),
                capa,
            };
        }));
    } catch (_) {
        return null;
    }
}

async function buscarGoogleBooks(query) {
    try {
        const url = `https://www.googleapis.com/books/v1/volumes?q=${
            encodeURIComponent(query)
        }&maxResults=8`;
        const res = await fetch(url);
        if (!res.ok) return null;
        const data = await res.json();

        if (!data.items) return [];

        return data.items.map((item) => {
            const info = item.volumeInfo;
            const iden = info.industryIdentifiers || [];
            const isbn13 = iden.find((id) => id.type === "ISBN_13")?.identifier;
            const isbn10 = iden.find((id) => id.type === "ISBN_10")?.identifier;

            return {
                googleBookId: item.id,
                openLibraryWorkId: "",
                isbn: isbn13 || isbn10 || "",
                titulo: info.title || "Sem título",
                autores: info.authors
                    ? info.authors.slice(0, 2).join(", ")
                    : "Autor desconhecido",
                capa: info.imageLinks?.thumbnail ||
                    info.imageLinks?.smallThumbnail || null,
            };
        });
    } catch (_) {
        return null;
    }
}

// Orquestrador do Input de Busca
async function buscarLivros() {
    const input = document.getElementById("inputBusca");
    const query = input.value.trim();
    if (!query) {
        input.focus();
        return;
    }

    const btn = document.getElementById("btnBuscar");
    const container = document.getElementById("resultadosBusca");

    // Tenta ler um select no HTML (se houver), senão padroniza para o googlebooks
    const selectProvedor = document.getElementById("selectProvedor");
    const provedor = selectProvedor ? selectProvedor.value : "google";

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Buscando...';
    container.innerHTML = "";

    try {
        const items = (provedor === "openlibrary")
            ? await buscarOpenLibrary(query)
            : await buscarGoogleBooks(query);

        if (!items || items.length === 0) {
            container.innerHTML =
                `<p class="search-hint">Nenhum resultado encontrado para "${
                    escHtml(query)
                }" no provedor selecionado.</p>`;
            return;
        }

        container.innerHTML = items.filter(Boolean).map(buildResultadoItem)
            .join("");
    } catch (e) {
        container.innerHTML =
            '<p class="search-error"><i class="fa-solid fa-triangle-exclamation"></i> Erro ao buscar. Verifique a conexão.</p>';
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-search"></i> Buscar';
    }
}

/* ================================================================
   RENDERIZAÇÃO E ACTIONS
   ================================================================ */

function buildResultadoItem(item) {
    const { googleBookId, openLibraryWorkId, isbn, titulo, autores, capa } =
        item;
    const idUnicoFront = googleBookId || openLibraryWorkId;

    const imgHtml = capa
        ? `<img class="resultado-capa" src="${capa}" alt="Capa" onerror="this.style.display='none';this.nextElementSibling.style.display='flex'">`
        : "";
    const placeholderHtml = `<div class="resultado-capa-placeholder" style="${
        capa ? "display:none" : ""
    }">📚</div>`;

    // 1. Gera as chaves textuais idênticas às que colocamos na leitura da lista salva
    const isbnChave = isbn ? "ISBN:" + String(isbn).trim() : null;
    const textoChave = "TEXTO:" + String(titulo).toLowerCase().trim() +
        (autores ? "_" + String(autores).toLowerCase().trim() : "");

    // 2. Procura por QUALQUER um dos critérios dentro do Set de idsSalvos
    const jaAdicionado = idsSalvos.has(idUnicoFront) ||
        (isbnChave && idsSalvos.has(isbnChave)) ||
        idsSalvos.has(textoChave);

    // 3. Monta o botão baseado no estado real de duplicidade
    const btnHtml = jaAdicionado
        ? `<span class="btn-ja-adicionado"><i class="fa-solid fa-check"></i> Salvo</span>`
        : `<button class="btn-adicionar"
               data-googleid="${escHtml(googleBookId)}"
               data-workid="${escHtml(openLibraryWorkId)}"
               data-isbn="${escHtml(isbn)}"
               data-titulo="${escHtml(titulo)}"
               data-autores="${escHtml(autores)}"
               onclick="adicionarDesejo(this.dataset.googleid, this.dataset.workid, this.dataset.isbn, this.dataset.titulo, this.dataset.autores, this)">
             <i class="fa-solid fa-heart"></i> Salvar
           </button>`;

    return `
        <div class="resultado-item" id="res-${
        escHtml(idUnicoFront).replace(/\//g, "-")
    }">
            ${imgHtml}${placeholderHtml}
            <div class="resultado-info">
                <div class="resultado-titulo">${escHtml(titulo)}</div>
                <div class="resultado-autor">${escHtml(autores)}</div>
                <div class="resultado-isbn">ISBN: <span>${
        isbn ? escHtml(isbn) : "Não disponível"
    }</span></div>
            </div>
            ${btnHtml}
        </div>`;
}

async function adicionarDesejo(
    googleBookId,
    openLibraryWorkId,
    isbn,
    titulo,
    autor,
    btn,
) {
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';

    const idUnico = googleBookId || openLibraryWorkId;

    try {
        const res = await fetch("/api/lista-desejos", {
            method: "POST",
            headers: authHeaders,
            credentials: "include",
            body: JSON.stringify({
                googleBookId: googleBookId || null,
                openLibraryWorkId: openLibraryWorkId || null,
                isbn: isbn || null,
                titulo,
                autor,
            }),
        });

        if (res.status === 401) {
            window.location.href = "/clientes/login";
            return;
        }

        if (!res.ok) {
            const txt = await res.text();
            throw new Error(txt || "Erro ao adicionar");
        }

        metaCache[idUnico] = {
            titulo,
            autor,
            isbn,
            googleBookId,
            openLibraryWorkId,
        };
        idsSalvos.add(idUnico);

        btn.outerHTML =
            `<span class="btn-ja-adicionado"><i class="fa-solid fa-check"></i> Salvo</span>`;
        mostrarToast("Livro adicionado à lista de desejos!", "sucesso");
        carregarListaSalva();
    } catch (e) {
        mostrarToast(e.message || "Erro ao adicionar.", "erro");
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-heart"></i> Salvar';
    }
}

async function removerDesejo(id, card, idUnicoFront) {
    card.style.opacity = "0.5";
    try {
        const res = await fetch("/api/lista-desejos/" + id, {
            method: "DELETE",
            headers: authHeaders,
            credentials: "include",
        });
        if (res.status === 401) {
            window.location.href = "/clientes/login";
            return;
        }
        if (!res.ok) throw new Error("Erro ao remover");

        card.remove();
        mostrarToast("Livro removido da lista de desejos.", "sucesso");

        if (idUnicoFront) idsSalvos.delete(idUnicoFront);

        atualizarBadge();
        atualizarBotoesResultados();
    } catch (e) {
        mostrarToast("Erro ao remover. Tente novamente.", "erro");
        card.style.opacity = "1";
    }
}

async function carregarListaSalva() {
    const container = document.getElementById("wishlistContainer");
    if (!container) return;

    try {
        const res = await fetch("/api/lista-desejos", {
            headers: authHeaders,
            credentials: "include",
        });
        if (res.status === 401) {
            window.location.href = "/clientes/login";
            return;
        }
        if (!res.ok) throw new Error("Falha na API");

        const lista = await res.json();
        idsSalvos.clear();

        lista.forEach((d) => {
            const id = d.googleBookId || d.openLibraryWorkId;
            if (id) idsSalvos.add(id);

            if (d.isbn) idsSalvos.add("ISBN:" + d.isbn.trim());
            if (d.titulo) {
                const chaveTexto = "TEXTO:" + d.titulo.toLowerCase().trim() +
                    (d.autor ? "_" + d.autor.toLowerCase().trim() : "");
                idsSalvos.add(chaveTexto);
            }
        });

        if (lista.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="icon">🔖</div>
                    <p>Sua lista de desejos está vazia. Busque livros acima e salve os que deseja adquirir.</p>
                </div>`;
            atualizarBadge(0);
            return;
        }

        container.innerHTML =
            `<div class="skeleton"></div><div class="skeleton"></div>`;

        // Mapeia os itens vindo do seu próprio banco de dados
        container.innerHTML = lista.map(buildDesejoCard).join("");
        atualizarBadge(lista.length);
    } catch (e) {
        container.innerHTML =
            `<p style="text-align:center;color:var(--accent);padding:2rem">Erro ao carregar a lista de desejos.</p>`;
    }
}

function buildDesejoCard(d) {
    const idUnicoFront = d.googleBookId || d.openLibraryWorkId;

    // Fallback de capas inteligente baseado na origem
    let capa = null;
    if (d.isbn) {
        capa = capaUrlOpenLibrary(d.isbn);
    }

    const imgHtml = capa
        ? `<img class="desejo-capa" src="${capa}" alt="Capa" onerror="this.src='/img/logo-bibliotroca.png'">`
        : `<img src="/img/logo-bibliotroca.png" class="desejo-capa-placeholder" style="object-fit:contain;">`;

    // CORRIGIDO: Removida a duplicidade de declaração const
    const tituloHtml = d.titulo
        ? `<div class="desejo-titulo">${escHtml(d.titulo)}</div>`
        : `<div class="desejo-titulo" style="color:var(--muted);font-style:italic">Livro Sem Título</div>`;

    // CORRIGIDO: Removida a duplicidade de declaração const
    const autorHtml = d.autor
        ? `<div class="desejo-autor">${escHtml(d.autor)}</div>`
        : "";

    return `
        <div class="desejo-card" id="desejo-${d.id}">
            ${imgHtml}
            <div class="desejo-info">
                ${tituloHtml}
                ${autorHtml}
                <div class="desejo-meta">
                    <span class="desejo-isbn">${
        d.isbn ? escHtml(d.isbn) : "Sem ISBN"
    }</span>
                    <span class="desejo-data"><i class="fa-regular fa-calendar"></i> ${
        fmtData(d.dataAdicao)
    }</span>
                </div>
            </div>
            <button class="btn-remover" onclick="removerDesejo(${d.id}, document.getElementById('desejo-${d.id}'), '${
        escHtml(idUnicoFront)
    }')">
                <i class="fa-solid fa-trash-can"></i> Remover
            </button>
        </div>`;
}

function atualizarBadge(n) {
    const badge = document.getElementById("badgeTotal");
    if (!badge) return;
    const total = n !== undefined
        ? n
        : document.querySelectorAll(".desejo-card").length;
    if (total > 0) {
        badge.textContent = total;
        badge.style.display = "inline-block";
    } else {
        badge.style.display = "none";
    }
}

function atualizarBotoesResultados() {
    document.querySelectorAll(".resultado-item").forEach((item) => {
        const idUnicoFront = item.id.replace("res-", "");
        const btnWrap = item.querySelector(
            ".btn-adicionar, .btn-ja-adicionado",
        );
        if (!btnWrap) return;

        if (
            idsSalvos.has(idUnicoFront) ||
            idsSalvos.has("/" + idUnicoFront.replace(/-/g, "/"))
        ) {
            if (!btnWrap.classList.contains("btn-ja-adicionado")) {
                btnWrap.outerHTML =
                    `<span class="btn-ja-adicionado"><i class="fa-solid fa-check"></i> Salvo</span>`;
            }
        }
    });
}

function escHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

// Inicializadores
const inputBusca = document.getElementById("inputBusca");
if (inputBusca) {
    inputBusca.addEventListener("keydown", function (e) {
        if (e.key === "Enter") buscarLivros();
    });
}

carregarPerfil();
carregarListaSalva();
