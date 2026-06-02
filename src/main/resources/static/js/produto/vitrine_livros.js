/* ================================================================
   vitrine_livros.js — Vitrine de livros · Bibliotroca
   Paginação server-side (20/pág) + cards 3D + busca e filtros
   ================================================================ */

/* ── Saldo na navbar ── */
async function carregarSaldo() {
    try {
        const res = await fetch("/clientes/meu-perfil-json", {
            credentials: "include",
        });
        if (!res.ok) return;
        const c = await res.json();
        const navSaldo = document.getElementById("navSaldo");
        if (navSaldo) {
            navSaldo.textContent = `T$ ${(c.saldoTokens || 0).toFixed(2)}`;
        }
    } catch (_) {}
}

/* ── Estado global ── */
let _todosLivros = []; // livros da página atual (até 20 itens)
let _paginaAtual = 0;
const _paginaSalva = parseInt(sessionStorage.getItem('vitrine_pagina') || '0');
if (!isNaN(_paginaSalva) && _paginaSalva > 0) {
    _paginaAtual = _paginaSalva;
}
let _totalPaginas = 0;
let _totalElements = 0;
let modoPromo = false;

let _filtros = {
    busca: "",
    estados: [],
    generos: [],
    precoMin: null,
    precoMax: null,
    ordem: "relevancia",
};

let _debounceTimer = null;

