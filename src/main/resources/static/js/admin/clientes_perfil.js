/* ================================================================
   clientes_perfil.js — Admin · Bibliotroca
   ================================================================ */

let _perfil = null;
let _periodoDias = 30;

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
function fmtDataHora(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleDateString("pt-BR", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    }) +
        " " +
        d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}
function fmtTokens(v) {
    if (v == null) return "0";
    return Number(v).toLocaleString("pt-BR", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });
}
function inicial(nome) {
    if (!nome) return "?";
    return nome.trim().charAt(0).toUpperCase();
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
function mostrarToast(cls, msg) {
    const t = document.getElementById("toast");
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

/* ── FETCH ──────────────────────────────────────────────────── */
function carregarPerfil() {
    const id = window._CLIENTE_ID;
    if (!id) {
        document.getElementById("perfilContent").innerHTML =
            '<div class="loading-placeholder" style="margin-top:60px"><i class="fa-solid fa-triangle-exclamation"></i> ID do cliente não encontrado.</div>';
        return;
    }

    fetch(`/api/admin/clientes/${id}`, { credentials: "include" })
        .then((r) => {
            if (!r.ok) throw new Error("Não encontrado");
            return r.json();
        })
        .then((data) => {
            _perfil = data;
            renderPerfil(data);
        })
        .catch((err) => {
            document.getElementById("perfilContent").innerHTML =
                `<div class="loading-placeholder" style="margin-top:60px">
                    <i class="fa-solid fa-triangle-exclamation"></i>
                    Erro ao carregar perfil. <a href="/admin/clientes" style="color:#722F37">Voltar para a lista</a>
                 </div>`;
            console.error(err);
        });
}

/* ── RENDER COMPLETO ─────────────────────────────────────────── */
function renderPerfil(p) {
    // Atualiza breadcrumb no topbar
    const tt = document.getElementById("topbarTitle");
    if (tt) tt.textContent = esc(p.nome);

    const nivelCls = {
        Bronze: "nivel-Bronze",
        Prata: "nivel-Prata",
        Ouro: "nivel-Ouro",
        Platina: "nivel-Platina",
    };
    const nivel = p.nivel || "Bronze";
    const statusConta = p.statusConta || (p.ativo ? "ATIVO" : "REMOVIDO");
    const statusBadge = statusConta === "ATIVO"
        ? `<span class="badge-status status-ativo"><i class="fa-solid fa-circle" style="font-size:7px"></i> Ativo</span>`
        : statusConta === "SUSPENSO"
            ? `<span class="badge-status" style="background:#fef9c3;color:#854d0e;border:1px solid #fde68a"><i class="fa-solid fa-circle" style="font-size:7px"></i> Suspenso</span>`
            : `<span class="badge-status status-inativo"><i class="fa-solid fa-circle" style="font-size:7px"></i> Removido</span>`;

    // Seção Gestão de Conta
    const statusContaBadge = statusConta === "ATIVO"
        ? `<span style="padding:4px 12px;border-radius:20px;background:#dcfce7;color:#166534;font-size:13px;font-weight:600"><i class="fa-solid fa-circle" style="font-size:7px;margin-right:5px"></i>ATIVO</span>`
        : statusConta === "SUSPENSO"
            ? `<span style="padding:4px 12px;border-radius:20px;background:#fef9c3;color:#854d0e;font-size:13px;font-weight:600"><i class="fa-solid fa-circle" style="font-size:7px;margin-right:5px"></i>SUSPENSO</span>`
            : `<span style="padding:4px 12px;border-radius:20px;background:#fee2e2;color:#991b1b;font-size:13px;font-weight:600"><i class="fa-solid fa-circle" style="font-size:7px;margin-right:5px"></i>REMOVIDO</span>`;
    // Card de detalhes da ação (suspensão ou remoção)
    let acaoDetalhe = "";
    if (statusConta === "SUSPENSO" || statusConta === "REMOVIDO") {
        const corBorda = statusConta === "SUSPENSO" ? "#fde68a" : "#fca5a5";
        const corFundo = statusConta === "SUSPENSO" ? "#fffbeb" : "#fff5f5";

        // Contagem regressiva para suspensão temporária
        let contagemRegressiva = "";
        if (statusConta === "SUSPENSO" && p.suspensaoAte) {
            const ate = new Date(p.suspensaoAte);
            const agora = new Date();
            const diffMs = ate - agora;
            if (diffMs > 0) {
                const dias = Math.floor(diffMs / (1000 * 60 * 60 * 24));
                const horas = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
                contagemRegressiva = `<span style="display:inline-block;margin-top:4px;padding:2px 10px;border-radius:12px;background:#fef9c3;color:#854d0e;font-size:11px;font-weight:600">
                    <i class="fa-solid fa-hourglass-half" style="margin-right:4px"></i>Expira em ${dias}d ${horas}h
                </span>`;
            } else {
                contagemRegressiva = `<span style="display:inline-block;margin-top:4px;padding:2px 10px;border-radius:12px;background:#fee2e2;color:#991b1b;font-size:11px;font-weight:600">Prazo expirado</span>`;
            }
        }

        const linhaValidade = statusConta === "SUSPENSO"
            ? `<div class="acao-detalhe-row">
                <span class="acao-detalhe-label">Válida até</span>
                <span class="acao-detalhe-value">${p.suspensaoAte ? fmtDataHora(p.suspensaoAte) : "Indefinida"} ${contagemRegressiva}</span>
               </div>`
            : "";

        const linhaEmail = p.emailNotificadoEm
            ? `<div class="acao-detalhe-row">
                <span class="acao-detalhe-label"><i class="fa-solid fa-envelope" style="margin-right:4px"></i>E-mail enviado</span>
                <span class="acao-detalhe-value" style="color:#166534">${fmtDataHora(p.emailNotificadoEm)}</span>
               </div>`
            : `<div class="acao-detalhe-row">
                <span class="acao-detalhe-label"><i class="fa-solid fa-envelope-slash" style="margin-right:4px"></i>E-mail</span>
                <span class="acao-detalhe-value" style="color:#7a6e65">Não notificado</span>
               </div>`;

        acaoDetalhe = `
            <div style="margin-top:14px;border:1.5px solid ${corBorda};border-radius:10px;background:${corFundo};padding:14px 18px">
                <div style="font-size:12px;font-weight:700;color:#5a4f47;margin-bottom:10px;text-transform:uppercase;letter-spacing:0.04em">
                    <i class="fa-solid fa-clock-rotate-left" style="margin-right:6px"></i>Detalhes da Ação
                </div>
                <div style="display:flex;flex-direction:column;gap:6px">
                    <div class="acao-detalhe-row">
                        <span class="acao-detalhe-label">Data da ação</span>
                        <span class="acao-detalhe-value">${p.dataAcao ? fmtDataHora(p.dataAcao) : "—"}</span>
                    </div>
                    <div class="acao-detalhe-row">
                        <span class="acao-detalhe-label">Responsável</span>
                        <span class="acao-detalhe-value">${p.adminAcao ? esc(p.adminAcao) : "—"}</span>
                    </div>
                    <div class="acao-detalhe-row">
                        <span class="acao-detalhe-label">Motivo</span>
                        <span class="acao-detalhe-value"><em>${p.motivoSuspensao ? esc(p.motivoSuspensao) : "—"}</em></span>
                    </div>
                    ${linhaValidade}
                    ${linhaEmail}
                </div>
            </div>`;
    }
    const suspensaoExtra = acaoDetalhe;
    const btnSuspender = statusConta === "ATIVO"
        ? `<button onclick="abrirModalSuspender()" style="padding:0.45rem 1.1rem;border-radius:8px;font-size:0.82rem;font-weight:600;cursor:pointer;background:#fff;color:#b45309;border:1.5px solid #b45309;transition:all 0.2s"><i class="fa-solid fa-ban"></i> Suspender Conta</button>`
        : "";
    const btnRemover = statusConta !== "REMOVIDO"
        ? `<button onclick="abrirModalRemover()" style="padding:0.45rem 1.1rem;border-radius:8px;font-size:0.82rem;font-weight:600;cursor:pointer;background:#fff;color:#722f37;border:1.5px solid #722f37;transition:all 0.2s"><i class="fa-solid fa-trash"></i> Remover Conta</button>`
        : "";
    const btnReativar = statusConta !== "ATIVO"
        ? `<button onclick="abrirModalReativar()" style="padding:0.45rem 1.1rem;border-radius:8px;font-size:0.82rem;font-weight:600;cursor:pointer;background:#2e7d32;color:#fff;border:none;transition:all 0.2s"><i class="fa-solid fa-rotate-left"></i> Reativar Conta</button>`
        : "";

    // Atualiza breadcrumb padronizado com o nome do cliente
    const bcrNome = document.getElementById("bcrNomeCliente");
    if (bcrNome) bcrNome.textContent = p.nome;

    const html = `
        <!-- HEADER DO PERFIL -->
        <div class="perfil-header">
            <div class="perfil-avatar">${inicial(p.nome)}</div>
            <div class="perfil-info">
                <div class="perfil-nome">${esc(p.nome)}</div>
                <div class="perfil-email">${esc(p.email)}</div>
                <div class="perfil-meta">
                    <span class="badge-nivel ${
        nivelCls[nivel] || "nivel-Bronze"
    }">${nivel}</span>
                    ${statusBadge}
                    <span class="perfil-meta-item"><i class="fa-solid fa-calendar"></i> Cadastro em ${
        fmtData(p.dataCadastro)
    }</span>
                    ${
        p.dataNascimento
            ? `<span class="perfil-meta-item"><i class="fa-solid fa-cake-candles"></i> ${
                esc(p.dataNascimento)
            }</span>`
            : ""
    }
                    ${
        p.cpfMascarado
            ? `<span class="perfil-meta-item"><i class="fa-solid fa-id-card"></i> CPF: ${
                esc(p.cpfMascarado)
            }</span>`
            : ""
    }
                </div>
            </div>
        </div>

        <!-- SUMMARY CARDS -->
        <div class="summary-grid">
            <div class="summary-card">
                <div class="summary-icon" style="background:#722F37"><i class="fa-solid fa-coins"></i></div>
                <div class="summary-value">${fmtTokens(p.saldoTokens)}</div>
                <div class="summary-label">Saldo Tokens</div>
            </div>
            <div class="summary-card">
                <div class="summary-icon" style="background:#b45309"><i class="fa-solid fa-money-bill-wave"></i></div>
                <div class="summary-value">${fmtTokens(p.totalGasto)}</div>
                <div class="summary-label">Total Gasto (tk)</div>
            </div>
            <div class="summary-card">
                <div class="summary-icon" style="background:#4a5d23"><i class="fa-solid fa-arrow-trend-up"></i></div>
                <div class="summary-value">${
        fmtTokens(p.totalRecarregado)
    }</div>
                <div class="summary-label">Total Recarregado</div>
            </div>
            <div class="summary-card">
                <div class="summary-icon" style="background:#1565c0"><i class="fa-solid fa-bag-shopping"></i></div>
                <div class="summary-value">${p.totalPedidos}</div>
                <div class="summary-label">Total Pedidos</div>
            </div>
            <div class="summary-card">
                <div class="summary-icon" style="background:#2e7d32"><i class="fa-solid fa-book"></i></div>
                <div class="summary-value">${p.totalLivrosVendidos}</div>
                <div class="summary-label">Livros Vendidos</div>
            </div>
            <div class="summary-card">
                <div class="summary-icon" style="background:#6a1b9a"><i class="fa-solid fa-ticket"></i></div>
                <div class="summary-value">${p.quantidadeCuponsUsados}</div>
                <div class="summary-label">Cupons Usados</div>
            </div>
        </div>

        <!-- ABAS -->
        <div class="tabs-area">
            <div class="tabs-header">
                <button class="tab-btn ativo" onclick="ativarAba('compras')" id="tab-compras">
                    <i class="fa-solid fa-bag-shopping"></i> Compras
                </button>
                <button class="tab-btn" onclick="ativarAba('vendas')" id="tab-vendas">
                    <i class="fa-solid fa-store"></i> Vendas
                </button>
                <button class="tab-btn" onclick="ativarAba('financeiro')" id="tab-financeiro">
                    <i class="fa-solid fa-money-bill-wave"></i> Financeiro
                </button>
                <button class="tab-btn" onclick="ativarAba('engajamento')" id="tab-engajamento">
                    <i class="fa-solid fa-heart"></i> Engajamento
                </button>
            </div>

            <!-- ABA COMPRAS -->
            <div class="tab-panel ativo" id="panel-compras">
                <div class="panel-card">
                    <div class="panel-header">
                        <div class="panel-title"><i class="fa-solid fa-bag-shopping"></i> Histórico de Compras</div>
                        <div class="period-filter" id="periodFilter">
                            <button class="period-btn" onclick="filtrarPeriodo(7)">7 dias</button>
                            <button class="period-btn ativo" onclick="filtrarPeriodo(30)">30 dias</button>
                            <button class="period-btn" onclick="filtrarPeriodo(90)">3 meses</button>
                            <button class="period-btn" onclick="filtrarPeriodo(365)">1 ano</button>
                            <button class="period-btn" onclick="filtrarPeriodo(0)">Todos</button>
                        </div>
                    </div>
                    <div id="tabelaCompras"></div>
                </div>
                <div class="panel-card">
                    <div class="panel-header">
                        <div class="panel-title"><i class="fa-solid fa-ban"></i> Cancelamentos</div>
                        <span style="font-size:13px;color:#7a6e65">${p.totalCancelamentos} solicitação(ões)</span>
                    </div>
                    <div class="info-grid" style="padding-top:16px;padding-bottom:16px">
                        <div class="info-item">
                            <div class="info-label">Total de Cancelamentos</div>
                            <div class="info-value">${p.totalCancelamentos}</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ABA VENDAS -->
            <div class="tab-panel" id="panel-vendas">
                <div class="panel-card">
                    <div class="panel-header">
                        <div class="panel-title"><i class="fa-solid fa-store"></i> Atividade de Vendedor</div>
                    </div>
                    <div class="info-grid">
                        <div class="info-item">
                            <div class="info-label">Livros Vendidos</div>
                            <div class="info-value">${p.totalLivrosVendidos}</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">Lotes Enviados</div>
                            <div class="info-value">${p.totalLotesEnviados}</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">Livros Rejeitados</div>
                            <div class="info-value">${p.totalLivrosRejeitados}</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ABA FINANCEIRO -->
            <div class="tab-panel" id="panel-financeiro">
                <div class="panel-card">
                    <div class="panel-header">
                        <div class="panel-title"><i class="fa-solid fa-money-bill-wave"></i> Resumo Financeiro</div>
                    </div>
                    <div class="info-grid">
                        <div class="info-item">
                            <div class="info-label">Saldo Atual (tokens)</div>
                            <div class="info-value">${
        fmtTokens(p.saldoTokens)
    }</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">Total Gasto (tokens)</div>
                            <div class="info-value">${
        fmtTokens(p.totalGasto)
    }</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">Total Recarregado</div>
                            <div class="info-value">${
        fmtTokens(p.totalRecarregado)
    }</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">Cupons Utilizados</div>
                            <div class="info-value">${p.quantidadeCuponsUsados}</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ABA ENGAJAMENTO -->
            <div class="tab-panel" id="panel-engajamento">
                <div class="panel-card">
                    <div class="panel-header">
                        <div class="panel-title"><i class="fa-solid fa-heart"></i> Engajamento na Plataforma</div>
                    </div>
                    <div class="info-grid">
                        <div class="info-item">
                            <div class="info-label">Tópicos no Fórum</div>
                            <div class="info-value">${p.totalTopicosForum}</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">Lista de Desejos</div>
                            <div class="info-value">${p.totalListaDesejos}</div>
                        </div>
                    </div>
                </div>
            </div>

        </div><!-- /tabs-area -->

        <!-- GESTÃO DE CONTA -->
        <div class="panel-card" style="margin-top:24px">
            <div class="panel-header">
                <div class="panel-title"><i class="fa-solid fa-shield-halved"></i> Gestão de Conta</div>
                ${statusContaBadge}
            </div>
            ${suspensaoExtra}
            <div style="display:flex;gap:0.75rem;flex-wrap:wrap;margin-top:16px">
                ${btnSuspender}
                ${btnRemover}
                ${btnReativar}
            </div>
        </div>

        <!-- MODAL SUSPENDER -->
        <div id="modalSuspender" style="display:none;position:fixed;inset:0;background:rgba(0,0,0,0.5);z-index:9999;align-items:center;justify-content:center;">
            <div style="background:#fff;border-radius:12px;padding:2rem;max-width:480px;width:90%;box-shadow:0 8px 32px rgba(0,0,0,0.2);">
                <h3 style="margin:0 0 1rem;font-family:'Playfair Display',serif;font-size:1.2rem;color:#2c241b">Suspender Conta</h3>
                <div style="margin-bottom:1rem">
                    <label style="display:block;font-size:0.85rem;font-weight:600;color:#2c241b;margin-bottom:0.4rem">Motivo *</label>
                    <textarea id="motivoSuspensao" rows="3" placeholder="Descreva o motivo da suspensão..." style="width:100%;padding:0.6rem;border:1.5px solid #e0d9d0;border-radius:8px;font-size:0.9rem;resize:vertical;box-sizing:border-box"></textarea>
                </div>
                <div style="margin-bottom:1rem">
                    <label style="display:block;font-size:0.85rem;font-weight:600;color:#2c241b;margin-bottom:0.6rem">Duração</label>
                    <div style="display:flex;flex-wrap:wrap;gap:0.5rem">
                        <label style="display:flex;align-items:center;gap:0.4rem;font-size:0.85rem;cursor:pointer"><input type="radio" name="diasSuspensao" value="7"> 7 dias</label>
                        <label style="display:flex;align-items:center;gap:0.4rem;font-size:0.85rem;cursor:pointer"><input type="radio" name="diasSuspensao" value="15"> 15 dias</label>
                        <label style="display:flex;align-items:center;gap:0.4rem;font-size:0.85rem;cursor:pointer"><input type="radio" name="diasSuspensao" value="30"> 30 dias</label>
                        <label style="display:flex;align-items:center;gap:0.4rem;font-size:0.85rem;cursor:pointer"><input type="radio" name="diasSuspensao" value="0" checked> Indefinido</label>
                    </div>
                </div>
                <div style="margin-bottom:1.5rem">
                    <label style="display:flex;align-items:center;gap:0.5rem;font-size:0.85rem;cursor:pointer">
                        <input type="checkbox" id="notificarSuspensao" checked> Notificar cliente por e-mail
                    </label>
                </div>
                <div style="display:flex;gap:0.75rem;justify-content:flex-end">
                    <button onclick="fecharModalSuspender()" style="padding:0.5rem 1.25rem;border:1.5px solid #e0d9d0;border-radius:8px;background:#fff;color:#7a6e65;font-size:0.9rem;cursor:pointer;font-weight:500">Cancelar</button>
                    <button onclick="confirmarSuspensao()" style="padding:0.5rem 1.25rem;border:none;border-radius:8px;background:#b45309;color:#fff;font-size:0.9rem;cursor:pointer;font-weight:600">Confirmar Suspensão</button>
                </div>
            </div>
        </div>

        <!-- MODAL REMOVER -->
        <div id="modalRemover" style="display:none;position:fixed;inset:0;background:rgba(0,0,0,0.5);z-index:9999;align-items:center;justify-content:center;">
            <div style="background:#fff;border-radius:12px;padding:2rem;max-width:480px;width:90%;box-shadow:0 8px 32px rgba(0,0,0,0.2);">
                <h3 style="margin:0 0 1rem;font-family:'Playfair Display',serif;font-size:1.2rem;color:#2c241b">Remover Conta</h3>
                <p style="margin:0 0 1rem;font-size:0.9rem;color:#5a4f47;line-height:1.6">Esta ação marcará a conta como <strong>removida</strong>. O cliente não poderá mais fazer login.</p>
                <div style="margin-bottom:1rem">
                    <label style="display:block;font-size:0.85rem;font-weight:600;color:#2c241b;margin-bottom:0.4rem">Motivo *</label>
                    <textarea id="motivoRemocao" rows="3" placeholder="Descreva o motivo da remoção..." style="width:100%;padding:0.6rem;border:1.5px solid #e0d9d0;border-radius:8px;font-size:0.9rem;resize:vertical;box-sizing:border-box"></textarea>
                </div>
                <div style="margin-bottom:1.5rem">
                    <label style="display:flex;align-items:center;gap:0.5rem;font-size:0.85rem;cursor:pointer">
                        <input type="checkbox" id="notificarRemocao" checked> Notificar cliente por e-mail
                    </label>
                </div>
                <div style="display:flex;gap:0.75rem;justify-content:flex-end">
                    <button onclick="fecharModalRemover()" style="padding:0.5rem 1.25rem;border:1.5px solid #e0d9d0;border-radius:8px;background:#fff;color:#7a6e65;font-size:0.9rem;cursor:pointer;font-weight:500">Cancelar</button>
                    <button onclick="confirmarRemocao()" style="padding:0.5rem 1.25rem;border:none;border-radius:8px;background:#722f37;color:#fff;font-size:0.9rem;cursor:pointer;font-weight:600">Confirmar Remoção</button>
                </div>
            </div>
        </div>

        <!-- MODAL REATIVAR -->
        <div id="modalReativar" style="display:none;position:fixed;inset:0;background:rgba(0,0,0,0.5);z-index:9999;align-items:center;justify-content:center;">
            <div style="background:#fff;border-radius:12px;padding:2rem;max-width:480px;width:90%;box-shadow:0 8px 32px rgba(0,0,0,0.2);">
                <h3 style="margin:0 0 1rem;font-family:'Playfair Display',serif;font-size:1.2rem;color:#2c241b">Reativar Conta</h3>
                <p style="margin:0 0 1rem;font-size:0.9rem;color:#5a4f47;line-height:1.6">A conta será reativada e o cliente poderá fazer login novamente.</p>
                <div style="margin-bottom:1rem">
                    <label style="display:block;font-size:0.85rem;font-weight:600;color:#2c241b;margin-bottom:0.4rem">Mensagem para o cliente (opcional)</label>
                    <textarea id="mensagemReativacao" rows="3" placeholder="Sua conta na Bibliotroca foi reativada. Você já pode fazer login normalmente. Agradecemos sua compreensão e esperamos que tenha uma boa experiência em nossa plataforma." style="width:100%;padding:0.6rem;border:1.5px solid #e0d9d0;border-radius:8px;font-size:0.9rem;resize:vertical;box-sizing:border-box"></textarea>
                </div>
                <div style="margin-bottom:1.5rem">
                    <label style="display:flex;align-items:center;gap:0.5rem;font-size:0.85rem;cursor:pointer">
                        <input type="checkbox" id="notificarReativacao" checked> Notificar cliente por e-mail
                    </label>
                </div>
                <div style="display:flex;gap:0.75rem;justify-content:flex-end">
                    <button onclick="fecharModalReativar()" style="padding:0.5rem 1.25rem;border:1.5px solid #e0d9d0;border-radius:8px;background:#fff;color:#7a6e65;font-size:0.9rem;cursor:pointer;font-weight:500">Cancelar</button>
                    <button onclick="confirmarReativacao()" style="padding:0.5rem 1.25rem;border:none;border-radius:8px;background:#2e7d32;color:#fff;font-size:0.9rem;cursor:pointer;font-weight:600"><i class="fa-solid fa-rotate-left"></i> Reativar Conta</button>
                </div>
            </div>
        </div>
    `;

    document.getElementById("perfilContent").innerHTML = html;

    // Renderiza tabela de compras inicial
    renderTabelaCompras(_perfil.pedidos || [], _periodoDias);
}

/* ── TABELA DE COMPRAS ──────────────────────────────────────── */
const STATUS_ENVIO_LABEL = {
    AGUARDANDO_ENVIO: "Aguardando Envio",
    EM_TRANSITO: "Em Trânsito",
    ENTREGUE: "Entregue",
    CANCELADO: "Cancelado",
};

function filtrarPeriodo(dias) {
    _periodoDias = dias;
    // Atualiza botões
    document.querySelectorAll(".period-btn").forEach((btn) => {
        const btnDias = parseInt(
            btn.getAttribute("onclick").match(/\d+/) || [0],
        );
        btn.classList.toggle("ativo", btnDias === dias);
    });
    if (_perfil) renderTabelaCompras(_perfil.pedidos || [], dias);
}

function renderTabelaCompras(pedidos, dias) {
    const container = document.getElementById("tabelaCompras");
    if (!container) return;

    let lista = pedidos;
    if (dias > 0) {
        const corte = new Date();
        corte.setDate(corte.getDate() - dias);
        lista = pedidos.filter((p) => new Date(p.dataCompra) >= corte);
    }

    if (lista.length === 0) {
        container.innerHTML = `
            <div class="empty-state-sm">
                <i class="fa-solid fa-bag-shopping"></i>
                <p>Nenhuma compra encontrada neste período.</p>
            </div>`;
        return;
    }

    const linhas = lista.map((p) => {
        const statusLabel = STATUS_ENVIO_LABEL[p.status] || p.status;
        const statusCls = "envio-" + (p.status || "AGUARDANDO_ENVIO");
        return `
            <tr>
                <td>#${p.id}</td>
                <td>
                    <div style="font-weight:600">${esc(p.titulo)}</div>
                    <div style="font-size:11px;color:#7a6e65">${
            esc(p.autor)
        }</div>
                </td>
                <td>${fmtTokens(p.preco)} tk</td>
                <td><span class="badge-envio ${statusCls}">${
            esc(statusLabel)
        }</span></td>
                <td>${
            p.codigoRastreio
                ? `<span style="font-family:monospace;font-size:12px">${
                    esc(p.codigoRastreio)
                }</span>`
                : "—"
        }</td>
                <td>${fmtDataHora(p.dataCompra)}</td>
            </tr>`;
    }).join("");

    container.innerHTML = `
        <table class="inner-table">
            <thead>
                <tr>
                    <th>#</th>
                    <th>Livro</th>
                    <th>Preço</th>
                    <th>Status</th>
                    <th>Rastreio</th>
                    <th>Data</th>
                </tr>
            </thead>
            <tbody>${linhas}</tbody>
        </table>`;
}

/* ── TABS ───────────────────────────────────────────────────── */
function ativarAba(nome) {
    document.querySelectorAll(".tab-btn").forEach((b) =>
        b.classList.remove("ativo")
    );
    document.querySelectorAll(".tab-panel").forEach((p) =>
        p.classList.remove("ativo")
    );

    const btn = document.getElementById("tab-" + nome);
    const panel = document.getElementById("panel-" + nome);
    if (btn) btn.classList.add("ativo");
    if (panel) panel.classList.add("ativo");
}

/* ── GESTÃO DE CONTA ─────────────────────────────────────────── */
function abrirModalSuspender() {
    const m = document.getElementById("modalSuspender");
    if (!m) return;
    m.style.display = "flex";
    m.onclick = (e) => { if (e.target === m) fecharModalSuspender(); };
}
function fecharModalSuspender() {
    const m = document.getElementById("modalSuspender");
    if (m) m.style.display = "none";
}

function abrirModalRemover() {
    const m = document.getElementById("modalRemover");
    if (!m) return;
    m.style.display = "flex";
    m.onclick = (e) => { if (e.target === m) fecharModalRemover(); };
}
function fecharModalRemover() {
    const m = document.getElementById("modalRemover");
    if (m) m.style.display = "none";
}

async function confirmarSuspensao() {
    const motivo = document.getElementById("motivoSuspensao")?.value?.trim();
    if (!motivo) { alert("Informe o motivo da suspensão."); return; }
    const dias = parseInt(document.querySelector('input[name="diasSuspensao"]:checked')?.value || "0");
    const notificar = document.getElementById("notificarSuspensao")?.checked ?? true;
    const id = window._CLIENTE_ID;
    try {
        const res = await fetch(`/api/admin/clientes/${id}/suspender`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ motivo, diasSuspensao: dias, notificarEmail: notificar })
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok) { mostrarToast("toast-err", data.erro || "Erro ao suspender conta."); return; }
        fecharModalSuspender();
        mostrarToast("toast-ok", "Conta suspensa com sucesso.");
        await carregarPerfil();
    } catch (_) {
        mostrarToast("toast-err", "Erro de conexão.");
    }
}

