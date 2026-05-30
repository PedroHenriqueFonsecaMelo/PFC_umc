/* ================================================================
   venda_livro.js — Vender Livros · Bibliotroca
   ================================================================ */
let debounceTimer;
function debounce(func, delay) {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(func, delay);
}

(async function () {
    try {
        const res = await fetch("/clientes/meu-perfil-json", {
            credentials: "include",
        });
        if (!res.ok) return;
        const c = await res.json();
        const el = document.getElementById("navSaldo");
        if (el) el.textContent = "T$ " + (c.saldoTokens || 0).toFixed(2);
    } catch (_) {}
})();

const MAX = 10;
let livros = [];
let nextId = 1;

function criarCard(id, index) {
    const card = document.createElement("div");
    card.className = "book-card";
    card.dataset.id = id;
    card.innerHTML = `
        <div class="book-card-header">
            <span class="book-num">Livro #0${index}</span>
            <div class="book-actions">
                <button type="button" class="btn-text clear"  data-action="limpar"  data-id="${id}">Limpar</button>
                <button type="button" class="btn-text remove" data-action="remover" data-id="${id}">Remover</button>
            </div>
        </div>
        <div class="book-card-body">
            <div class="fields-row full">
                <div class="field">
                    <label for="isbn-${id}">ISBN</label>
                    <div class="field-wrap">
                        <input type="text" id="isbn-${id}" data-field="isbn" data-id="${id}"
                               placeholder="Ex: 9788532511010" maxlength="17" autocomplete="off"/>
                        <div class="spinner" id="spinner-${id}"></div>
                    </div>
                </div>
            </div>
            <div class="fields-row">
                <div class="field">
                    <label for="titulo-${id}">Título</label>
                    <div class="field-wrap">
                        <input type="text" id="titulo-${id}" data-field="titulo" data-id="${id}"
                               placeholder="Título do livro" required/>
                    </div>
                </div>
                <div class="field">
                    <label for="autor-${id}">Autor</label>
                    <div class="field-wrap">
                        <input type="text" id="autor-${id}" data-field="autor" data-id="${id}"
                               placeholder="Nome do autor" required/>
                    </div>
                </div>
            </div>
            <div class="fotos-area">
                <label class="fotos-label">
                    Fotos do livro <small>até 3 imagens · JPG ou PNG</small>
                </label>
                <div class="fotos-grid" id="fotos-grid-${id}">
                    <label class="foto-add-btn" for="foto-input-${id}" title="Adicionar foto">
                        <span class="foto-add-icon">+</span>
                    </label>
                </div>
                <input type="file" accept="image/*" multiple
                       id="foto-input-${id}" data-id="${id}" style="display:none"/>
            </div>
        </div>`;
    return card;
}

function adicionarLivro() {
    if (livros.length >= MAX) return;
    const id = nextId++;
    livros.push({
        id,
        isbn: "",
        titulo: "",
        autor: "",
        arquivos: [],
        quantidadedeFotos: 0,
    });
    const index = livros.length;
    const card = criarCard(id, index);
    document.getElementById("livrosContainer").appendChild(card);
    atualizarUI();
    card.scrollIntoView({ behavior: "smooth", block: "start" });
}

function removerLivro(id) {
    if (livros.length <= 1) return;
    livros = livros.filter((l) => l.id !== id);
    document.querySelector(`.book-card[data-id="${id}"]`).remove();
    document.querySelectorAll(".book-card").forEach((card, i) => {
        card.querySelector(".book-num").textContent = `Livro #0${i + 1}`;
    });
    atualizarUI();
}

function limparLivro(id) {
    const livro = livros.find((l) => l.id === id);
    if (!livro) return;
    livro.arquivos.forEach((f) => { f._dataUrl = null; });
    livro.isbn = "";
    livro.titulo = "";
    livro.autor = "";
    livro.arquivos = [];
    livro.quantidadedeFotos = 0;
    document.getElementById(`isbn-${id}`).value = "";
    document.getElementById(`titulo-${id}`).value = "";
    document.getElementById(`autor-${id}`).value = "";
    const inp = document.getElementById(`foto-input-${id}`);
    if (inp) inp.value = "";
    renderFotosGrid(id);
}

