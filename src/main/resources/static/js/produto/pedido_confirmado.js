/* ================================================================
   pedido_confirmado.js — Confirmação de compra · Bibliotroca
   Lê os dados do pedido gravados no sessionStorage pelo checkout.js
   e renderiza o resumo completo da compra.
   ================================================================ */

const CONF_KEY = 'bibliotroca_confirmacao';

function escHtml(str) {
    return String(str || '')
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function carregarSaldo() {
    fetch('/clientes/meu-perfil-json', { credentials: 'include' })
        .then(r => r.ok ? r.json() : null)
        .then(c => {
            if (!c) return;
            const el = document.getElementById('navSaldo');
            if (el) el.textContent = `T$ ${(c.saldoTokens || 0).toFixed(2)}`;
        }).catch(() => {});
}

document.addEventListener('DOMContentLoaded', () => {
    carregarSaldo();

    let dados = null;
    try { dados = JSON.parse(sessionStorage.getItem(CONF_KEY)); } catch (_) {}

    const estadoVazio = document.getElementById('estadoVazio');
    const conteudo    = document.getElementById('conteudo');

    if (!dados || !Array.isArray(dados.comprados) || dados.comprados.length === 0) {
        estadoVazio.style.display = 'block';
        return;
    }

    // Dados lidos — limpa a sessão
    sessionStorage.removeItem(CONF_KEY);

    conteudo.style.display = 'block';

    // Data da compra
    document.getElementById('heroData').textContent = dados.dataCompra || '';

    // Cards dos pedidos
    document.getElementById('listaPedidos').innerHTML = dados.comprados.map((item, idx) => {
        const delay = idx * 80;

        const imgHtml = item.foto
            ? `<img class="pedido-foto" src="${escHtml(item.foto)}"
                    alt="${escHtml(item.titulo)}"
                    onerror="this.parentElement.innerHTML='<div class=\\'pedido-foto pedido-foto--vazio\\'>📚</div>'">`
            : `<div class="pedido-foto pedido-foto--vazio">📚</div>`;

        const autorHtml = item.autor
            ? `<div class="pedido-autor">por ${escHtml(item.autor)}</div>` : '';

        return `
        <div class="pedido-card" style="animation-delay:${delay}ms">
          <div class="pedido-card-topo">
            <div class="pedido-id-wrap">
              <span class="pedido-id-label">Pedido</span>
              <strong class="pedido-id">#${item.pedidoId || '—'}</strong>
            </div>
            <span class="status-badge">Aguardando envio</span>
          </div>
          <div class="pedido-card-corpo">
            ${imgHtml}
            <div class="pedido-info">
              <div class="pedido-titulo">${escHtml(item.titulo || '—')}</div>
              ${autorHtml}
              <div class="pedido-preco">T$ ${(item.preco || 0).toFixed(2)}</div>
            </div>
          </div>
        </div>`;
    }).join('');

    // Resumo financeiro
    document.getElementById('resumoTotal').textContent =
        `T$ ${(dados.totalGasto || 0).toFixed(2)}`;
    document.getElementById('resumoSaldo').textContent =
        `T$ ${(dados.saldoRestante || 0).toFixed(2)}`;
});
