/* ================================================================
   estoque.js — Gestão de Estoque · Admin Bibliotroca
   ================================================================ */

let todosLivros = [];
let modoEdicao  = false;

/* ── DATA NO TOPBAR ── */
document.getElementById("dataHoje").textContent =
    new Date().toLocaleDateString("pt-BR", { weekday: "long", day: "2-digit", month: "long", year: "numeric" });

/* ── UTILS ── */
function promoValida(promocaoExpira) {
    if (!promocaoExpira) return true; // sem expiração = válida indefinidamente
    return new Date(promocaoExpira).getTime() > Date.now();
}

function primeiraFoto(fotosUrls) {
    try {
        const arr = JSON.parse(fotosUrls);
        if (Array.isArray(arr) && arr.length > 0) return arr[0];
    } catch (_) {}
    return null;
}

const ESTADO_LABEL = {
    NOVO: "Novo", OTIMO: "Ótimo", BOM: "Bom", DESGASTADO: "Desgastado", RUIM: "Ruim"
};

/* ── CARREGAR LIVROS ── */
async function carregarEstoque() {
    try {
        const res = await fetch("/api/admin/livros/aprovados");
        if (!res.ok) throw new Error();
        todosLivros = await res.json();
        renderGrid(todosLivros);
    } catch (_) {
        document.getElementById("estoqueGrid").innerHTML =
            `<p style="color:#722f37;grid-column:1/-1;padding:2rem;text-align:center">Erro ao carregar estoque.</p>`;
    }
}

function renderGrid(livros) {
    const tbody  = document.getElementById("estoqueBody");
    const vazio  = document.getElementById("estoqueVazio");
    const cont   = document.getElementById("contadorLivros");

    if (!tbody) return;

    cont.textContent = `${livros.length} livro${livros.length !== 1 ? "s" : ""}`;

    if (livros.length === 0) {
        tbody.innerHTML = "";
        vazio.style.display = "block";
        return;
    }
    vazio.style.display = "none";

    tbody.innerHTML = livros.map((l, idx) => {
        const foto   = l.fotoUrl || primeiraFoto(l.fotosUrls) || "";
        const estado = l.estadoAprovado || "BOM";
        const promoAtiva = l.emPromocao && l.precoOriginal != null && promoValida(l.promocaoExpira);

        // Preço
        let precoHtml;
        if (promoAtiva) {
            const desc = Math.round((1 - l.precoAprovado / l.precoOriginal) * 100);
            precoHtml = `
                <span class="preco-original" style="text-decoration:line-through;color:#9a8a80;font-size:.8rem;">
                    T$ ${Number(l.precoOriginal).toFixed(2)}
                </span><br>
                <span class="preco-promo" style="color:#e11d48;font-weight:700;">
                    T$ ${Number(l.precoAprovado).toFixed(2)}
                </span>`;
        } else {
            precoHtml = `<span style="font-weight:600;">
                T$ ${l.precoAprovado != null ? Number(l.precoAprovado).toFixed(2) : "—"}
            </span>`;
        }

        // Promoção
        let promoHtml = "—";
        if (promoAtiva) {
            const desc = Math.round((1 - l.precoAprovado / l.precoOriginal) * 100);
            promoHtml = `
                <span class="badge-promo-adm">${desc}% OFF</span>
                ${l.promocaoExpira ? `<br><small id="adm-timer-${l.id}" style="color:#e11d48;font-size:.72rem;font-weight:700;">...</small>` : ""}`;
        }

        // Capa miniatura
        const capaHtml = foto
            ? `<img src="${foto}" alt="${l.titulo}"
                    style="width:36px;height:52px;object-fit:cover;border-radius:3px;
                           box-shadow:0 2px 6px rgba(0,0,0,0.2);flex-shrink:0;"
                    onerror="this.style.display='none'">`
            : `<div style="width:36px;height:52px;background:#f0ebe4;border-radius:3px;
                           display:flex;align-items:center;justify-content:center;
                           font-size:1.1rem;">📚</div>`;

        return `
        <tr class="estoque-row" id="adm-card-${l.id}">
          <td class="col-num">${idx + 1}</td>
          <td class="col-livro">
            <div style="display:flex;align-items:center;gap:10px;">
              ${capaHtml}
              <div>
                <div class="row-titulo">${l.titulo}</div>
                <div class="row-autor">${l.autor || "—"}</div>
              </div>
            </div>
          </td>
          <td class="col-isbn">
            <span class="isbn-tag">${l.isbn || "—"}</span>
          </td>
          <td class="col-estado">
            <span class="card-estado estado-${estado}">
              ${ESTADO_LABEL[estado] || estado}
            </span>
          </td>
          <td class="col-preco" id="adm-preco-${l.id}">${precoHtml}</td>
          <td class="col-promo"
              data-livro-id="${l.id}"
              data-expira="${l.promocaoExpira || ""}"
              data-preco-original="${l.precoOriginal || ""}">
            ${promoHtml}
          </td>
          <td class="col-acoes">
            <button class="btn-editar btn-sm"
                    onclick='abrirModalEdit(${JSON.stringify(l)})'>
              <i class="fa-solid fa-pen"></i> Editar
            </button>
            <button class="btn-excluir btn-sm"
                    onclick="confirmarExclusao(${l.id}, '${l.titulo.replace(/'/g, "\\'")}')">
              <i class="fa-solid fa-trash"></i>
            </button>
          </td>
        </tr>`;
    }).join("");

    iniciarContadoresAdmin();
}

