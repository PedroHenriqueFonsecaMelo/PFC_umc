/* ================================================================
   estante.js — Página da Estante (/livros/estante)
   Lê livros do localStorage, permite selecionar e prosseguir
   para o checkout com os IDs selecionados via sessionStorage.
   ================================================================ */

const CART_KEY = "bibliotroca_carrinho";
const CHECKOUT_KEY = "bibliotroca_checkout_ids";
const CUPOM_KEY = "bibliotroca_checkout_cupom";

/* ── Estado do cupom ── */
let _cupomAtivo = null; // { codigo, percentual, desconto, totalComDesconto, totalOriginal }

/* ── Helpers do carrinho ── */

function getCarrinho() {
    try {
        return JSON.parse(localStorage.getItem(CART_KEY)) || [];
    } catch (_) {
        return [];
    }
}

function saveCarrinho(itens) {
    localStorage.setItem(CART_KEY, JSON.stringify(itens));
}

/* ── Saldo na navbar ── */
function carregarSaldo() {
    fetch("/clientes/meu-perfil-json", { credentials: "include" })
        .then((r) => r.ok ? r.json() : null)
        .then((c) => {
            if (!c) return;
            const el = document.getElementById("navSaldo");
            if (el) el.textContent = `T$ ${(c.saldoTokens || 0).toFixed(2)}`;
        }).catch(() => {});
}

/* ── Classe CSS por estado ── */
function classeEstado(estado) {
    const mapa = {
        "ÓTIMO": "otimo",
        "OTIMO": "otimo",
        "BOM": "bom",
        "COMO_NOVO": "otimo",
        "REGULAR": "regular",
        "RUIM": "ruim",
    };
    return mapa[(estado || "").toUpperCase()] || "bom";
}

/* ── Renderiza a lista de livros ── */
function renderEstante() {
    const itens = getCarrinho();
    const vazia = document.getElementById("estanteVazia");
    const conteudo = document.getElementById("estanteConteudo");
    const lista = document.getElementById("listaEstante");

    if (!lista) return;

    if (itens.length === 0) {
        if (vazia) vazia.style.display = "block";
        if (conteudo) conteudo.style.display = "none";
        return;
    }

    if (vazia) vazia.style.display = "none";
    if (conteudo) conteudo.style.display = "block";

    lista.innerHTML = itens.map((item) => {
        let foto = null;
        try {
            const src = item.fotosUrls || item.fotoUrl;
            const arr = JSON.parse(src);
            if (Array.isArray(arr) && arr.length > 0) foto = arr[0];
        } catch (_) {}
        if (!foto && item.isbn) {
            foto = "https://covers.openlibrary.org/b/isbn/" +
                item.isbn.replace(/-/g, "") + "-L.jpg";
        }
        if (!foto) foto = "/img/logo-bibliotroca.png";

        const estado = item.estadoAprovado || "BOM";
        const preco = (item.precoAprovado || 0).toFixed(2);
        const estadoLabel = estado.replace("_", " ");

        return `
        <div class="estante-item" id="item-${item.id}">
            <input type="checkbox" class="estante-item-check" id="chk-${item.id}"
                   data-id="${item.id}" data-preco="${item.precoAprovado || 0}"
                   onchange="atualizarSubtotal()">
            <img class="estante-item-img" src="${foto}" alt="${
            escHtml(item.titulo)
        }"
                 onerror="this.src='https://via.placeholder.com/54x72?text=📚'">
            <div class="estante-item-info">
                <div class="estante-item-titulo">${escHtml(item.titulo)}</div>
                <div class="estante-item-autor">${
            escHtml(item.autor || "")
        }</div>
                <span class="estante-item-estado estado-${
            classeEstado(estado)
        }">${estadoLabel}</span>
            </div>
            <span class="estante-item-preco">T$ ${preco}</span>
            <button class="estante-item-remover" onclick="removerItem(${item.id})"
                    title="Remover da estante">✕</button>
        </div>`;
    }).join("");

    atualizarSubtotal();
}