function atualizarUI() {
    document.getElementById("countAtual").textContent = livros.length;
    document.getElementById("btnAdicionar").disabled = livros.length >= MAX;
    document.querySelectorAll(".btn-text.remove").forEach((btn) => {
        btn.style.visibility = livros.length > 1 ? "visible" : "hidden";
    });
}

function setField(id, field, value) {
    const livro = livros.find((l) => l.id === id);
    if (livro) livro[field] = value;
}

async function buscarIsbn(id, isbnBruto) {
    const isbn = isbnBruto.replace(/\D/g, "");
    if (isbn.length < 10) return;
    const spinner = document.getElementById(`spinner-${id}`);
    spinner.classList.add("active");
    try {
        const res = await fetch(
            `https://openlibrary.org/api/books?bibkeys=ISBN:${isbn}&format=json&jscmd=data`,
        );
        const data = await res.json();
        const info = data[`ISBN:${isbn}`];
        if (info) {
            const titulo = info.title || "";
            const autor = info.authors ? info.authors[0].name : "";
            document.getElementById(`titulo-${id}`).value = titulo;
            document.getElementById(`autor-${id}`).value = autor;
            setField(id, "titulo", titulo);
            setField(id, "autor", autor);
        }
    } catch (_) {
    } finally {
        spinner.classList.remove("active");
    }
}

// Tipos e extensões aceitos (HEIC/HEIF não são processáveis no backend)
const TIPOS_ACEITOS  = ["image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"];
const EXT_ACEITAS    = /\.(jpe?g|png|webp|gif)$/i;
const TAMANHO_MAX_MB = 10;

function validarArquivo(file) {
    // Verifica extensão pelo nome (mais confiável que MIME em iOS)
    const nomeOk = !file.name || EXT_ACEITAS.test(file.name);
    const mimeOk = !file.type || TIPOS_ACEITOS.includes(file.type.toLowerCase());

    if (!nomeOk || !mimeOk) {
        // HEIC/HEIF comum em iPhones com "Alta Eficiência" ativado
        const isHeic = /\.(heic|heif)$/i.test(file.name) ||
                       ["image/heic","image/heif"].includes(file.type.toLowerCase());
        if (isHeic) {
            return `Formato HEIC/HEIF não suportado (${file.name}). No iPhone, vá em ` +
                   `Ajustes → Câmera → Formatos → "Mais Compatível" e tire a foto novamente.`;
        }
        return `Formato não aceito: ${file.name || file.type}. Use JPG, PNG ou WebP.`;
    }
    if (file.size > TAMANHO_MAX_MB * 1024 * 1024) {
        return `Arquivo muito grande: ${file.name} (${(file.size/1024/1024).toFixed(1)} MB). Máximo: ${TAMANHO_MAX_MB} MB.`;
    }
    return null; // ok
}

function renderFotosGrid(id) {
    const livro = livros.find((l) => l.id === id);
    if (!livro) return;
    const grid = document.getElementById(`fotos-grid-${id}`);
    if (!grid) return;
    grid.innerHTML = "";

    livro.arquivos.forEach((f, idx) => {
        const slot = document.createElement("div");
        slot.className = "foto-slot";

        const img = document.createElement("img");
        img.alt = "";
        // Usa dataURL já cacheado ou lê do arquivo
        if (f._dataUrl) {
            img.src = f._dataUrl;
        } else {
            const reader = new FileReader();
            reader.onload = (e) => {
                f._dataUrl = e.target.result;
                img.src = f._dataUrl;
            };
            reader.readAsDataURL(f);
        }

        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "foto-remove-btn";
        btn.title = "Remover foto";
        btn.textContent = "×";
        btn.onclick = () => removerFotoSlot(id, idx);

        slot.appendChild(img);
        slot.appendChild(btn);
        grid.appendChild(slot);
    });

    if (livro.arquivos.length < 3) {
        const label = document.createElement("label");
        label.className = "foto-add-btn";
        label.htmlFor = `foto-input-${id}`;
        label.title = livro.arquivos.length === 0
            ? "Adicionar fotos (até 3)"
            : "Adicionar mais uma foto";
        label.innerHTML = '<span class="foto-add-icon">+</span>';
        grid.appendChild(label);
    }
}