/* ── FILTRO ── */
function filtrar() {
    const q = document.getElementById("searchInput").value.toLowerCase();
    if (!q) { renderGrid(todosLivros); return; }
    renderGrid(todosLivros.filter(l =>
        (l.titulo || "").toLowerCase().includes(q) ||
        (l.autor  || "").toLowerCase().includes(q) ||
        (l.isbn   || "").toLowerCase().includes(q)
    ));
}

/* ── PROMO HELPERS ── */
function togglePromo() {
    const ativo = document.getElementById("fEmPromocao").checked;
    document.getElementById("promoSection").style.display = ativo ? "block" : "none";
    if (!ativo) {
        document.getElementById("fDesconto").value   = "";
        document.getElementById("fPrecoPromo").value = "";
        document.getElementById("fPromoExpira").value = "";
    }
}

function calcularPrecoPromo() {
    const preco    = parseFloat(document.getElementById("fPreco").value) || 0;
    const desconto = parseFloat(document.getElementById("fDesconto").value) || 0;
    const promoEl  = document.getElementById("fPrecoPromo");
    if (preco > 0 && desconto > 0 && desconto < 100) {
        promoEl.value = (preco * (1 - desconto / 100)).toFixed(2);
    } else {
        promoEl.value = "";
    }
}

function limparCamposPromo() {
    document.getElementById("fEmPromocao").checked  = false;
    document.getElementById("promoSection").style.display = "none";
    document.getElementById("fDesconto").value      = "";
    document.getElementById("fPrecoPromo").value    = "";
    document.getElementById("fPromoExpira").value   = "";
}

/* ── BUSCA ISBN VIA OPEN LIBRARY (mesmo padrão de venda_livro.js) ── */
// Guarda o último ISBN enviado à API para evitar sobrescrever com resposta desatualizada
let _isbnUltimoBuscado = "";

async function buscarIsbnModal() {
    const isbn = document.getElementById("fIsbn").value.replace(/\D/g, "");

    // Mesma condição que venda_livro.js: só busca com pelo menos 10 dígitos
    if (isbn.length < 10) return;

    // Evita buscar o mesmo ISBN repetidamente (ex.: oninput dispara em cada tecla)
    if (isbn === _isbnUltimoBuscado) return;
    _isbnUltimoBuscado = isbn;

    const statusEl = document.getElementById("fIsbnStatus");
    const tituloEl = document.getElementById("fTitulo");
    const autorEl  = document.getElementById("fAutor");

    // Mostra "Buscando..." como placeholder no campo Título enquanto aguarda
    const placeholderOriginal = tituloEl.placeholder;
    tituloEl.placeholder = "Buscando...";
    if (statusEl) statusEl.style.display = "none";

    try {
        // Mesmo endpoint e parâmetros usados em venda_livro.js
        const res  = await fetch(
            `https://openlibrary.org/api/books?bibkeys=ISBN:${isbn}&format=json&jscmd=data`
        );
        if (!res.ok) throw new Error(`HTTP ${res.status}`);

        const data = await res.json();
        const info = data[`ISBN:${isbn}`];

        if (info) {
            const titulo = info.title || "";
            // Mesmo mapeamento de venda_livro.js: authors[0].name
            const autor  = (info.authors && info.authors.length > 0) ? info.authors[0].name : "";

            if (!modoEdicao) {
                // Modo Adicionar: sempre preenche título e autor
                tituloEl.value = titulo;
                autorEl.value  = autor;
            } else {
                // Modo Editar: preenche apenas campos ainda vazios
                if (!tituloEl.value.trim()) tituloEl.value = titulo;
                if (!autorEl.value.trim())  autorEl.value  = autor;
            }

            if (statusEl && titulo) {
                statusEl.textContent = "✓ Informações encontradas automaticamente";
                statusEl.style.color = "#4a5d23";
                statusEl.style.display = "inline";
                setTimeout(() => { if (statusEl) statusEl.style.display = "none"; }, 2500);
            }
        }
        // Não encontrado → campos ficam em branco; sem mensagem de erro
    } catch (_) {
        // Falha de conexão ou API → silencioso, usuário preenche manualmente
    } finally {
        tituloEl.placeholder = placeholderOriginal;
    }
}