/* ── Normalize: remove acentos e converte para minúsculo ── */
function norm(str) {
    return (str || "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLowerCase();
}

/* ── Contador regressivo das promoções ── */
let contadorInterval = null;

function iniciarContadores() {
    if (contadorInterval) clearInterval(contadorInterval);

    const atualizar = () => {
        const agora = Date.now();
        document.querySelectorAll(".promo-countdown").forEach((el) => {
            const expira = new Date(el.dataset.expira).getTime();
            const diff = expira - agora;
            const id = el.dataset.livroId;

            if (diff <= 0) {
                el.style.display = "none";
            } else {
                const h = Math.floor(diff / 3600000);
                const m = Math.floor((diff % 3600000) / 60000);
                const s = Math.floor((diff % 60000) / 1000);
                const timerEl = document.getElementById("timer-" + id);
                if (timerEl) timerEl.textContent = `${h}h ${m}m ${s}s`;
            }
        });
    };

    atualizar();
    contadorInterval = setInterval(atualizar, 1000);
}

/* ── Toggle: somente promoções ── */
function togglePromo() {
    modoPromo = !modoPromo;
    _paginaAtual = 0;
    sessionStorage.removeItem('vitrine_pagina');
    const btn = document.getElementById("btnPromo");
    const titulo = document.getElementById("vitrineTitulo");

    if (modoPromo) {
        btn.classList.add("ativo");
        btn.textContent = "✕ Ver Todos";
        if (titulo) titulo.textContent = "Promoções";
    } else {
        btn.classList.remove("ativo");
        btn.textContent = "🏷 Ver Promoções";
        if (titulo) titulo.textContent = "Livros Disponíveis";
    }
    carregarLivros();
}

/* ── Fallback de capa: OpenLibrary → foto do vendedor → placeholder ── */
window.vitrineFallback = function (img) {
    const fallback = img.dataset.fallback;
    if (fallback) {
        delete img.dataset.fallback;
        img.onerror = function () {
            img.style.display = "none";
            const ph = img.nextElementSibling;
            if (ph) ph.style.display = "flex";
        };
        img.src = fallback;
    } else {
        img.style.display = "none";
        const ph = img.nextElementSibling;
        if (ph) ph.style.display = "flex";
    }
};

/* ── Classe CSS do badge por estado ── */
function classeBadge(estado) {
    const mapa = {
        "NOVO": "novo",
        "OTIMO": "novo",
        "COMO_NOVO": "novo",
        "BOM": "bom",
        "REGULAR": "regular",
        "RUIM": "ruim",
    };
    return mapa[(estado || "").toUpperCase()] || "bom";
}

/* ── Renderiza a grade com a lista já filtrada/ordenada ── */
function renderLivros(livros) {
    const grid = document.getElementById("gridLivros");
    if (!grid) return;

    atualizarContador(livros.length);

    if (livros.length === 0) {
        grid.innerHTML = `
            <div class="vitrine-vazio" style="grid-column:1/-1;">
                <div class="vazio-icon">📚</div>
                <p class="vazio-msg">Nenhum livro encontrado para sua busca.</p>
                <button class="btn-limpar-vazio" onclick="limparFiltros()">Limpar filtros</button>
            </div>`;
        return;
    }

    grid.innerHTML = livros.map((livro) => {
        const isbn = (livro.isbn || "").replace(/-/g, "");
        let fotos = [];
        try {
            fotos = JSON.parse(livro.fotosUrls || "[]");
        } catch (_) {}
        const vendorFoto = fotos.length > 0 ? fotos[0] : "";

        const estado = livro.estadoAprovado || "BOM";
        const estadoLabel = estado.replace("_", " ");
        const badgeClass = classeBadge(estado);

        let precoHtml;
        if (livro.emPromocao && livro.precoOriginal) {
            precoHtml = `<p class="book-price">
                <span style="text-decoration:line-through;color:#9a8a80;font-size:.85rem;margin-right:.3rem;">
                    T$ ${livro.precoOriginal.toFixed(2)}</span>
                <span style="color:#e11d48;">T$ ${
                (livro.precoAprovado || 0).toFixed(2)
            }</span>
            </p>`;
        } else {
            precoHtml = `<p class="book-price">T$ ${
                (livro.precoAprovado || 0).toFixed(2)
            }</p>`;
        }

        const promoCountdown = (livro.emPromocao && livro.promocaoExpira)
            ? `<div class="promo-countdown"
                    data-livro-id="${livro.id}"
                    data-expira="${livro.promocaoExpira}"
                    style="font-size:.75rem;font-weight:700;color:#e11d48;margin-top:.3rem;">
                   🔥 Expira em: <span id="timer-${livro.id}">…</span>
               </div>`
            : "";

        // Lógica de imagem: OpenLibrary → foto do vendedor → placeholder
        let imgTag, phStyle;
        if (isbn) {
            const fbAttr = vendorFoto ? `data-fallback="${vendorFoto}"` : "";
            imgTag =
                `<img src="https://covers.openlibrary.org/b/isbn/${isbn}-L.jpg"
                            ${fbAttr} onerror="vitrineFallback(this)"
                            alt="${livro.titulo}" />`;
            phStyle = "display:none";
        } else if (vendorFoto) {
            imgTag = `<img src="${vendorFoto}"
                            onerror="this.style.display='none';this.nextElementSibling.style.display='flex'"
                            alt="${livro.titulo}" />`;
            phStyle = "display:none";
        } else {
            imgTag = "";
            phStyle = "display:flex";
        }

        return `
        <div class="book-item" onclick="window.location='/livros/${livro.id}'">
          <div class="main-book-wrap">
            <div class="book-cover">
              <div class="book-inside"></div>
              <div class="book-image">
                ${imgTag}
                <div class="placeholder-cover" style="${phStyle}">
                  <div class="ptitle">${livro.titulo}</div>
                  <div class="pauthor">${livro.autor}</div>
                </div>
                <div class="effect"></div>
                <div class="light"></div>
              </div>
            </div>
          </div>
          <div class="book-info">
            <span class="book-badge badge-${badgeClass}">${estadoLabel}</span>
            <p class="book-title">${livro.titulo}</p>
            <p class="book-author">por ${livro.autor}</p>
            ${precoHtml}
            ${promoCountdown}
          </div>
        </div>`;
    }).join("");

    iniciarContadores();
}

function popularFiltroGeneros(livros) {
    const generos = [
        ...new Set(
            livros
                .map((l) => l.genero)
                .filter((g) => g && g.trim()),
        ),
    ].sort((a, b) => a.localeCompare(b, "pt-BR"));

    const container = document.getElementById("filtroGeneros");
    if (!container) return;

    if (generos.length === 0) {
        container.innerHTML =
            '<span style="font-size:.8rem;color:#9a8a80;font-style:italic;">Nenhum gênero disponível</span>';
        return;
    }

    container.innerHTML = generos.map((g) =>
        `<label class="filtro-check-item">
            <input type="checkbox" value="${g}"
                   onchange="atualizarFiltroGeneros()"> ${g}
         </label>`
    ).join("");
}

function atualizarFiltroGeneros() {
    const checks = document.querySelectorAll("#filtroGeneros input:checked");
    _filtros.generos = Array.from(checks).map((c) => c.value);
    aplicarFiltros();
}

window.removerChipGenero = function (g) {
    _filtros.generos = _filtros.generos.filter((x) => x !== g);
    const cb = document.querySelector(`#filtroGeneros input[value="${g}"]`);
    if (cb) cb.checked = false;
    aplicarFiltros();
};

/* ── Paginação ── */
function irParaPagina(p) {
    if (p < 0 || p >= _totalPaginas) return;
    _paginaAtual = p;
    sessionStorage.setItem('vitrine_pagina', p);
    carregarLivros();
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function renderPaginacao() {
    const container = document.getElementById("vitrinePaginacao");
    if (!container) return;

    if (_totalPaginas <= 1) {
        container.innerHTML = "";
        return;
    }

    const isPrimeira = _paginaAtual === 0;
    const isUltima = _paginaAtual === _totalPaginas - 1;
    const delta = 2;
    let left = Math.max(0, _paginaAtual - delta);
    let right = Math.min(_totalPaginas - 1, _paginaAtual + delta);

    if (right - left < 4) {
        if (left === 0) right = Math.min(_totalPaginas - 1, left + 4);
        else left = Math.max(0, right - 4);
    }

    let html = "";
    html += `<button class="page-btn${isPrimeira ? " disabled" : ""}"
                     onclick="irParaPagina(${
        _paginaAtual - 1
    })" aria-label="Anterior">←</button>`;

    if (left > 0) {
        html += `<button class="page-btn" onclick="irParaPagina(0)">1</button>`;
        if (left > 1) {
            html +=
                `<span class="page-btn" style="border:none;cursor:default;pointer-events:none;">…</span>`;
        }
    }
    for (let i = left; i <= right; i++) {
        html += `<button class="page-btn${i === _paginaAtual ? " active" : ""}"
                         onclick="irParaPagina(${i})">${i + 1}</button>`;
    }
    if (right < _totalPaginas - 1) {
        if (right < _totalPaginas - 2) {
            html +=
                `<span class="page-btn" style="border:none;cursor:default;pointer-events:none;">…</span>`;
        }
        html += `<button class="page-btn" onclick="irParaPagina(${
            _totalPaginas - 1
        })">${_totalPaginas}</button>`;
    }

    html += `<button class="page-btn${isUltima ? " disabled" : ""}"
                     onclick="irParaPagina(${
        _paginaAtual + 1
    })" aria-label="Próxima">→</button>`;

    container.innerHTML = html;
}

/* ── Aplica filtros client-side sobre os livros da página atual ── */
function aplicarFiltros() {
    let lista = [..._todosLivros];

    if (_filtros.busca) {
        const termo = norm(_filtros.busca);
        lista = lista.filter((l) =>
            norm(l.titulo).includes(termo) ||
            norm(l.autor).includes(termo) ||
            norm(l.isbn || "").includes(termo)
        );
    }

    if (_filtros.estados.length > 0) {
        lista = lista.filter((l) => {
            const est = (l.estadoAprovado || "BOM").toUpperCase();
            return _filtros.estados.includes(est);
        });
    }

    if (_filtros.generos.length > 0) {
        lista = lista.filter((l) =>
            l.genero && _filtros.generos.includes(l.genero)
        );
    }

    if (_filtros.precoMin !== null) {
        lista = lista.filter((l) =>
            (l.precoAprovado || 0) >= _filtros.precoMin
        );
    }
    if (_filtros.precoMax !== null) {
        lista = lista.filter((l) =>
            (l.precoAprovado || 0) <= _filtros.precoMax
        );
    }

    switch (_filtros.ordem) {
        case "menor_preco":
            lista.sort((a, b) =>
                (a.precoAprovado || 0) - (b.precoAprovado || 0)
            );
            break;
        case "maior_preco":
            lista.sort((a, b) =>
                (b.precoAprovado || 0) - (a.precoAprovado || 0)
            );
            break;
        case "recente":
            lista.sort((a, b) => (b.id || 0) - (a.id || 0));
            break;
        case "az":
            lista.sort((a, b) =>
                (a.titulo || "").localeCompare(b.titulo || "", "pt-BR")
            );
            break;
        case "za":
            lista.sort((a, b) =>
                (b.titulo || "").localeCompare(a.titulo || "", "pt-BR")
            );
            break;
    }

    renderLivros(lista);
    renderChips();
    renderPaginacao();
}

/* ── Lê valores do painel de filtros e aplica ── */
function lerEAplicarFiltros() {
    const checks = document.querySelectorAll(
        "#filtroEstados input[type=checkbox]:checked",
    );
    _filtros.estados = Array.from(checks).map((c) => c.value);

    const genChecks = document.querySelectorAll("#filtroGeneros input:checked");
    _filtros.generos = Array.from(genChecks).map((c) => c.value);

    const minVal = document.getElementById("filtroPrecoMin")?.value;
    const maxVal = document.getElementById("filtroPrecoMax")?.value;
    _filtros.precoMin = minVal !== "" && minVal !== null
        ? parseFloat(minVal)
        : null;
    _filtros.precoMax = maxVal !== "" && maxVal !== null
        ? parseFloat(maxVal)
        : null;

    _filtros.ordem = document.getElementById("filtroOrdem")?.value ||
        "relevancia";

    _paginaAtual = 0;
    aplicarFiltros();
    fecharPainelFiltros();
}

/* ── Contador ── */
function atualizarContador(n) {
    const el = document.getElementById("vitrineContador");
    if (!el) return;
    const sufixo = _totalElements > 0 ? ` de ${_totalElements}` : "";
    el.textContent = `Exibindo ${n}${sufixo} livro${
        _totalElements !== 1 ? "s" : ""
    }`;
}

/* ── Chips de filtros ativos ── */
const ESTADO_LABEL_MAP = {
    NOVO: "Novo",
    OTIMO: "Ótimo",
    BOM: "Bom",
    REGULAR: "Regular",
    RUIM: "Ruim",
};
const ORDEM_LABEL_MAP = {
    menor_preco: "Menor preço",
    maior_preco: "Maior preço",
    recente: "Mais recente",
    az: "A–Z",
    za: "Z–A",
};

function renderChips() {
    const container = document.getElementById("filtroChips");
    if (!container) return;
    const chips = [];

    _filtros.estados.forEach((est) => {
        chips.push(
            `<span class="filtro-chip">Estado: ${ESTADO_LABEL_MAP[est] || est}
            <button onclick="removerChipEstado('${est}')" aria-label="Remover filtro">×</button></span>`,
        );
    });
    if (_filtros.precoMin !== null) {
        chips.push(
            `<span class="filtro-chip">De T$ ${_filtros.precoMin.toFixed(2)}
            <button onclick="removerChipPrecoMin()" aria-label="Remover filtro">×</button></span>`,
        );
    }
    if (_filtros.precoMax !== null) {
        chips.push(
            `<span class="filtro-chip">Até T$ ${_filtros.precoMax.toFixed(2)}
            <button onclick="removerChipPrecoMax()" aria-label="Remover filtro">×</button></span>`,
        );
    }
    _filtros.generos.forEach((g) => {
        chips.push(`<span class="filtro-chip">Gênero: ${g}
            <button onclick="removerChipGenero('${g}')" aria-label="Remover filtro">×</button></span>`);
    });
    if (_filtros.ordem !== "relevancia") {
        chips.push(
            `<span class="filtro-chip">Ordenar: ${
                ORDEM_LABEL_MAP[_filtros.ordem] || _filtros.ordem
            }
            <button onclick="removerChipOrdem()" aria-label="Remover filtro">×</button></span>`,
        );
    }

    container.innerHTML = chips.join("");
}

/* ── Remoção individual de chips ── */
window.removerChipEstado = function (est) {
    _filtros.estados = _filtros.estados.filter((e) => e !== est);
    const cb = document.querySelector(`#filtroEstados input[value="${est}"]`);
    if (cb) cb.checked = false;
    aplicarFiltros();
};
window.removerChipPrecoMin = function () {
    _filtros.precoMin = null;
    const el = document.getElementById("filtroPrecoMin");
    if (el) el.value = "";
    aplicarFiltros();
};
window.removerChipPrecoMax = function () {
    _filtros.precoMax = null;
    const el = document.getElementById("filtroPrecoMax");
    if (el) el.value = "";
    aplicarFiltros();
};
window.removerChipOrdem = function () {
    _filtros.ordem = "relevancia";
    const sel = document.getElementById("filtroOrdem");
    if (sel) sel.value = "relevancia";
    aplicarFiltros();
};

/* ── Busca em tempo real (debounce 400 ms) ── */
function onBuscaInput() {
    const val = document.getElementById("vitrineBusca")?.value || "";
    const btnX = document.getElementById("btnLimparBusca");
    if (btnX) btnX.style.display = val ? "flex" : "none";

    clearTimeout(_debounceTimer);
    _debounceTimer = setTimeout(() => {
        _filtros.busca = val;
        aplicarFiltros();
    }, 400);
}

function limparBusca() {
    _filtros.busca = "";
    const input = document.getElementById("vitrineBusca");
    if (input) {
        input.value = "";
        input.focus();
    }
    const btnX = document.getElementById("btnLimparBusca");
    if (btnX) btnX.style.display = "none";
    aplicarFiltros();
}

/* ── Limpar todos os filtros (volta à pág 1 e re-busca) ── */
function limparFiltros() {
    _filtros = {
        busca: "",
        estados: [],
        generos: [],
        precoMin: null,
        precoMax: null,
        ordem: "relevancia",
    };
    document.querySelectorAll("#filtroGeneros input").forEach((cb) => {
        cb.checked = false;
    });
    _paginaAtual = 0;

    const busca = document.getElementById("vitrineBusca");
    if (busca) busca.value = "";
    const btnX = document.getElementById("btnLimparBusca");
    if (btnX) btnX.style.display = "none";
    document.querySelectorAll("#filtroEstados input[type=checkbox]").forEach(
        (cb) => {
            cb.checked = false;
        },
    );
    const min = document.getElementById("filtroPrecoMin");
    const max = document.getElementById("filtroPrecoMax");
    if (min) min.value = "";
    if (max) max.value = "";
    const sel = document.getElementById("filtroOrdem");
    if (sel) sel.value = "relevancia";

    carregarLivros();
}

/* ── Painel de filtros (abrir/fechar) ── */
let _painelAberto = false;

function toggleFiltros() {
    _painelAberto ? fecharPainelFiltros() : abrirPainelFiltros();
}

function abrirPainelFiltros() {
    _painelAberto = true;
    document.getElementById("painelFiltros")?.classList.add("aberto");
    const arrow = document.getElementById("btnFiltrosArrow");
    if (arrow) arrow.textContent = "▴";
    document.getElementById("btnFiltros")?.classList.add("ativo");
}

function fecharPainelFiltros() {
    _painelAberto = false;
    document.getElementById("painelFiltros")?.classList.remove("aberto");
    const arrow = document.getElementById("btnFiltrosArrow");
    if (arrow) arrow.textContent = "▾";
    document.getElementById("btnFiltros")?.classList.remove("ativo");
}

/* ── Carrega página de livros do servidor ── */
async function carregarLivros() {
    const grid = document.getElementById("gridLivros");
    if (!grid) return;

    grid.innerHTML =
        `<p style="grid-column:1/-1;text-align:center;color:#7A6E65;
        padding:3rem 0;font-family:'IM Fell English',serif;font-style:italic;">Carregando…</p>`;
    const contadorEl = document.getElementById("vitrineContador");
    if (contadorEl) contadorEl.textContent = "";
    const paginacaoEl = document.getElementById("vitrinePaginacao");
    if (paginacaoEl) paginacaoEl.innerHTML = "";

    try {
        const params = new URLSearchParams({ page: _paginaAtual, size: 20 });
        if (modoPromo) params.set("emPromocao", "true");

        const res = await fetch(`/api/livros/vitrine?${params}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        _todosLivros = data.content || [];
        _totalPaginas = data.totalPages || 0;
        _totalElements = data.totalElements || 0;

        popularFiltroGeneros(_todosLivros);
        aplicarFiltros();
    } catch (err) {
        console.error("Erro ao carregar vitrine:", err);
        grid.innerHTML =
            `<p style="grid-column:1/-1;text-align:center;color:#722F37;
            padding:2.5rem 0;font-family:'IM Fell English',serif;font-style:italic">
            Erro ao carregar vitrine.</p>`;
    }
}

/* ── Init ── */
document.addEventListener("DOMContentLoaded", () => {
    carregarSaldo();
    carregarLivros();
});