function handleFotos(id, files) {
    const livro = livros.find((l) => l.id === id);
    if (!livro) return;
    const vagasRestantes = 3 - livro.arquivos.length;
    if (vagasRestantes <= 0) return;

    let adicionados = 0;
    for (const file of Array.from(files)) {
        if (adicionados >= vagasRestantes) break;
        const erro = validarArquivo(file);
        if (erro) { mostrarErro(erro); continue; }
        livro.arquivos.push(file);
        adicionados++;
    }

    if (adicionados > 0) {
        livro.quantidadedeFotos = livro.arquivos.length;
        renderFotosGrid(id);
    }
}

function removerFotoSlot(id, idx) {
    const livro = livros.find((l) => l.id === id);
    if (!livro) return;
    livro.arquivos.splice(idx, 1);
    livro.quantidadedeFotos = livro.arquivos.length;
    renderFotosGrid(id);
}

document.getElementById("livrosContainer").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-action]");
    if (!btn) return;
    const id = Number(btn.dataset.id);
    if (btn.dataset.action === "remover") removerLivro(id);
    if (btn.dataset.action === "limpar") limparLivro(id);
});

document.getElementById("livrosContainer").addEventListener("input", (e) => {
    const el = e.target;
    if (!el.dataset.field) return;
    const id = Number(el.dataset.id);

    // Atualiza o valor no array local imediatamente
    setField(id, el.dataset.field, el.value);

    // Se for o campo ISBN, dispara a busca automática com debounce
    if (el.dataset.field === "isbn") {
        const isbnLimpo = el.value.replace(/\D/g, "");
        if (isbnLimpo.length >= 10) {
            debounce(() => {
                consultarBackendIsbn(id, isbnLimpo);
            }, 600); // 600ms de espera após parar de digitar
        }
    }
});

async function consultarBackendIsbn(id, isbn) {
    const spinner = document.getElementById(`spinner-${id}`);
    if (spinner) spinner.classList.add("active");

    try {
        // Ajuste a URL para bater com o seu @PostMapping do Java
        const res = await fetch(`/api/livros/cadastrar-isbn/${isbn}`, {
            method: "GET",
        });

        if (!res.ok) throw new Error("ISBN não encontrado");

        const data = await res.json();

        // Preenche os inputs na tela
        const inputTitulo = document.getElementById(`titulo-${id}`);
        const inputAutor = document.getElementById(`autor-${id}`);

        if (inputTitulo) inputTitulo.value = data.titulo;
        if (inputAutor) inputAutor.value = data.autor;

        setField(id, "titulo", data.titulo);
        setField(id, "autor", data.autor);
    } catch (err) {
        console.warn("Não foi possível auto-preencher os dados:", err);
    } finally {
        if (spinner) spinner.classList.remove("active");
    }
}

document.getElementById("livrosContainer").addEventListener("change", (e) => {
    const el = e.target;
    if (el.type !== "file" || !el.files.length) return;
    const id = Number(el.dataset.id);
    handleFotos(id, el.files);
    el.value = ""; // permite re-selecionar os mesmos arquivos
});

document.getElementById("btnAdicionar").addEventListener(
    "click",
    adicionarLivro,
);