/* ── Escapa HTML para evitar XSS ── */
function escHtml(str) {
    return String(str || "")
        .replace(/&/g, "&amp;").replace(/</g, "&lt;")
        .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

/* ── Atualiza subtotal e estado dos botões ── */
function atualizarSubtotal() {
    const checks = document.querySelectorAll(".estante-item-check:checked");
    const totalOriginal = Array.from(checks).reduce(
        (s, c) => s + parseFloat(c.dataset.preco || 0),
        0,
    );
    const qtd = checks.length;

    const subtotalEl = document.getElementById("subtotalValor");
    const qtdEl = document.getElementById("qtdSelecionados");
    const btnPross = document.getElementById("btnProsseguir");
    const btnRemover = document.getElementById("btnRemoverSelecionados");
    const chkTodos = document.getElementById("chkSelecionarTodos");

    // Se houver cupom ativo, recalcula com o novo total
    if (_cupomAtivo) {
        const desconto = totalOriginal * (_cupomAtivo.percentual / 100);
        const totalFinal = Math.max(0, totalOriginal - desconto);
        _cupomAtivo.totalOriginal = totalOriginal;
        _cupomAtivo.desconto = desconto;
        _cupomAtivo.totalComDesconto = totalFinal;
        renderCupomAplicado();

        if (subtotalEl) {
            subtotalEl.innerHTML =
                `<span class="subtotal-original">T$ ${
                    totalOriginal.toFixed(2)
                }</span>` +
                `<span class="subtotal-valor" style="color:var(--forest,#4a5d23)">T$ ${
                    totalFinal.toFixed(2)
                }</span>`;
        }
    } else {
        if (subtotalEl) {
            subtotalEl.textContent = `T$ ${totalOriginal.toFixed(2)}`;
        }
    }

    if (qtdEl) qtdEl.textContent = qtd;
    if (btnPross) btnPross.disabled = qtd === 0;
    if (btnRemover) btnRemover.disabled = qtd === 0;

    // Atualiza estado do "Selecionar todos"
    if (chkTodos) {
        const totalItens =
            document.querySelectorAll(".estante-item-check").length;
        chkTodos.checked = totalItens > 0 && qtd === totalItens;
        chkTodos.indeterminate = qtd > 0 && qtd < totalItens;
    }

    // Destaca visualmente os itens selecionados
    document.querySelectorAll(".estante-item-check").forEach((c) => {
        const item = document.getElementById("item-" + c.dataset.id);
        if (item) item.classList.toggle("selecionado", c.checked);
    });
}

/* ── Selecionar / desselecionar todos ── */
window.toggleSelecionarTodos = function (chk) {
    document.querySelectorAll(".estante-item-check").forEach((c) => {
        c.checked = chk.checked;
    });
    atualizarSubtotal();
};

/* ── Remover item individual ── */
window.removerItem = function (id) {
    saveCarrinho(getCarrinho().filter((i) => i.id !== id));
    renderEstante();
};

/* ── Remover itens selecionados ── */
window.removerSelecionados = function () {
    const selecionados = new Set(
        Array.from(document.querySelectorAll(".estante-item-check:checked"))
            .map((c) => parseInt(c.dataset.id, 10)),
    );
    saveCarrinho(getCarrinho().filter((i) => !selecionados.has(i.id)));
    renderEstante();
};

/* ── Aplicar cupom ── */
window.aplicarCupom = async function () {
    const codigo = (document.getElementById("cupomCodigo")?.value || "").trim()
        .toUpperCase();
    const feedbackEl = document.getElementById("cupomFeedback");

    if (!codigo) return;

    // Calcula total dos itens selecionados
    const checks = document.querySelectorAll(".estante-item-check:checked");
    const total = Array.from(checks).reduce(
        (s, c) => s + parseFloat(c.dataset.preco || 0),
        0,
    );

    if (checks.length === 0) {
        mostrarFeedbackCupom(
            "Selecione pelo menos um livro antes de aplicar o cupom.",
            "erro",
        );
        return;
    }

    const btn = document.querySelector(".btn-cupom-aplicar");
    if (btn) {
        btn.disabled = true;
        btn.textContent = "…";
    }

    try {
        const res = await fetch(
            `/api/cupons/validar?codigo=${
                encodeURIComponent(codigo)
            }&total=${total}`,
            { credentials: "include" },
        );
        const data = await res.json();

        if (!res.ok || !data.valido) {
            mostrarFeedbackCupom(data.mensagem || "Cupom inválido.", "erro");
            _cupomAtivo = null;
        } else {
            _cupomAtivo = {
                codigo: data.codigo,
                percentual: data.percentual,
                totalOriginal: data.totalOriginal,
                desconto: data.desconto,
                totalComDesconto: data.totalComDesconto,
            };
            if (feedbackEl) feedbackEl.style.display = "none";
            atualizarSubtotal();
        }
    } catch (_) {
        mostrarFeedbackCupom("Erro ao validar cupom. Tente novamente.", "erro");
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = "Aplicar";
        }
    }
};