/* ── MODAL ADD ── */
function abrirModalAdd() {
    modoEdicao = false;
    document.getElementById("modalTitulo").textContent = "Adicionar Livro";
    document.getElementById("livroId").value = "";
    document.getElementById("fIsbn").value   = "";
    document.getElementById("fTitulo").value = "";
    document.getElementById("fAutor").value  = "";
    document.getElementById("fPreco").value  = "";
    document.getElementById("fEstado").value = "BOM";
    document.getElementById("fResumo").value = "";
    document.getElementById("fGenero").value = "";
    const statusEl = document.getElementById("fIsbnStatus");
    if (statusEl) statusEl.style.display = "none";
    limparCamposPromo();
    document.getElementById("btnSalvar").textContent = "Adicionar";
    esconderErro();
    abrirModal();
}

/* ── MODAL EDIT ── */
function abrirModalEdit(livro) {
    modoEdicao = true;
    document.getElementById("modalTitulo").textContent = "Editar Livro";
    document.getElementById("livroId").value = livro.id;
    document.getElementById("fTitulo").value = livro.titulo || "";
    document.getElementById("fAutor").value  = livro.autor  || "";
    document.getElementById("fIsbn").value   = livro.isbn   || "";
    document.getElementById("fEstado").value = livro.estadoAprovado || "BOM";
    document.getElementById("fResumo").value = livro.resumoOficial  || "";
    document.getElementById("fGenero").value = livro.genero || "";

    // Preço: se em promoção, mostra o preço original no campo
    const precoBase = livro.emPromocao && livro.precoOriginal != null
        ? livro.precoOriginal : (livro.precoAprovado != null ? livro.precoAprovado : "");
    document.getElementById("fPreco").value = precoBase;

    // Promo fields — só preenche se a promoção ainda estiver válida
    if (livro.emPromocao && livro.precoOriginal != null && promoValida(livro.promocaoExpira)) {
        const desconto = Math.round((1 - livro.precoAprovado / livro.precoOriginal) * 100);
        document.getElementById("fEmPromocao").checked           = true;
        document.getElementById("promoSection").style.display   = "block";
        document.getElementById("fDesconto").value              = desconto;
        document.getElementById("fPrecoPromo").value            = Number(livro.precoAprovado).toFixed(2);
        document.getElementById("fPromoExpira").value           = livro.promocaoExpira
            ? livro.promocaoExpira.substring(0, 16) : "";
    } else {
        limparCamposPromo();
    }

    document.getElementById("btnSalvar").textContent = "Salvar alterações";
    esconderErro();
    abrirModal();
}

function abrirModal() {
    document.getElementById("modal").classList.add("open");
    document.getElementById("modalOverlay").classList.add("open");
}

function fecharModal() {
    document.getElementById("modal").classList.remove("open");
    document.getElementById("modalOverlay").classList.remove("open");
}

function mostrarErro(msg) {
    const el = document.getElementById("modalErro");
    el.textContent = msg;
    el.style.display = "block";
}

function esconderErro() {
    document.getElementById("modalErro").style.display = "none";
}