async function confirmarRemocao() {
    const motivo = document.getElementById("motivoRemocao")?.value?.trim();
    if (!motivo) { alert("Informe o motivo da remoção."); return; }
    const notificar = document.getElementById("notificarRemocao")?.checked ?? true;
    const id = window._CLIENTE_ID;
    try {
        const res = await fetch(`/api/admin/clientes/${id}/remover`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ motivo, notificarEmail: notificar })
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok) { mostrarToast("toast-err", data.erro || "Erro ao remover conta."); return; }
        fecharModalRemover();
        mostrarToast("toast-ok", "Conta removida com sucesso.");
        await carregarPerfil();
    } catch (_) {
        mostrarToast("toast-err", "Erro de conexão.");
    }
}

function abrirModalReativar() {
    const m = document.getElementById("modalReativar");
    if (!m) return;
    m.style.display = "flex";
    m.onclick = (e) => { if (e.target === m) fecharModalReativar(); };
}
function fecharModalReativar() {
    const m = document.getElementById("modalReativar");
    if (m) m.style.display = "none";
}

async function confirmarReativacao() {
    const mensagem = document.getElementById("mensagemReativacao")?.value?.trim() || null;
    const notificar = document.getElementById("notificarReativacao")?.checked ?? true;
    const id = window._CLIENTE_ID;
    try {
        const res = await fetch(`/api/admin/clientes/${id}/reativar`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ mensagem, notificarEmail: notificar })
        });
        const data = await res.json().catch(() => ({}));
        if (!res.ok) { mostrarToast("toast-err", data.erro || "Erro ao reativar conta."); return; }
        fecharModalReativar();
        mostrarToast("toast-ok", "Conta reativada com sucesso.");
        await carregarPerfil();
    } catch (_) {
        mostrarToast("toast-err", "Erro de conexão.");
    }
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
    carregarPerfil();
});
