/* ================================================================
   clientes.js — Admin · Bibliotroca
   ================================================================ */

let _todos = [];
let _filtroAtivo = "todos";
let _paginaAtual = 0;
let _ultimaPagina = false;

/* ── UTILS ──────────────────────────────────────────────────── */
function fmtData(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleDateString("pt-BR", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    });
}

function fmtTokens(v) {
    if (v == null) return "0";
    return Number(v).toLocaleString("pt-BR", {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
    });
}

function inicial(nome) {
    if (!nome) return "?";
    return nome.trim().charAt(0).toUpperCase();
}

function mostrarToast(cls, msg) {
    const t = document.getElementById("toast");
    if (!t) return;
    t.className = "toast " + cls;
    t.textContent = msg;
    t.style.display = "block";
    setTimeout(() => {
        t.style.display = "none";
    }, 4000);
}

function abrirMenu() {
    document.getElementById("sidebar")?.classList.add("open");
    document.getElementById("overlay")?.classList.add("show");
}
function fecharMenu() {
    document.getElementById("sidebar")?.classList.remove("open");
    document.getElementById("overlay")?.classList.remove("show");
}

/* ── FETCH & PAGINAÇÃO ──────────────────────────────────────── */

// 💡 Função chamada ao iniciar a tela ou clicar em "Atualizar"
function carregarDados() {
    _paginaAtual = 0;
    _ultimaPagina = false;
    _todos = []; // Reseta o acumulador

    const card = document.getElementById("tableCard");
    card.innerHTML =
        '<div class="loading-placeholder"><i class="fa-solid fa-spinner fa-spin"></i> Carregando clientes...</div>';

    buscarPaginaDoServidor();
}

// 💡 Função executada ao clicar no botão "Carregar mais" do HTML
function carregarProximaPagina() {
    if (_ultimaPagina) return;
    _paginaAtual++;
    
    // Altera o estado visual do botão para indicar carregamento
    const btn = document.getElementById("btnCarregarMais");
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Carregando...';
    }
    
    buscarPaginaDoServidor();
}

function buscarPaginaDoServidor() {
    // 💡 Passa a página atual e o tamanho do bloco (20 registros)
    fetch(`/api/admin/clientes?page=${_paginaAtual}&size=20`, { credentials: "include" })
        .then((r) => {
            if (!r.ok) throw new Error("Falha ao carregar clientes");
            return r.json();
        })
        .then((pageData) => {
            // pageData agora é o objeto do Spring Page (contém content, last, totalElements, etc.)
            const novosClientes = pageData.content || [];
            _ultimaPagina = pageData.last; 

            // Junta os novos registros aos que já haviam sido carregados antes
            _todos = _todos.concat(novosClientes);

            // Executa os filtros em cima da base atualizada e renderiza
            aplicarFiltros();
            
            // Atualiza os contadores na tela baseado no total do banco de dados
            const total = document.getElementById("totalLabel");
            if (total) {
                const totalBanco = pageData.totalElements || _todos.length;
                total.textContent = `${totalBanco} cliente${totalBanco !== 1 ? "s" : ""} cadastrado${totalBanco !== 1 ? "s" : ""}`;
            }

            // Gerencia a exibição do container e botão de paginação adicionado no HTML
            atualizarPainelPaginacao(_todos.length, pageData.totalElements);
        })
        .catch((err) => {
            const card = document.getElementById("tableCard");
            if (_paginaAtual === 0) {
                card.innerHTML =
                    '<div class="empty-state"><i class="fa-solid fa-triangle-exclamation"></i><p>Erro ao carregar dados. Tente novamente.</p></div>';
            }
            mostrarToast("toast-err", "Erro ao carregar clientes.");
            console.error(err);
        });
}

function atualizarPainelPaginacao(carregados, totalGeral) {
    const container = document.getElementById("paginacaoContainer");
    const btn = document.getElementById("btnCarregarMais");
    const status = document.getElementById("statusPaginacao");

    if (!container) return;

    // Se não há registros ou já chegou na última página, gerencia a visibilidade
    if (totalGeral === 0) {
        container.style.display = "none";
        return;
    }

    container.style.display = "block";

    if (btn) {
        btn.disabled = _ultimaPagina;
        btn.style.display = _ultimaPagina ? "none" : "inline-flex";
        btn.innerHTML = '<i class="fa-solid fa-plus"></i> Carregar mais clientes';
    }

    if (status) {
        status.textContent = `Exibindo ${carregados} de ${totalGeral} clientes`;
    }
}

/* ── FILTROS (Mantidos idênticos, mas operando no lote acumulado) ── */
function setFiltro(filtro) {
    _filtroAtivo = filtro;
    document.querySelectorAll(".filter-tab").forEach((el) => {
        el.classList.toggle("ativo", el.dataset.filtro === filtro);
    });
    aplicarFiltros();
}

