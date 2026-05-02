/* ================================================================
   venda_livro.js — Vender Livros · Bibliotroca
   ================================================================ */

(async function () {
    try {
        const res = await fetch("/clientes/meu-perfil-json", { credentials: "include" });
        if (!res.ok) return;
        const c = await res.json();
        const el = document.getElementById("navSaldo");
        if (el) el.textContent = "T$ " + (c.saldoTokens || 0).toFixed(2);
    } catch (_) {}
})();

const MAX = 5;
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
            <div class="upload-area" id="upload-area-${id}">
                <input type="file" multiple accept="image/*" id="fotos-${id}" data-id="${id}"/>
                <div class="upload-placeholder" id="upload-placeholder-${id}">
                    Clique para adicionar fotos do livro
                    <small>até 3 imagens · JPG ou PNG</small>
                </div>
                <div class="upload-previews" id="upload-previews-${id}" style="display:none"></div>
            </div>
        </div>`;
    return card;
}

function adicionarLivro() {
    if (livros.length >= MAX) return;
    const id = nextId++;
    livros.push({ id, isbn: "", titulo: "", autor: "", arquivos: [], quantidadedeFotos: 0 });
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
    livro.isbn = "";
    livro.titulo = "";
    livro.autor = "";
    livro.arquivos = [];
    livro.quantidadedeFotos = 0;
    document.getElementById(`isbn-${id}`).value = "";
    document.getElementById(`titulo-${id}`).value = "";
    document.getElementById(`autor-${id}`).value = "";
    document.getElementById(`fotos-${id}`).value = "";
    document.getElementById(`upload-placeholder-${id}`).style.display = "";
    const prev = document.getElementById(`upload-previews-${id}`);
    prev.innerHTML = "";
    prev.style.display = "none";
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

function handleFotos(id, files) {
    const selecionados = Array.from(files).slice(0, 3);
    const livro = livros.find((l) => l.id === id);
    if (livro) {
        livro.arquivos = selecionados;
        livro.quantidadedeFotos = selecionados.length;
    }
    const placeholder = document.getElementById(`upload-placeholder-${id}`);
    const previews = document.getElementById(`upload-previews-${id}`);
    if (selecionados.length > 0) {
        placeholder.style.display = "none";
        previews.style.display = "flex";
        previews.innerHTML = selecionados.map((f) => {
            const url = URL.createObjectURL(f);
            return `<img src="${url}" alt="preview"/>`;
        }).join("");
    } else {
        placeholder.style.display = "";
        previews.style.display = "none";
        previews.innerHTML = "";
    }
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
    setField(id, el.dataset.field, el.value);
    if (el.dataset.field === "isbn") buscarIsbn(id, el.value);
});

document.getElementById("livrosContainer").addEventListener("change", (e) => {
    const el = e.target;
    if (el.type !== "file") return;
    const id = Number(el.dataset.id);
    handleFotos(id, el.files);
});

document.getElementById("btnAdicionar").addEventListener("click", adicionarLivro);

document.getElementById("formVenda").addEventListener("submit", async (e) => {
    e.preventDefault();
    esconderAlertas();

    const invalidos = livros.filter((l) => !l.titulo.trim() || !l.autor.trim());
    if (invalidos.length > 0) {
        mostrarErro("Preencha título e autor de todos os livros.");
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
            quantidadedeFotos: l.quantidadedeFotos,
        })),
    };
    formData.append("loteDados", new Blob([JSON.stringify(loteJson)], { type: "application/json" }));
    livros.forEach((l) => l.arquivos.forEach((f) => formData.append("fotos", f)));

    try {
        const res = await fetch("/api/livros/lotes/vender", { method: "POST", body: formData });
        if (res.ok) {
            mostrarOk("Lote enviado! Nossa curadoria avaliará em breve.");
            setTimeout(() => window.location.href = "/", 2000);
        } else {
            const msg = await res.text();
            mostrarErro(msg || "Erro ao enviar o lote. Tente novamente.");
            btnSubmit.disabled = false;
            btnSubmit.textContent = "Enviar para avaliação";
        }
    } catch (_) {
        mostrarErro("Erro de conexão. Verifique sua internet e tente novamente.");
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

adicionarLivro();