document.getElementById("formVenda").addEventListener("submit", async (e) => {
    e.preventDefault();
    esconderAlertas();

    const invalidos = livros.filter((l) => !l.titulo.trim() || !l.autor.trim());
    if (invalidos.length > 0) {
        mostrarErro("Preencha título e autor de todos os livros.");
        return;
    }

    const semFoto = livros.find((l) => l.arquivos.length === 0);
    if (semFoto) {
        mostrarErro("Adicione pelo menos uma foto do livro antes de enviar.");
        return;
    }

    const btnSubmit = document.getElementById("btnSubmit");
    btnSubmit.disabled = true;
    btnSubmit.textContent = "Enviando…";

    const formData = new FormData();
    const loteJson = {
        livros: livros.map((l) => ({
            titulo: l.titulo.trim(),
            autor: l.autor.trim(),
            isbn: l.isbn.replace(/\D/g, ""),
            idioma: null,
            quantidadedeFotos: l.quantidadedeFotos,
        })),
    };
    formData.append(
        "loteDados",
        new Blob([JSON.stringify(loteJson)], { type: "application/json" }),
    );
    livros.forEach((l) =>
        l.arquivos.forEach((f) => formData.append("fotos", f))
    );

    try {
        const res = await fetch("/api/livros/lotes/vender", {
            method: "POST",
            body: formData,
        });
        if (res.ok) {
            const lote = await res.json();
            mostrarModalConfirmacao(lote);
        } else {
            const msg = await res.text();
            mostrarErro(msg || "Erro ao enviar o lote. Tente novamente.");
            btnSubmit.disabled = false;
            btnSubmit.textContent = "Enviar para avaliação";
        }
    } catch (_) {
        mostrarErro(
            "Erro de conexão. Verifique sua internet e tente novamente.",
        );
        btnSubmit.disabled = false;
        btnSubmit.textContent = "Enviar para avaliação";
    }
});

function mostrarErro(msg) {
    const el = document.getElementById("alertErro");
    el.innerHTML = msg;
    el.classList.add("show");
    el.scrollIntoView({ behavior: "smooth", block: "center" });
}
function mostrarOk(msg) {
    const el = document.getElementById("alertOk");
    el.innerHTML = msg;
    el.classList.add("show");
}
function esconderAlertas() {
    document.getElementById("alertErro").classList.remove("show");
    document.getElementById("alertOk").classList.remove("show");
}

/* ── Comprovante do lote ── */

function escHtml(str) {
    return String(str || "")
        .replace(/&/g, "&amp;").replace(/</g, "&lt;")
        .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function mostrarModalConfirmacao(lote) {
    const protocolo = lote.codigoProtocolo || String(lote.id || "—");
    const numLote = lote.id ? "Lote #" + lote.id : "";
    const data = new Date().toLocaleDateString("pt-BR", {
        day: "2-digit",
        month: "long",
        year: "numeric",
    });

    document.getElementById("recProtocolo").textContent = protocolo;
    document.getElementById("recNumLote").textContent = numLote;
    document.getElementById("recData").textContent = data;

    document.getElementById("recLivros").innerHTML = livros.map((l, i) => {
        const sub = [
            l.autor ? escHtml(l.autor) : null,
            l.isbn ? "ISBN: " + l.isbn.replace(/\D/g, "") : null,
        ].filter(Boolean).join(" · ");
        return `<div class="rec-livro-item">
            <span class="rec-livro-num">${String(i + 1).padStart(2, "0")}</span>
            <div>
                <div class="rec-livro-titulo">${escHtml(l.titulo || "—")}</div>
                ${sub ? `<div class="rec-livro-sub">${sub}</div>` : ""}
            </div>
        </div>`;
    }).join("");

    // Espelha o recibo na área de impressão
    document.getElementById("printArea").innerHTML =
        document.getElementById("recibo").innerHTML;

    document.getElementById("modalConfirmacao").style.display = "flex";
    document.body.style.overflow = "hidden";
}

document.getElementById("btnPdf").addEventListener(
    "click",
    () => window.print(),
);

document.getElementById("btnConcluir").addEventListener("click", () => {
    window.location.href = "/?enviado=1";
});

/* ─────────────────────────────────────────────────────────── */

adicionarLivro();
