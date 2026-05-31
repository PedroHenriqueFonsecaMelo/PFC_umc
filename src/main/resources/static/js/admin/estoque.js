/* ================================================================
   estoque.js — Gestão de Estoque · Admin Bibliotroca
   ================================================================ */

let todosLivros = [];
let modoEdicao = false;

/* ── DATA NO TOPBAR ── */
document.getElementById("dataHoje").textContent = new Date().toLocaleDateString(
    "pt-BR",
    { weekday: "long", day: "2-digit", month: "long", year: "numeric" },
);

/* ── UTILS ── */
function promoValida(promocaoExpira) {
    if (!promocaoExpira) return true;
    // Jackson pode serializar LocalDateTime como array [ano,mes,dia,h,m,s] ou como string ISO
    if (Array.isArray(promocaoExpira)) {
        const [y, mo, d, h = 0, m = 0, s = 0] = promocaoExpira;
        return new Date(y, mo - 1, d, h, m, s).getTime() > Date.now();
    }
    return new Date(promocaoExpira).getTime() > Date.now();
}

function primeiraFoto(fotosUrls) {
    if (!fotosUrls || fotosUrls === "[]") return null;
    try {
        const arr = JSON.parse(fotosUrls);
        if (Array.isArray(arr) && arr.length > 0) return arr[0];
    } catch (_) {
        // String não é JSON: assume que é URL direta
        if (fotosUrls.startsWith("http")) return fotosUrls;
    }
    return null;
}