function mostrarFeedbackCupom(msg, tipo) {
    const el = document.getElementById("cupomFeedback");
    if (!el) return;
    el.className = "cupom-feedback " + tipo;
    el.textContent = msg;
    el.style.display = "block";
    const bloco = document.getElementById("cupomAplicadoBloco");
    if (bloco) bloco.style.display = "none";
}

function renderCupomAplicado() {
    if (!_cupomAtivo) return;
    const bloco = document.getElementById("cupomAplicadoBloco");
    if (!bloco) return;
    bloco.innerHTML = `<div>
            <span class="cupom-aplicado-info">${
        escHtml(_cupomAtivo.codigo)
    } — ${_cupomAtivo.percentual}% OFF</span>
            <span class="cupom-aplicado-desconto"> -T$ ${
        (_cupomAtivo.desconto || 0).toFixed(2)
    }</span>
         </div>
         <button class="btn-cupom-remover" onclick="removerCupom()">✕ Remover</button>`;
    bloco.style.display = "flex";
}

/* ── Remover cupom ── */
window.removerCupom = function () {
    _cupomAtivo = null;
    const bloco = document.getElementById("cupomAplicadoBloco");
    const feedback = document.getElementById("cupomFeedback");
    const input = document.getElementById("cupomCodigo");
    if (bloco) bloco.style.display = "none";
    if (feedback) feedback.style.display = "none";
    if (input) input.value = "";
    atualizarSubtotal();
};

/* ── Prosseguir para o checkout ── */
window.prosseguirCheckout = function () {
    const ids = Array.from(
        document.querySelectorAll(".estante-item-check:checked"),
    )
        .map((c) => parseInt(c.dataset.id, 10));

    if (ids.length === 0) return;

    // Persiste os IDs selecionados e o cupom para a página de checkout
    sessionStorage.setItem(CHECKOUT_KEY, JSON.stringify(ids));
    if (_cupomAtivo) {
        sessionStorage.setItem(CUPOM_KEY, JSON.stringify(_cupomAtivo));
    } else {
        sessionStorage.removeItem(CUPOM_KEY);
    }
    window.location.href = "/livros/checkout";
};

/* ── Sincronizar preços com a API ── */
async function sincronizarPrecos() {
    const itens = getCarrinho();
    if (itens.length === 0) return;

    const avisos = [];
    let atualizado = false;

    await Promise.all(itens.map(async (item) => {
        try {
            const res = await fetch(`/api/livros/${item.id}`, {
                credentials: "include",
            });
            if (!res.ok) return; // livro pode ter sido removido; ignora silenciosamente
            const livro = await res.json();
            const precoNovo = livro.precoAprovado ?? livro.preco;
            if (precoNovo == null) return;

            if (Math.abs((item.precoAprovado || 0) - precoNovo) > 0.001) {
                avisos.push(
                    `O preço de "<strong>${
                        escHtml(item.titulo)
                    }</strong>" foi atualizado de T$ ${
                        (item.precoAprovado || 0).toFixed(2)
                    } para T$ ${precoNovo.toFixed(2)}.`,
                );
                item.precoAprovado = precoNovo;
                atualizado = true;
            }
        } catch (_) { /* rede indisponível — mantém preço local */ }
    }));

    if (atualizado) {
        saveCarrinho(itens);
        renderEstante();
    }

    if (avisos.length > 0) {
        let banner = document.getElementById("preco-atualizado-banner");
        if (!banner) {
            banner = document.createElement("div");
            banner.id = "preco-atualizado-banner";
            banner.style.cssText =
                "background:#fef9c3;border:1px solid #fde047;border-radius:8px;padding:.85rem 1.1rem;margin-bottom:1rem;font-size:.85rem;color:#854d0e;line-height:1.6;";
            const conteudo = document.getElementById("estanteConteudo");
            if (conteudo) conteudo.prepend(banner);
        }
        banner.innerHTML = "<strong>⚠ Preços atualizados:</strong><br>" +
            avisos.join("<br>");
    }
}

/* ── Init ── */
document.addEventListener("DOMContentLoaded", () => {
    // Garante que nenhum cupom de compra anterior persista
    sessionStorage.removeItem(CUPOM_KEY);
    carregarSaldo();
    renderEstante();
    sincronizarPrecos();
});