function toggleFiltros() {
    const panel = document.getElementById("filtrosPanel");
    const btn = document.getElementById("btnFiltros");
    const aberto = panel.style.display !== "none";
    panel.style.display = aberto ? "none" : "block";
    btn.style.borderColor = aberto ? "rgba(44,36,27,.2)" : "#722f37";
    btn.style.color = aberto ? "#2c241b" : "#722f37";
}

function atualizarPills() {
    const pills = document.getElementById("filtrosPills");
    const badge = document.getElementById("filtrosBadge");
    if (!pills) return;

    const ativos = [];
    const nivel = document.getElementById("filtroNivel")?.value;
    const saldoMin = document.getElementById("filtroSaldoMin")?.value;
    const saldoMax = document.getElementById("filtroSaldoMax")?.value;
    const comprasMin = document.getElementById("filtroComprasMin")?.value;
    const comprasMax = document.getElementById("filtroComprasMax")?.value;
    const cadInicio = document.getElementById("filtroCadastroInicio")?.value;
    const cadFim = document.getElementById("filtroCadastroFim")?.value;

    if (nivel) ativos.push({ label: `Nível: ${nivel}`, id: "filtroNivel" });
    if (saldoMin) ativos.push({ label: `Saldo ≥ ${saldoMin}`, id: "filtroSaldoMin" });
    if (saldoMax) ativos.push({ label: `Saldo ≤ ${saldoMax}`, id: "filtroSaldoMax" });
    if (comprasMin) ativos.push({ label: `Compras ≥ ${comprasMin}`, id: "filtroComprasMin" });
    if (comprasMax) ativos.push({ label: `Compras ≤ ${comprasMax}`, id: "filtroComprasMax" });
    const cuponsMin = document.getElementById("filtroCuponsMin")?.value;
    const cuponsMax = document.getElementById("filtroCuponsMax")?.value;
    if (cuponsMin) ativos.push({ label: `Cupons ≥ ${cuponsMin}`, id: "filtroCuponsMin" });
    if (cuponsMax) ativos.push({ label: `Cupons ≤ ${cuponsMax}`, id: "filtroCuponsMax" });
    if (cadInicio) ativos.push({ label: `De: ${cadInicio}`, id: "filtroCadastroInicio" });
    if (cadFim) ativos.push({ label: `Até: ${cadFim}`, id: "filtroCadastroFim" });

    pills.innerHTML = ativos.map(f => `
        <span style="display:inline-flex;align-items:center;gap:.3rem;
            background:#f0e8e8;color:#722f37;border-radius:20px;
            padding:.25rem .65rem;font-size:.75rem;font-weight:600;">
            ${f.label}
            <button onclick="document.getElementById('${f.id}').value='';aplicarFiltros()"
                style="background:none;border:none;cursor:pointer;color:#722f37;
                font-size:.8rem;padding:0;line-height:1;">✕</button>
        </span>
    `).join("");

    if (badge) {
        badge.textContent = ativos.length;
        badge.style.display = ativos.length > 0 ? "inline" : "none";
    }
}

function aplicarFiltros() {
    const busca = (document.getElementById("busca")?.value || "").toLowerCase().trim();

    let lista = _todos;
    if (_filtroAtivo === "ativos") lista = lista.filter((c) => c.ativo && (c.statusConta || "ATIVO") === "ATIVO");
    if (_filtroAtivo === "inativos") lista = lista.filter((c) => !c.ativo && (c.statusConta || "REMOVIDO") === "REMOVIDO");
    if (_filtroAtivo === "suspensos") lista = lista.filter((c) => (c.statusConta || "") === "SUSPENSO");
    if (busca) {
        lista = lista.filter((c) =>
            (c.nome || "").toLowerCase().includes(busca) ||
            (c.email || "").toLowerCase().includes(busca)
        );
    }

    // Filtro nível
    const nivel = document.getElementById("filtroNivel")?.value;
    if (nivel) lista = lista.filter(c => (c.nivel || "").toUpperCase() === nivel);

    // Filtro saldo
    const saldoMin = parseFloat(document.getElementById("filtroSaldoMin")?.value);
    const saldoMax = parseFloat(document.getElementById("filtroSaldoMax")?.value);
    if (!isNaN(saldoMin)) lista = lista.filter(c => (c.saldoTokens || 0) >= saldoMin);
    if (!isNaN(saldoMax)) lista = lista.filter(c => (c.saldoTokens || 0) <= saldoMax);

    // Filtro compras
    const comprasMin = parseInt(document.getElementById("filtroComprasMin")?.value);
    const comprasMax = parseInt(document.getElementById("filtroComprasMax")?.value);
    if (!isNaN(comprasMin)) lista = lista.filter(c => (c.totalCompras || 0) >= comprasMin);
    if (!isNaN(comprasMax)) lista = lista.filter(c => (c.totalCompras || 0) <= comprasMax);

    // Filtro cupons
    const cuponsMin = parseInt(document.getElementById("filtroCuponsMin")?.value);
    const cuponsMax = parseInt(document.getElementById("filtroCuponsMax")?.value);
    if (!isNaN(cuponsMin)) lista = lista.filter(c => (c.totalCupons || 0) >= cuponsMin);
    if (!isNaN(cuponsMax)) lista = lista.filter(c => (c.totalCupons || 0) <= cuponsMax);

    // Filtro cadastro
    const cadInicio = document.getElementById("filtroCadastroInicio")?.value;
    const cadFim = document.getElementById("filtroCadastroFim")?.value;
    if (cadInicio) list = lista.filter(c => c.dataCadastro && c.dataCadastro >= cadInicio);
    if (cadFim) lista = lista.filter(c => c.dataCadastro && c.dataCadastro <= cadFim + "T23:59:59");

    atualizarPills();
    renderTabela(lista);
}