/* ── SALVAR (ADD OU EDIT) ── */
async function salvarLivro(e) {
    e.preventDefault();
    esconderErro();

    const btn = document.getElementById("btnSalvar");
    btn.disabled = true;
    btn.textContent = "Salvando...";

    const emPromocao = document.getElementById("fEmPromocao").checked;
    const desconto   = parseFloat(document.getElementById("fDesconto").value) || 0;
    const promoExpiraRaw = document.getElementById("fPromoExpira").value;
    // datetime-local gives "YYYY-MM-DDTHH:mm", backend expects "YYYY-MM-DDTHH:mm:ss"
    const promocaoExpira = promoExpiraRaw ? promoExpiraRaw + ":00" : null;

    const payload = {
        titulo:              document.getElementById("fTitulo").value.trim(),
        autor:               document.getElementById("fAutor").value.trim(),
        isbn:                document.getElementById("fIsbn").value.trim(),
        preco:               parseFloat(document.getElementById("fPreco").value),
        estado:              document.getElementById("fEstado").value,
        resumo:              document.getElementById("fResumo").value.trim(),
        genero:              document.getElementById("fGenero").value.trim() || null,
        emPromocao:          emPromocao,
        percentualDesconto:  emPromocao ? desconto : null,
        promocaoExpira:      emPromocao ? promocaoExpira : null,
    };

    try {
        const id  = document.getElementById("livroId").value;
        const url = modoEdicao ? `/api/admin/livros/${id}` : "/api/admin/livros/novo";
        const method = modoEdicao ? "PUT" : "POST";

        const res = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });

        const data = await res.json().catch(() => ({}));

        if (!res.ok) {
            mostrarErro(typeof data === "string" ? data : (data.message || "Erro ao salvar."));
            btn.disabled = false;
            btn.textContent = modoEdicao ? "Salvar alterações" : "Adicionar";
            return;
        }

        btn.disabled = false;
        btn.textContent = modoEdicao ? "Salvar alterações" : "Adicionar";
        fecharModal();
        await carregarEstoque();

    } catch (_) {
        mostrarErro("Erro de conexão. Tente novamente.");
        btn.disabled = false;
        btn.textContent = modoEdicao ? "Salvar alterações" : "Adicionar";
    }
}

/* ── EXCLUIR ── */
function confirmarExclusao(id, titulo) {
    document.getElementById("confirmMsg").textContent =
        `Tem certeza que deseja remover "${titulo}" do estoque? Esta ação não pode ser desfeita.`;
    document.getElementById("btnConfirmDelete").onclick = () => excluirLivro(id);
    document.getElementById("confirmModal").classList.add("open");
    document.getElementById("confirmOverlay").classList.add("open");
}

function fecharConfirm() {
    document.getElementById("confirmModal").classList.remove("open");
    document.getElementById("confirmOverlay").classList.remove("open");
}

async function excluirLivro(id) {
    fecharConfirm();
    try {
        const res = await fetch(`/api/admin/livros/${id}`, { method: "DELETE" });
        if (res.ok) {
            todosLivros = todosLivros.filter(l => l.id !== id);
            renderGrid(todosLivros);
        }
    } catch (_) {}
}

/* ── CONTADOR REGRESSIVO (ADMIN) ── */
let admContadorInterval = null;

function iniciarContadoresAdmin() {
    if (admContadorInterval) clearInterval(admContadorInterval);

    const atualizar = () => {
        const agora = Date.now();
        document.querySelectorAll('[data-expira]').forEach(el => {
            if (!el.dataset.expira) return;
            const expira = new Date(el.dataset.expira).getTime();
            if (!expira) return;
            const diff = expira - agora;
            const id   = el.dataset.livroId;
            if (diff <= 0) {
                const badge = el.querySelector('.badge-promo-adm');
                if (badge) badge.style.display = 'none';
                const precoEl = document.getElementById('adm-preco-' + id);
                if (precoEl) {
                    const precoOriginal = parseFloat(el.dataset.precoOriginal) || 0;
                    precoEl.innerHTML = `T$ ${precoOriginal.toFixed(2)}`;
                }
            } else {
                const h = Math.floor(diff / 3600000);
                const m = Math.floor((diff % 3600000) / 60000);
                const s = Math.floor((diff % 60000) / 1000);
                const timerEl = document.getElementById('adm-timer-' + id);
                if (timerEl) timerEl.textContent = `${h}h ${m}m ${s}s`;
            }
        });
    };

    atualizar();
    admContadorInterval = setInterval(atualizar, 1000);
}

/* ── SIDEBAR MOBILE ── */
function abrirMenu() {
    document.getElementById("sidebar").classList.add("aberto");
    document.getElementById("overlay").style.display = "block";
}
function fecharMenu() {
    document.getElementById("sidebar").classList.remove("aberto");
    document.getElementById("overlay").style.display = "none";
}

/* ── INIT ── */
carregarEstoque();
