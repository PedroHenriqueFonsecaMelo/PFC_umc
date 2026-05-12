/* ================================================================
   clientes_perfil.js — Admin · Bibliotroca
   ================================================================ */

let _perfil = null;
let _periodoDias = 30;

/* ── UTILS ──────────────────────────────────────────────────── */
function fmtData(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' });
}
function fmtDataHora(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' }) +
           ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}
function fmtTokens(v) {
    if (v == null) return '0';
    return Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
function inicial(nome) {
    if (!nome) return '?';
    return nome.trim().charAt(0).toUpperCase();
}
function esc(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
function mostrarToast(cls, msg) {
    const t = document.getElementById('toast');
    t.className = 'toast ' + cls;
    t.textContent = msg;
    t.style.display = 'block';
    setTimeout(() => { t.style.display = 'none'; }, 4000);
}
function abrirMenu() {
    document.getElementById('sidebar')?.classList.add('open');
    document.getElementById('overlay')?.classList.add('show');
}
function fecharMenu() {
    document.getElementById('sidebar')?.classList.remove('open');
    document.getElementById('overlay')?.classList.remove('show');
}

/* ── FETCH ──────────────────────────────────────────────────── */
function carregarPerfil() {
    const id = window._CLIENTE_ID;
    if (!id) {
        document.getElementById('perfilContent').innerHTML =
            '<div class="loading-placeholder" style="margin-top:60px"><i class="fa-solid fa-triangle-exclamation"></i> ID do cliente não encontrado.</div>';
        return;
    }

    fetch(`/api/admin/clientes/${id}`, { credentials: 'include' })
        .then(r => {
            if (!r.ok) throw new Error('Não encontrado');
            return r.json();
        })
        .then(data => {
            _perfil = data;
            renderPerfil(data);
        })
        .catch(err => {
            document.getElementById('perfilContent').innerHTML =
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
    const tt = document.getElementById('topbarTitle');
    if (tt) tt.textContent = esc(p.nome);

    const nivelCls = { Bronze: 'nivel-Bronze', Prata: 'nivel-Prata', Ouro: 'nivel-Ouro', Platina: 'nivel-Platina' };
    const nivel = p.nivel || 'Bronze';
    const statusBadge = p.ativo
        ? `<span class="badge-status status-ativo"><i class="fa-solid fa-circle" style="font-size:7px"></i> Ativo</span>`
        : `<span class="badge-status status-inativo"><i class="fa-solid fa-circle" style="font-size:7px"></i> Inativo</span>`;

    const html = `
        <!-- BREADCRUMB -->
        <div class="breadcrumb">
            <a href="/admin/dashboard"><i class="fa-solid fa-gauge"></i> Painel Admin</a>
            <i class="fa-solid fa-chevron-right"></i>
            <a href="/admin/clientes">Clientes</a>
            <i class="fa-solid fa-chevron-right"></i>
            <span>${esc(p.nome)}</span>
        </div>

        <!-- HEADER DO PERFIL -->
        <div class="perfil-header">
            <div class="perfil-avatar">${inicial(p.nome)}</div>
            <div class="perfil-info">
                <div class="perfil-nome">${esc(p.nome)}</div>
                <div class="perfil-email">${esc(p.email)}</div>
                <div class="perfil-meta">
                    <span class="badge-nivel ${nivelCls[nivel] || 'nivel-Bronze'}">${nivel}</span>
                    ${statusBadge}
                    <span class="perfil-meta-item"><i class="fa-solid fa-calendar"></i> Cadastro em ${fmtData(p.dataCadastro)}</span>
                    ${p.dataNascimento ? `<span class="perfil-meta-item"><i class="fa-solid fa-cake-candles"></i> ${esc(p.dataNascimento)}</span>` : ''}
                    ${p.cpfMascarado ? `<span class="perfil-meta-item"><i class="fa-solid fa-id-card"></i> CPF: ${esc(p.cpfMascarado)}</span>` : ''}
                </div>
            </div>
            <a href="/admin/clientes" class="btn-voltar">
                <i class="fa-solid fa-arrow-left"></i> Voltar para Clientes
            </a>
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
                <div class="summary-value">${fmtTokens(p.totalRecarregado)}</div>
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
                            <div class="info-value">${fmtTokens(p.saldoTokens)}</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">Total Gasto (tokens)</div>
                            <div class="info-value">${fmtTokens(p.totalGasto)}</div>
                        </div>
                        <div class="info-item">
                            <div class="info-label">Total Recarregado</div>
                            <div class="info-value">${fmtTokens(p.totalRecarregado)}</div>
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
    `;

    document.getElementById('perfilContent').innerHTML = html;

    // Renderiza tabela de compras inicial
    renderTabelaCompras(_perfil.pedidos || [], _periodoDias);
}

/* ── TABELA DE COMPRAS ──────────────────────────────────────── */
const STATUS_ENVIO_LABEL = {
    AGUARDANDO_ENVIO: 'Aguardando Envio',
    EM_TRANSITO: 'Em Trânsito',
    ENTREGUE: 'Entregue',
    CANCELADO: 'Cancelado',
};

function filtrarPeriodo(dias) {
    _periodoDias = dias;
    // Atualiza botões
    document.querySelectorAll('.period-btn').forEach(btn => {
        const btnDias = parseInt(btn.getAttribute('onclick').match(/\d+/) || [0]);
        btn.classList.toggle('ativo', btnDias === dias);
    });
    if (_perfil) renderTabelaCompras(_perfil.pedidos || [], dias);
}

function renderTabelaCompras(pedidos, dias) {
    const container = document.getElementById('tabelaCompras');
    if (!container) return;

    let lista = pedidos;
    if (dias > 0) {
        const corte = new Date();
        corte.setDate(corte.getDate() - dias);
        lista = pedidos.filter(p => new Date(p.dataCompra) >= corte);
    }

    if (lista.length === 0) {
        container.innerHTML = `
            <div class="empty-state-sm">
                <i class="fa-solid fa-bag-shopping"></i>
                <p>Nenhuma compra encontrada neste período.</p>
            </div>`;
        return;
    }

    const linhas = lista.map(p => {
        const statusLabel = STATUS_ENVIO_LABEL[p.status] || p.status;
        const statusCls = 'envio-' + (p.status || 'AGUARDANDO_ENVIO');
        return `
            <tr>
                <td>#${p.id}</td>
                <td>
                    <div style="font-weight:600">${esc(p.titulo)}</div>
                    <div style="font-size:11px;color:#7a6e65">${esc(p.autor)}</div>
                </td>
                <td>${fmtTokens(p.preco)} tk</td>
                <td><span class="badge-envio ${statusCls}">${esc(statusLabel)}</span></td>
                <td>${p.codigoRastreio ? `<span style="font-family:monospace;font-size:12px">${esc(p.codigoRastreio)}</span>` : '—'}</td>
                <td>${fmtDataHora(p.dataCompra)}</td>
            </tr>`;
    }).join('');

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
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('ativo'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('ativo'));

    const btn = document.getElementById('tab-' + nome);
    const panel = document.getElementById('panel-' + nome);
    if (btn) btn.classList.add('ativo');
    if (panel) panel.classList.add('ativo');
}

/* ── INIT ───────────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
    const hoje = document.getElementById('dataHoje');
    if (hoje) {
        hoje.textContent = new Date().toLocaleDateString('pt-BR', {
            weekday: 'long', day: '2-digit', month: 'long', year: 'numeric'
        });
    }
    carregarPerfil();
});