function limparFiltrosAvancados() {
    ["filtroNivel","filtroSaldoMin","filtroSaldoMax",
     "filtroComprasMin","filtroComprasMax",
     "filtroCuponsMin","filtroCuponsMax",
     "filtroCadastroInicio","filtroCadastroFim"].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = "";
    });
    aplicarFiltros();
    atualizarPills();
}

/* ── RENDER ─────────────────────────────────────────────────── */
function badgeNivel(nivel) {
    const cls = {
        Bronze: "nivel-Bronze",
        Prata: "nivel-Prata",
        Ouro: "nivel-Ouro",
        Platina: "nivel-Platina",
    };
    return `<span class="badge-nivel ${cls[nivel] || "nivel-Bronze"}">${
        nivel || "Bronze"
    }</span>`;
}

function badgeStatus(ativo, statusConta) {
    const status = statusConta || (ativo ? "ATIVO" : "REMOVIDO");
    if (status === "SUSPENSO") {
        return `<span class="badge-status" style="background:#fef9c3;color:#854d0e;border:1px solid #fde68a"><i class="fa-solid fa-circle" style="font-size:7px"></i> Suspenso</span>`;
    }
    if (status === "ATIVO") {
        return `<span class="badge-status status-ativo"><i class="fa-solid fa-circle" style="font-size:7px"></i> Ativo</span>`;
    }
    return `<span class="badge-status status-inativo"><i class="fa-solid fa-circle" style="font-size:7px"></i> Inativo</span>`;
}

function renderTabela(lista) {
    const card = document.getElementById("tableCard");
    if (lista.length === 0) {
        card.innerHTML = `
            <div class="empty-state">
                <i class="fa-solid fa-users-slash"></i>
                <p>Nenhum cliente encontrado.</p>
            </div>`;
        return;
    }

    const linhas = lista.map((c) => `
        <tr>
            <td>
                <div class="cliente-info">
                    <div class="cliente-avatar">${inicial(c.nome)}</div>
                    <div>
                        <div class="cliente-nome">${esc(c.nome)}</div>
                        <div class="cliente-email">${esc(c.email)}</div>
                    </div>
                </div>
            </td>
            <td>${fmtData(c.dataCadastro)}</td>
            <td>${fmtTokens(c.saldoTokens)} tk</td>
            <td>${badgeNivel(c.nivel)}</td>
            <td>${c.totalCompras}</td>
            <td>${badgeStatus(c.ativo, c.statusConta)}</td>
            <td>
                <a href="/admin/clientes/${c.id}" class="btn-detalhes">
                    <i class="fa-solid fa-eye"></i> Ver Detalhes
                </a>
            </td>
        </tr>
    `).join("");

    card.innerHTML = `
        <table class="clientes-table">
            <thead>
                <tr>
                    <th>Cliente</th>
                    <th>Cadastro</th>
                    <th>Saldo</th>
                    <th>Nível</th>
                    <th>Compras</th>
                    <th>Status</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>${linhas}</tbody>
        </table>`;
}

function esc(str) {
    if (str == null) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

/* ── INIT ───────────────────────────────────────────────────── */
document.addEventListener("DOMContentLoaded", () => {
    const hoje = document.getElementById("dataHoje");
    if (hoje) {
        hoje.textContent = new Date().toLocaleDateString("pt-BR", {
            weekday: "long",
            day: "2-digit",
            month: "long",
            year: "numeric",
        });
    }
    carregarDados();
});