const ESTADO_LABEL = {
    NOVO: "Novo",
    OTIMO: "Ótimo",
    BOM: "Bom",
    DESGASTADO: "Desgastado",
    RUIM: "Ruim",
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
    const tbody = document.getElementById("estoqueBody");
    const vazio = document.getElementById("estoqueVazio");
    const cont = document.getElementById("contadorLivros");

    if (!tbody) return;

    cont.textContent = `${livros.length} livro${
        livros.length !== 1 ? "s" : ""
    }`;

    if (livros.length === 0) {
        tbody.innerHTML = "";
        vazio.style.display = "block";
        return;
    }
    vazio.style.display = "none";

    tbody.innerHTML = livros.map((l, idx) => {
        const foto = primeiraFoto(l.fotosUrls) || primeiraFoto(l.fotoUrl) ||
            null;
        const estado = l.estadoAprovado || "BOM";
        const promoAtiva = l.emPromocao && l.precoOriginal != null &&
            promoValida(l.promocaoExpira);

        // Preço
        let precoHtml;
        if (promoAtiva) {
            const desc = Math.round(
                (1 - l.precoAprovado / l.precoOriginal) * 100,
            );
            precoHtml = `
                <span class="preco-original" style="text-decoration:line-through;color:#9a8a80;font-size:.8rem;">
                    T$ ${Number(l.precoOriginal).toFixed(2)}
                </span><br>
                <span class="preco-promo" style="color:#e11d48;font-weight:700;">
                    T$ ${Number(l.precoAprovado).toFixed(2)}
                </span>`;
        } else {
            precoHtml = `<span style="font-weight:600;">
                T$ ${
                l.precoAprovado != null
                    ? Number(l.precoAprovado).toFixed(2)
                    : "—"
            }
            </span>`;
        }

        // Promoção
        let promoHtml = "—";
        if (promoAtiva) {
            const desc = Math.round(
                (1 - l.precoAprovado / l.precoOriginal) * 100,
            );
            promoHtml = `
                <span class="badge-promo-adm">${desc}% OFF</span>
                ${
                l.promocaoExpira
                    ? `<br><small id="adm-timer-${l.id}" style="color:#e11d48;font-size:.72rem;font-weight:700;">...</small>`
                    : ""
            }`;
        }

        // Capa miniatura
        const capaHtml = foto
            ? `<img src="${foto}" alt="${l.titulo}"
                    style="width:56px;height:76px;object-fit:cover;border-radius:5px;
                           box-shadow:0 2px 8px rgba(0,0,0,0.18);flex-shrink:0;
                           border:1px solid rgba(0,0,0,0.08);"
                    onerror="this.outerHTML='<div style=&quot;width:56px;height:76px;background:#f0ebe4;border-radius:5px;display:flex;align-items:center;justify-content:center;font-size:1.5rem;flex-shrink:0;&quot;>📚</div>'">`
            : `<div style="width:56px;height:76px;background:#f0ebe4;border-radius:5px;
                           display:flex;align-items:center;justify-content:center;
                           font-size:1.5rem;flex-shrink:0;">📚</div>`;

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
                    onclick="confirmarExclusao(${l.id}, '${
            l.titulo.replace(/'/g, "\\'")
        }')">
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
    if (!q) {
        renderGrid(todosLivros);
        return;
    }
    renderGrid(
        todosLivros.filter((l) =>
            (l.titulo || "").toLowerCase().includes(q) ||
            (l.autor || "").toLowerCase().includes(q) ||
            (l.isbn || "").toLowerCase().includes(q)
        ),
    );
}

/* ── MÁSCARA E VALIDAÇÃO DA DATA DE PROMOÇÃO ── */
function mascaraDataHora(input) {
    var digits = input.value.replace(/\D/g, "").substring(0, 12);
    var out = "";
    if (digits.length > 0) out = digits.substring(0, 2);
    if (digits.length > 2) out += "/" + digits.substring(2, 4);
    if (digits.length > 4) out += "/" + digits.substring(4, 8);
    if (digits.length > 8) out += " " + digits.substring(8, 10);
    if (digits.length > 10) out += ":" + digits.substring(10, 12);
    input.value = out;
    validarDataPromoInput(out);
}

function validarDataPromoInput(valor) {
    var erroEl = document.getElementById("fPromoExpiraErro");
    if (!erroEl) return true;
    if (!valor || valor.length < 16) {
        erroEl.style.display = "none";
        return true;
    }
    var iso = maskedParaIso(valor);
    if (!iso) {
        erroEl.style.display = "none";
        return true;
    }
    var data = new Date(iso);
    if (isNaN(data.getTime())) {
        erroEl.textContent = "Data inválida.";
        erroEl.style.display = "block";
        return false;
    }
    if (data <= new Date()) {
        erroEl.textContent = "A data deve ser futura.";
        erroEl.style.display = "block";
        return false;
    }
    erroEl.style.display = "none";
    return true;
}

function maskedParaIso(masked) {
    var m = masked.match(/^(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2})$/);
    if (!m) return null;
    return m[3] + "-" + m[2] + "-" + m[1] + "T" + m[4] + ":" + m[5] + ":00";
}

function isoParaMasked(iso) {
    if (!iso) return "";
    // Se vier como array do Jackson: [ano, mes, dia, hora, minuto, ...]
    if (Array.isArray(iso)) {
        const [y, mo, d, h = 0, mi = 0] = iso;
        const pad = (n) => String(n).padStart(2, "0");
        return pad(d) + "/" + pad(mo) + "/" + y + " " + pad(h) + ":" + pad(mi);
    }
    var m = iso.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
    if (!m) return "";
    return m[3] + "/" + m[2] + "/" + m[1] + " " + m[4] + ":" + m[5];
}

/* ── PROMO HELPERS ── */
function togglePromo() {
    const ativo = document.getElementById("fEmPromocao").checked;
    document.getElementById("promoSection").style.display = ativo
        ? "block"
        : "none";
    if (!ativo) {
        document.getElementById("fDesconto").value = "";
        document.getElementById("fPrecoPromo").value = "";
        document.getElementById("fPromoExpira").value = "";
    }
}

function calcularPrecoPromo() {
    const preco = parseFloat(document.getElementById("fPreco").value) || 0;
    const desconto = parseFloat(document.getElementById("fDesconto").value) ||
        0;
    const promoEl = document.getElementById("fPrecoPromo");
    if (preco > 0 && desconto > 0 && desconto < 100) {
        promoEl.value = (preco * (1 - desconto / 100)).toFixed(2);
    } else {
        promoEl.value = "";
    }
}

function limparCamposPromo() {
    document.getElementById("fEmPromocao").checked = false;
    document.getElementById("promoSection").style.display = "none";
    document.getElementById("fDesconto").value = "";
    document.getElementById("fPrecoPromo").value = "";
    document.getElementById("fPromoExpira").value = "";
}

/* ── FOTOS DO MODAL ── */
let modalFotos = []; // { url: string, uploading: boolean }
const MODAL_MAX_FOTOS = 3;

function renderModalFotos() {
    const grid = document.getElementById("modalFotosGrid");
    if (!grid) return;
    grid.innerHTML = "";

    modalFotos.forEach((foto, idx) => {
        const slot = document.createElement("div");
        slot.className = "modal-foto-slot";
        if (foto.uploading) {
            slot.innerHTML = `<div class="modal-foto-uploading">
                <i class="fa-solid fa-spinner fa-spin"></i></div>`;
        } else {
            slot.innerHTML = `
                <img src="${foto.url}" alt="foto"
                     onerror="this.style.opacity='.3'" />
                <button type="button" class="modal-foto-remove"
                        onclick="removerFotoModal(${idx})">×</button>`;
        }
        grid.appendChild(slot);
    });

    const temUpload = modalFotos.some((f) => f.uploading);
    if (modalFotos.length < MODAL_MAX_FOTOS && !temUpload) {
        const label = document.createElement("label");
        label.className = "modal-foto-add";
        label.htmlFor = "modalFotoInput";
        label.title = "Adicionar foto";
        label.innerHTML = `<i class="fa-solid fa-plus"></i>`;
        grid.appendChild(label);
    }
}

async function handleModalFotos(files) {
    const input = document.getElementById("modalFotoInput");
    if (input) input.value = "";

    const vagas = MODAL_MAX_FOTOS -
        modalFotos.filter((f) => !f.uploading).length;
    if (vagas <= 0) return;

    const lista = Array.from(files).slice(0, vagas);
    for (const file of lista) {
        const placeholder = { url: "", uploading: true };
        modalFotos.push(placeholder);
        renderModalFotos();

        try {
            const fd = new FormData();
            fd.append("foto", file);
            const res = await fetch("/api/admin/livros/upload-foto", {
                method: "POST",
                credentials: "include",
                body: fd,
            });
            if (!res.ok) throw new Error();
            const data = await res.json();
            placeholder.url = data.url;
            placeholder.uploading = false;
        } catch (_) {
            modalFotos.splice(modalFotos.indexOf(placeholder), 1);
            mostrarErro("Erro ao enviar foto. Tente novamente.");
        }
        renderModalFotos();
    }
}

function removerFotoModal(idx) {
    modalFotos.splice(idx, 1);
    renderModalFotos();
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
    const autorEl = document.getElementById("fAutor");

    const placeholderOriginal = tituloEl.placeholder;
    tituloEl.placeholder = "Buscando...";
    if (statusEl) statusEl.style.display = "none";

    try {
        const res = await fetch(
            `https://openlibrary.org/api/books?bibkeys=ISBN:${isbn}&format=json&jscmd=data`,
        );
        if (!res.ok) throw new Error(`HTTP ${res.status}`);

        const data = await res.json();
        const info = data[`ISBN:${isbn}`];

        if (info) {
            const titulo = info.title || "";
            const autor = (info.authors && info.authors.length > 0)
                ? info.authors[0].name
                : "";

            if (!modoEdicao) {
                tituloEl.value = titulo;
                autorEl.value = autor;
            } else {
                if (!tituloEl.value.trim()) tituloEl.value = titulo;
                if (!autorEl.value.trim()) autorEl.value = autor;
            }

            if (statusEl && titulo) {
                statusEl.textContent =
                    "✓ Informações encontradas automaticamente";
                statusEl.style.color = "#4a5d23";
                statusEl.style.display = "inline";
                setTimeout(() => {
                    if (statusEl) statusEl.style.display = "none";
                }, 2500);
            }
        }
    } catch (_) {
        // Falha de conexão → silencioso
    } finally {
        tituloEl.placeholder = placeholderOriginal;
    }
}

/* ── MODAL ADD ── */
function abrirModalAdd() {
    modoEdicao = false;
    document.getElementById("modalTitulo").textContent = "Adicionar Livro";
    document.getElementById("livroId").value = "";
    document.getElementById("fIsbn").value = "";
    document.getElementById("fTitulo").value = "";
    document.getElementById("fAutor").value = "";
    document.getElementById("fPreco").value = "";
    document.getElementById("fEstado").value = "BOM";
    document.getElementById("fResumo").value = "";
    document.getElementById("fGenero").value = "";
    modalFotos = [];
    renderModalFotos();
    _isbnUltimoBuscado = "";
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
    document.getElementById("fAutor").value = livro.autor || "";
    document.getElementById("fIsbn").value = livro.isbn || "";
    document.getElementById("fEstado").value = livro.estadoAprovado || "BOM";
    document.getElementById("fResumo").value = livro.resumoOficial || "";
    document.getElementById("fGenero").value = livro.genero || "";

    // Preço: se em promoção, mostra o preço original no campo
    const precoBase = livro.emPromocao && livro.precoOriginal != null
        ? livro.precoOriginal
        : (livro.precoAprovado != null ? livro.precoAprovado : "");
    document.getElementById("fPreco").value = precoBase;

    // Promo fields — só preenche se a promoção ainda estiver válida
    if (
        livro.emPromocao && livro.precoOriginal != null &&
        promoValida(livro.promocaoExpira)
    ) {
        const desconto = Math.round(
            (1 - livro.precoAprovado / livro.precoOriginal) * 100,
        );
        document.getElementById("fEmPromocao").checked = true;
        document.getElementById("promoSection").style.display = "block";
        document.getElementById("fDesconto").value = desconto;
        document.getElementById("fPrecoPromo").value = Number(
            livro.precoAprovado,
        ).toFixed(2);
        document.getElementById("fPromoExpira").value = isoParaMasked(
            livro.promocaoExpira,
        );
    } else {
        limparCamposPromo();
    }

    // Popula fotos existentes do livro
    modalFotos = [];
    try {
        const arr = JSON.parse(livro.fotosUrls || "[]");
        if (Array.isArray(arr)) {
            arr.forEach((url) => modalFotos.push({ url, uploading: false }));
        }
    } catch (_) {
        if (livro.fotosUrls && livro.fotosUrls.startsWith("http")) {
            modalFotos.push({ url: livro.fotosUrls, uploading: false });
        }
    }
    renderModalFotos();

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
    const desconto = parseFloat(document.getElementById("fDesconto").value) ||
        0;
    const promoExpiraRaw = document.getElementById("fPromoExpira").value.trim();

    if (emPromocao && desconto <= 0) {
        mostrarErro(
            "Informe o percentual de desconto (deve ser maior que 0%).",
        );
        btn.disabled = false;
        btn.textContent = modoEdicao ? "Salvar alterações" : "Adicionar";
        return;
    }

    if (emPromocao && !promoExpiraRaw) {
        mostrarErro(
            "Informe a data de validade da promoção (DD/MM/AAAA HH:MM).",
        );
        btn.disabled = false;
        btn.textContent = modoEdicao ? "Salvar alterações" : "Adicionar";
        return;
    }

    let promocaoExpira = null;
    if (promoExpiraRaw) {
        if (promoExpiraRaw.length < 16) {
            mostrarErro(
                "Preencha a data de validade completa (DD/MM/AAAA HH:MM).",
            );
            btn.disabled = false;
            btn.textContent = modoEdicao ? "Salvar alterações" : "Adicionar";
            return;
        }
        const isoConvertido = maskedParaIso(promoExpiraRaw);
        if (!isoConvertido) {
            mostrarErro("Data de validade inválida.");
            btn.disabled = false;
            btn.textContent = modoEdicao ? "Salvar alterações" : "Adicionar";
            return;
        }
        if (new Date(isoConvertido) <= new Date()) {
            mostrarErro("A data de validade da promoção deve ser futura.");
            btn.disabled = false;
            btn.textContent = modoEdicao ? "Salvar alterações" : "Adicionar";
            return;
        }
        promocaoExpira = isoConvertido;
    }

    const urlsFotos = modalFotos.filter((f) => !f.uploading && f.url).map((f) =>
        f.url
    );
    const capa = urlsFotos.length > 0 ? JSON.stringify(urlsFotos) : null;

    const payload = {
        titulo: document.getElementById("fTitulo").value.trim(),
        autor: document.getElementById("fAutor").value.trim(),
        isbn: document.getElementById("fIsbn").value.trim(),
        preco: parseFloat(document.getElementById("fPreco").value),
        estado: document.getElementById("fEstado").value,
        resumo: document.getElementById("fResumo").value.trim(),
        genero: document.getElementById("fGenero").value.trim() || null,
        capa: capa,
        emPromocao: emPromocao,
        percentualDesconto: emPromocao ? desconto : null,
        promocaoExpira: emPromocao ? promocaoExpira : null,
    };

    try {
        const id = document.getElementById("livroId").value;
        const url = modoEdicao
            ? `/api/admin/livros/${id}`
            : "/api/admin/livros/novo";
        const method = modoEdicao ? "PUT" : "POST";

        const res = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });

        const data = await res.json().catch(() => ({}));

        if (!res.ok) {
            mostrarErro(
                typeof data === "string"
                    ? data
                    : (data.message || "Erro ao salvar."),
            );
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
    document.getElementById("btnConfirmDelete").onclick = () =>
        excluirLivro(id);
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
        const res = await fetch(`/api/admin/livros/${id}`, {
            method: "DELETE",
        });
        if (res.ok) {
            todosLivros = todosLivros.filter((l) => l.id !== id);
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
        document.querySelectorAll("[data-expira]").forEach((el) => {
            if (!el.dataset.expira) return;
            const expira = new Date(el.dataset.expira).getTime();
            if (!expira) return;
            const diff = expira - agora;
            const id = el.dataset.livroId;
            if (diff <= 0) {
                const badge = el.querySelector(".badge-promo-adm");
                if (badge) badge.style.display = "none";
                const precoEl = document.getElementById("adm-preco-" + id);
                if (precoEl) {
                    const precoOriginal =
                        parseFloat(el.dataset.precoOriginal) || 0;
                    precoEl.innerHTML = `T$ ${precoOriginal.toFixed(2)}`;
                }
            } else {
                const h = Math.floor(diff / 3600000);
                const m = Math.floor((diff % 3600000) / 60000);
                const s = Math.floor((diff % 60000) / 1000);
                const timerEl = document.getElementById("adm-timer-" + id);
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
