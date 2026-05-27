/* ================================================================
   checkout.js — Página de Checkout (/livros/checkout)
   Lê IDs selecionados do sessionStorage, busca dados no localStorage,
   exibe resumo, valida saldo via backend e confirma a compra via
   POST /api/livros/carrinho/comprar (transação atômica no servidor).
   ================================================================ */

const CART_KEY     = 'bibliotroca_carrinho';
const CHECKOUT_KEY = 'bibliotroca_checkout_ids';
const CUPOM_KEY    = 'bibliotroca_checkout_cupom';

/* ── Helpers do carrinho ── */

function getCarrinho() {
    try { return JSON.parse(localStorage.getItem(CART_KEY)) || []; }
    catch (_) { return []; }
}

function saveCarrinho(itens) {
    localStorage.setItem(CART_KEY, JSON.stringify(itens));
}

/* ── Dados do checkout (preenchidos no init) ── */
let _itensSelecionados = [];
let _saldoAtual        = null;
let _totalOriginal     = 0;   // total sem desconto; usado para recalcular ao aplicar/remover cupom
let _cupomCheckout     = null; // { codigo, percentual, desconto, totalComDesconto }
// true  → veio da estante (localStorage); false → compra direta (sessionStorage)
let _compraViaEstante  = true;
// true enquanto o usuário não tiver endereço cadastrado — impede renderFinanceiro de habilitar o botão
let _semEndereco       = false;

/* ── Perfil completo (saldo + endereços) ── */
async function carregarPerfil() {
    try {
        const res = await fetch('/clientes/meu-perfil-json', { credentials: 'include' });
        if (res.status === 401) { window.location.href = '/clientes/login'; return null; }
        if (!res.ok) return null;
        const c = await res.json();
        _saldoAtual = c.saldoTokens || 0;
        const navEl = document.getElementById('navSaldo');
        if (navEl) navEl.textContent = `T$ ${_saldoAtual.toFixed(2)}`;
        return c;
    } catch (_) { return null; }
}

/* ── Saldo do usuário ── */
async function carregarSaldo() {
    const c = await carregarPerfil();
    return c ? (c.saldoTokens || 0) : null;
}

/* ── Exibe / valida endereço de entrega ── */
function carregarEnderecoEntrega(perfil) {
    const card    = document.getElementById('enderecoEntregaCard');
    const btn     = document.getElementById('btnConfirmar');
    const btnWrap = document.getElementById('btnConfirmarWrap');

    if (!perfil || !perfil.enderecos || perfil.enderecos.length === 0) {
        _semEndereco = true;

        if (card) card.innerHTML =
            '<span style="color:var(--muted,#7a6e65);font-size:.83rem;">Nenhum endereço cadastrado.</span><br>' +
            '<button type="button" class="checkout-link-endereco btn-link-inline" onclick="abrirFormEndereco()">+ Adicionar endereço aqui</button>';

        if (btn)     btn.disabled = true;
        if (btnWrap) btnWrap.dataset.tooltip = 'Cadastre um endereço de entrega primeiro';
        return;
    }

    _semEndereco = false;
    if (btnWrap) delete btnWrap.dataset.tooltip;

    const endereco = perfil.enderecos.find(e => e.id === perfil.enderecoSelecionadoId)
                  || perfil.enderecos[0];

    if (card) {
        card.innerHTML =
            `<strong>${escHtml(endereco.rua)}, ${escHtml(endereco.numero)}</strong>` +
            (endereco.complemento ? `<br>${escHtml(endereco.complemento)}` : '') +
            `<br>${escHtml(endereco.bairro)} — ${escHtml(endereco.cidade)}/${escHtml(endereco.estado)}` +
            `<br>CEP: ${escHtml(endereco.cep)}` +
            `<br><button type="button" class="checkout-link-endereco btn-link-inline" style="margin-top:.4rem;" onclick="abrirFormEndereco()">Usar outro endereço →</button>`;
    }
}

/* ── Escapa HTML ── */
function escHtml(str) {
    return String(str || '')
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/* ── Renderiza o resumo dos itens ── */
function renderItens(itens) {
    const lista = document.getElementById('listaCheckout');
    if (!lista) return;

    lista.innerHTML = itens.map(item => {
        let foto = 'https://via.placeholder.com/50x66?text=📚';
        try {
            const arr = JSON.parse(item.fotosUrls);
            if (Array.isArray(arr) && arr.length > 0) foto = arr[0];
        } catch (_) {}

        return `
        <div class="checkout-item">
            <img class="checkout-item-img" src="${foto}" alt="${escHtml(item.titulo)}"
                 onerror="this.src='https://via.placeholder.com/50x66?text=📚'">
            <div class="checkout-item-info">
                <div class="checkout-item-titulo">${escHtml(item.titulo)}</div>
                <div class="checkout-item-autor">${escHtml(item.autor || '')}</div>
            </div>
            <span class="checkout-item-preco">T$ ${(item.precoAprovado || 0).toFixed(2)}</span>
        </div>`;
    }).join('');
}

/* ── Renderiza estado do cupom no checkout (aplicado ou limpo) ── */
function renderCupomCheckout() {
    const inputRow  = document.getElementById('checkoutCupomInputRow');
    const aplicado  = document.getElementById('checkoutCupomAplicado');
    const feedback  = document.getElementById('checkoutCupomFeedback');
    const linhaOrig = document.getElementById('linhaSubtotalOriginal');
    const origEl    = document.getElementById('subtotalOriginal');
    const linhaDesc = document.getElementById('linhaDesconto');
    const labelDesc = document.getElementById('labelDesconto');
    const valorDesc = document.getElementById('valorDesconto');

    if (_cupomCheckout) {
        // Oculta input, mostra bloco "cupom aplicado"
        if (inputRow) inputRow.style.display = 'none';
        if (feedback) feedback.style.display = 'none';
        if (aplicado) {
            aplicado.innerHTML =
                `<div>
                    <span class="cupom-aplicado-info">${escHtml(_cupomCheckout.codigo)} — ${_cupomCheckout.percentual}% OFF</span>
                    <span class="cupom-aplicado-desconto"> -T$ ${(_cupomCheckout.desconto || 0).toFixed(2)}</span>
                 </div>
                 <button class="btn-cupom-remover" onclick="removerCupomCheckout()">✕ Remover</button>`;
            aplicado.style.display = 'flex';
        }
        // Linhas de desconto no card financeiro
        if (linhaOrig) linhaOrig.style.display = 'flex';
        if (origEl)    origEl.textContent       = `T$ ${_totalOriginal.toFixed(2)}`;
        if (linhaDesc) linhaDesc.style.display  = 'flex';
        if (labelDesc) labelDesc.textContent    = `Desconto (${_cupomCheckout.codigo})`;
        if (valorDesc) valorDesc.textContent    = `-T$ ${(_cupomCheckout.desconto || 0).toFixed(2)}`;
    } else {
        // Mostra input, oculta bloco aplicado e linhas de desconto
        if (inputRow) inputRow.style.display = 'flex';
        if (aplicado) aplicado.style.display = 'none';
        if (linhaOrig) linhaOrig.style.display = 'none';
        if (linhaDesc) linhaDesc.style.display = 'none';
    }
}

/* ── Aplicar cupom diretamente no checkout ── */
window.aplicarCupomCheckout = async function() {
    const codigo = (document.getElementById('checkoutCupomCodigo')?.value || '').trim().toUpperCase();
    const feedback = document.getElementById('checkoutCupomFeedback');
    if (!codigo) return;

    const btn = document.querySelector('.btn-cupom-aplicar');
    if (btn) { btn.disabled = true; btn.textContent = '…'; }

    try {
        const res  = await fetch(
            `/api/cupons/validar?codigo=${encodeURIComponent(codigo)}&total=${_totalOriginal}`,
            { credentials: 'include' }
        );
        const data = await res.json();

        if (!res.ok || !data.valido) {
            if (feedback) {
                feedback.className   = 'cupom-feedback erro';
                feedback.textContent = data.mensagem || 'Cupom inválido.';
                feedback.style.display = 'block';
            }
        } else {
            if (feedback) feedback.style.display = 'none';
            _cupomCheckout = {
                codigo:          data.codigo,
                percentual:      data.percentual,
                desconto:        data.desconto,
                totalComDesconto: data.totalComDesconto
            };
            renderCupomCheckout();
            renderFinanceiro(_saldoAtual, _totalOriginal);
        }
    } catch (_) {
        if (feedback) {
            feedback.className   = 'cupom-feedback erro';
            feedback.textContent = 'Erro ao validar cupom. Tente novamente.';
            feedback.style.display = 'block';
        }
    } finally {
        if (btn) { btn.disabled = false; btn.textContent = 'Aplicar'; }
    }
};

/* ── Remover cupom no checkout ── */
window.removerCupomCheckout = function() {
    _cupomCheckout = null;
    sessionStorage.removeItem(CUPOM_KEY);
    const input = document.getElementById('checkoutCupomCodigo');
    if (input) input.value = '';
    renderCupomCheckout();
    renderFinanceiro(_saldoAtual, _totalOriginal);
};

/* ── Atualiza os valores de saldo / total / saldo pós ── */
function renderFinanceiro(saldo, totalOriginal) {
    _saldoAtual    = saldo;
    _totalOriginal = totalOriginal;

    // Recalcula desconto com o total atualizado quando há cupom ativo
    if (_cupomCheckout) {
        const desconto = totalOriginal * (_cupomCheckout.percentual / 100);
        _cupomCheckout.desconto          = desconto;
        _cupomCheckout.totalComDesconto  = Math.max(0, totalOriginal - desconto);
    }

    const total    = _cupomCheckout ? _cupomCheckout.totalComDesconto : totalOriginal;
    const saldoPos = saldo - total;

    const saldoEl = document.getElementById('saldoAtual');
    const totalEl = document.getElementById('totalDebitar');
    const aposEl  = document.getElementById('saldoApos');

    if (saldoEl) saldoEl.textContent = `T$ ${saldo.toFixed(2)}`;
    if (totalEl) totalEl.textContent = `T$ ${total.toFixed(2)}`;

    if (aposEl) {
        aposEl.textContent = `T$ ${Math.max(0, saldoPos).toFixed(2)}`;
        aposEl.classList.toggle('negativo', saldoPos < 0);
    }

    renderCupomCheckout();

    // Alerta de saldo insuficiente
    const alerta = document.getElementById('alertaSaldo');
    const btn    = document.getElementById('btnConfirmar');
    if (saldoPos < 0) {
        const falta = Math.abs(saldoPos).toFixed(2);
        const msg   = document.getElementById('alertaSaldoMsg');
        if (msg) msg.textContent = ` Você precisa de mais T$ ${falta} para concluir esta compra.`;
        if (alerta) alerta.style.display = 'block';
        if (btn)    btn.disabled         = true;
    } else {
        if (alerta) alerta.style.display = 'none';
        /* Só habilita o botão se o usuário já tiver endereço cadastrado */
        if (btn && !_semEndereco) btn.disabled = false;
    }
}

/* ── Toast ── */
function mostrarToast(msg, tipo) {
    const t = document.getElementById('toastCheckout');
    if (!t) return;
    t.className = 'toast toast-' + (tipo || 'info');
    t.innerHTML = msg;
    t.style.display = 'block';
    setTimeout(() => { t.style.display = 'none'; }, 5000);
}

/* ── Confirmar compra ── */
window.confirmarCompra = async function() {
    if (_itensSelecionados.length === 0) return;

    const btn = document.getElementById('btnConfirmar');
    btn.disabled    = true;
    btn.textContent = 'Processando…';

    // Esconde alertas anteriores
    document.getElementById('alertaErro').style.display  = 'none';
    document.getElementById('alertaSaldo').style.display = 'none';

    const livroIds    = _itensSelecionados.map(i => i.id);
    const codigoCupom = _cupomCheckout ? _cupomCheckout.codigo : null;

    try {
        const res = await fetch('/api/livros/carrinho/comprar', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ livroIds, codigoCupom })
        });

        if (res.status === 401) {
            window.location.href = '/clientes/login';
            return;
        }

        const data = await res.json();

        if (!res.ok) {
            // Erro retornado pelo backend (saldo insuficiente, livro indisponível, etc.)
            const msg = typeof data === 'string' ? data : (data.falhas?.[0]?.motivo || 'Erro ao processar a compra.');
            const alertaErro = document.getElementById('alertaErro');
            const alertaMsg  = document.getElementById('alertaErroMsg');

            // Saldo insuficiente → alerta dedicado
            if (msg.toLowerCase().includes('saldo')) {
                const alertaSaldo = document.getElementById('alertaSaldo');
                const alertaSaldoMsg = document.getElementById('alertaSaldoMsg');
                if (alertaSaldoMsg) alertaSaldoMsg.textContent = ' ' + msg;
                if (alertaSaldo)   alertaSaldo.style.display = 'block';
            } else {
                if (alertaMsg)  alertaMsg.textContent      = msg;
                if (alertaErro) alertaErro.style.display   = 'block';
            }
            btn.disabled    = false;
            btn.textContent = 'Confirmar compra';
            return;
        }

        // ── Sucesso ──
        // Monta o set de IDs comprados (usa resposta da API; fallback para os itens enviados)
        const idsComprados = new Set(
            (data.comprados && data.comprados.length > 0)
                ? data.comprados.map(c => c.livroId)
                : _itensSelecionados.map(i => i.id)
        );

        // Remove os livros comprados do localStorage em qualquer fluxo de compra
        saveCarrinho(getCarrinho().filter(i => !idsComprados.has(i.id)));

        // Limpa sessão de checkout
        sessionStorage.removeItem(CHECKOUT_KEY);
        sessionStorage.removeItem(CHECKOUT_KEY + '_direto');
        sessionStorage.removeItem(CUPOM_KEY);

        // Monta dados completos para a página de confirmação
        const agora      = new Date();
        const dataCompra = agora.toLocaleDateString('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' })
                         + ' às ' + agora.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

        const confirmacaoData = {
            comprados: (data.comprados || []).map(c => {
                const itemLocal = _itensSelecionados.find(i => i.id === c.livroId) || {};
                let foto = null;
                try {
                    const arr = JSON.parse(itemLocal.fotosUrls);
                    if (Array.isArray(arr) && arr.length > 0) foto = arr[0];
                } catch (_) {}
                return {
                    pedidoId:     c.pedidoId,
                    codigoPedido: c.codigoPedido || null,
                    livroId:      c.livroId,
                    titulo:       c.titulo || itemLocal.titulo || '',
                    autor:        itemLocal.autor || '',
                    preco:        c.preco,
                    foto
                };
            }),
            totalGasto:    data.totalGasto,
            saldoRestante: data.saldoRestante,
            dataCompra
        };
        sessionStorage.setItem('bibliotroca_confirmacao', JSON.stringify(confirmacaoData));

        // Limpa reserva sem incrementar tentativas (compra efetuada)
        _livrosReservados = [];

        setTimeout(() => { window.location.href = '/livros/pedido-confirmado'; }, 900);

    } catch (err) {
        console.error('Erro ao confirmar compra:', err);
        const alertaErro = document.getElementById('alertaErro');
        const alertaMsg  = document.getElementById('alertaErroMsg');
        if (alertaMsg)  alertaMsg.textContent    = 'Erro de conexão. Verifique sua internet e tente novamente.';
        if (alertaErro) alertaErro.style.display = 'block';
        btn.disabled    = false;
        btn.textContent = 'Confirmar compra';
    }
};

/* ── Formulário inline de endereço ── */
window.abrirFormEndereco = function() {
    const form   = document.getElementById('formEnderecoInline');
    const erroEl = document.getElementById('erroFormEndereco');
    if (form)   form.style.display   = 'block';
    if (erroEl) erroEl.style.display = 'none';
};

window.fecharFormEndereco = function() {
    const form = document.getElementById('formEnderecoInline');
    if (form) form.style.display = 'none';
};

window.mascaraCepCheckout = function(input) {
    let v = input.value.replace(/\D/g, '').substring(0, 8);
    if (v.length > 5) v = v.substring(0, 5) + '-' + v.substring(5);
    input.value = v;
    const statusEl = document.getElementById('cepStatus');
    if (v.replace(/\D/g, '').length === 8) {
        buscarCepCheckout(v.replace(/\D/g, ''));
    } else {
        if (statusEl) { statusEl.className = 'cep-status'; statusEl.textContent = ''; }
    }
};

async function buscarCepCheckout(cep) {
    const statusEl = document.getElementById('cepStatus');
    if (statusEl) { statusEl.className = 'cep-status cep-buscando'; statusEl.textContent = 'Buscando...'; }
    try {
        const res  = await fetch(`https://viacep.com.br/ws/${cep}/json/`);
        if (!res.ok) throw new Error();
        const data = await res.json();
        if (data.erro) {
            if (statusEl) { statusEl.className = 'cep-status cep-erro'; statusEl.textContent = 'CEP não encontrado.'; }
            return;
        }
        const preencher = (id, val) => { const el = document.getElementById(id); if (el && val) el.value = val; };
        preencher('fEndRua',    data.logradouro);
        preencher('fEndBairro', data.bairro);
        preencher('fEndCidade', data.localidade);
        preencher('fEndEstado', (data.uf || '').toUpperCase());
        if (statusEl) { statusEl.className = 'cep-status cep-ok'; statusEl.textContent = '✓ Endereço encontrado'; }
        document.getElementById('fEndNumero')?.focus();
    } catch (_) {
        if (statusEl) { statusEl.className = 'cep-status cep-erro'; statusEl.textContent = 'Erro ao buscar CEP.'; }
    }
}

window.salvarEnderecoCheckout = async function() {
    const rua    = (document.getElementById('fEndRua')?.value         || '').trim();
    const num    = (document.getElementById('fEndNumero')?.value       || '').trim();
    const comp   = (document.getElementById('fEndComplemento')?.value  || '').trim();
    const bairro = (document.getElementById('fEndBairro')?.value       || '').trim();
    const cep    = (document.getElementById('fEndCep')?.value          || '').trim();
    const cidade = (document.getElementById('fEndCidade')?.value       || '').trim();
    const estado = (document.getElementById('fEndEstado')?.value       || '').trim().toUpperCase();

    const erroEl = document.getElementById('erroFormEndereco');
    const mostrarErroEnd = msg => {
        if (erroEl) { erroEl.textContent = msg; erroEl.style.display = 'block'; }
    };

    if (!rua || !num || !bairro || !cep || !cidade || !estado) {
        mostrarErroEnd('Preencha todos os campos obrigatórios (*).');
        return;
    }
    if (estado.length !== 2) {
        mostrarErroEnd('Estado deve ter 2 letras (ex: SP).');
        return;
    }
    if (cep.replace(/\D/g, '').length !== 8) {
        mostrarErroEnd('CEP inválido. Informe os 8 dígitos.');
        return;
    }

    const btnSalvar = document.querySelector('.btn-end-salvar');
    if (btnSalvar) { btnSalvar.disabled = true; btnSalvar.textContent = 'Salvando...'; }
    if (erroEl) erroEl.style.display = 'none';

    try {
        const res = await fetch('/api/clientes/enderecos', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ rua, numero: num, complemento: comp || null,
                                   bairro, cep, cidade, estado, pais: 'BR' })
        });

        if (res.status === 401) { window.location.href = '/clientes/login'; return; }

        if (!res.ok) {
            mostrarErroEnd('Erro ao salvar endereço. Tente novamente.');
            return;
        }

        fecharFormEndereco();
        ['fEndRua','fEndNumero','fEndComplemento','fEndBairro','fEndCep','fEndCidade','fEndEstado']
            .forEach(id => { const el = document.getElementById(id); if (el) el.value = ''; });

        const perfil = await carregarPerfil();
        carregarEnderecoEntrega(perfil);
        renderFinanceiro(_saldoAtual, _totalOriginal);

    } catch (_) {
        mostrarErroEnd('Erro de conexão. Tente novamente.');
    } finally {
        if (btnSalvar) { btnSalvar.disabled = false; btnSalvar.textContent = 'Salvar e usar'; }
    }
};

/* ── Reserva de checkout ── */
const LIMITE_LIVROS_CHECKOUT = 5;
let _countdownInterval = null;
let _livrosReservados  = [];

function mostrarModalExpirada() {
    const modal = document.getElementById('modalReservaExpirada');
    if (modal) modal.style.display = 'flex';
    const btn = document.getElementById('btnConfirmar');
    if (btn) btn.disabled = true;
    setTimeout(() => { window.location.href = '/livros/vitrine'; }, 5000);
}

function mostrarModalIndisponivel(mensagem) {
    const modal = document.getElementById('modalIndisponivel');
    const msg   = document.getElementById('modalIndisonivel-msg');
    if (msg) msg.textContent = mensagem || 'Este produto está temporariamente reservado.';
    if (modal) modal.style.display = 'flex';
}

window.fecharModalIndisponivel = function() {
    const modal = document.getElementById('modalIndisponivel');
    if (modal) modal.style.display = 'none';
    window.location.href = '/livros/estante';
};

function iniciarCountdown(segundos) {
    const el = document.getElementById('reservaCountdown');
    if (!el) return;

    el.style.display = 'flex';

    if (_countdownInterval) clearInterval(_countdownInterval);

    let restantes = segundos;

    const atualizar = () => {
        if (restantes <= 0) {
            clearInterval(_countdownInterval);
            el.innerHTML = '⏰ Reserva expirada';
            el.style.color = '#c0392b';
            liberarReservasSemContagem();
            mostrarModalExpirada();
            return;
        }
        const min = Math.floor(restantes / 60);
        const sec = restantes % 60;
        el.innerHTML = `⏱ Produto reservado por: <strong>${min}:${sec.toString().padStart(2,'0')}</strong>`;
        if (restantes <= 60) el.style.color = '#c0392b';
        restantes--;
    };

    atualizar();
    _countdownInterval = setInterval(atualizar, 1000);
}

async function reservarLivros(livroIds) {
    try {
        const res = await fetch('/api/checkout/reservar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ livroIds })
        });

        if (res.status === 401) { window.location.href = '/clientes/login'; return; }
        const data = await res.json();

        if (!data.reservado) {
            if (data.motivo === 'LIMITE_EXCEDIDO') {
                mostrarToast(data.mensagem, 'erro');
                const btn = document.getElementById('btnConfirmar');
                if (btn) btn.disabled = true;
            } else if (data.motivo === 'BLOQUEADO') {
                mostrarModalIndisponivel(data.mensagem);
            } else if (data.motivo === 'INDISPONIVEL') {
                mostrarModalIndisponivel(data.mensagem);
            }
            return;
        }

        _livrosReservados = livroIds;
        iniciarCountdown(data.duracaoSegundos || 300);

    } catch (_) {
        mostrarToast('Não foi possível reservar o produto. Tente novamente.', 'aviso');
    }
}

async function liberarReservas() {
    if (_livrosReservados.length === 0) return;
    try {
        await fetch('/api/checkout/reservar', {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ livroIds: _livrosReservados })
        });
    } catch (_) {}
}

async function liberarReservasSemContagem() {
    if (_livrosReservados.length === 0) return;
    try {
        await fetch('/api/checkout/reservar', {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ livroIds: _livrosReservados, semContagem: true })
        });
    } catch (_) {}
}

// Libera ao sair da página sem comprar
window.addEventListener('beforeunload', () => {
    if (_livrosReservados.length > 0) {
        navigator.sendBeacon('/api/checkout/reservar',
            new Blob([JSON.stringify({ livroIds: _livrosReservados })],
                { type: 'application/json' }));
    }
});

/* ── Init ── */
document.addEventListener('DOMContentLoaded', async () => {

    // 1. Lê IDs selecionados no sessionStorage
    let ids = [];
    try {
        ids = JSON.parse(sessionStorage.getItem(CHECKOUT_KEY)) || [];
    } catch (_) {}

    const vazio  = document.getElementById('estadoVazio');
    const layout = document.getElementById('checkoutLayout');

    if (ids.length === 0) {
        // Nenhum item selecionado — redireciona para a estante
        if (vazio)  vazio.style.display  = 'block';
        if (layout) layout.style.display = 'none';
        setTimeout(() => { window.location.href = '/livros/estante'; }, 2000);
        return;
    }

    // 2. Detecta fluxo: compra direta (sessionStorage) ou via estante (localStorage)
    const idsSet = new Set(ids.map(Number));
    let dadosDireto = [];
    try {
        dadosDireto = JSON.parse(sessionStorage.getItem(CHECKOUT_KEY + '_direto')) || [];
    } catch (_) {}

    // Só usa compra direta se os IDs do _direto baterem exatamente
    // com os IDs selecionados — evita conflito com compras anteriores
    const idsDireto = new Set(dadosDireto.map(i => Number(i.id)));
    const idsConferem = ids.every(id => idsDireto.has(Number(id))) && ids.length === idsDireto.size;

    if (dadosDireto.length > 0 && idsConferem) {
        _compraViaEstante  = false;
        _itensSelecionados = dadosDireto;
    } else {
        // Via estante ou IDs não conferem — limpa _direto e usa localStorage
        sessionStorage.removeItem(CHECKOUT_KEY + '_direto');
        _compraViaEstante  = true;
        _itensSelecionados = getCarrinho().filter(i => idsSet.has(Number(i.id)));
    }

    if (_itensSelecionados.length === 0) {
        if (vazio)  vazio.style.display  = 'block';
        if (layout) layout.style.display = 'none';
        return;
    }

    // 3. Exibe o layout
    if (vazio)  vazio.style.display  = 'none';
    if (layout) layout.style.display = 'grid';

    // 4. Lê cupom do sessionStorage (vindo da estante)
    try {
        _cupomCheckout = JSON.parse(sessionStorage.getItem(CUPOM_KEY)) || null;
    } catch (_) { _cupomCheckout = null; }

    // 5. Renderiza itens
    renderItens(_itensSelecionados);

    // Verifica limite de livros
    if (_itensSelecionados.length > LIMITE_LIVROS_CHECKOUT) {
        mostrarToast(
            `Limite de ${LIMITE_LIVROS_CHECKOUT} livros por compra. Remova alguns itens para continuar.`,
            'aviso'
        );
        const btn = document.getElementById('btnConfirmar');
        if (btn) btn.disabled = true;
    } else {
        // Inicia reserva
        const livroIds = _itensSelecionados.map(i => Number(i.id));
        reservarLivros(livroIds);
    }

    // 6. Carrega perfil, valida endereço e calcula financeiro
    const perfil = await carregarPerfil();
    carregarEnderecoEntrega(perfil);
    const total = _itensSelecionados.reduce((s, i) => s + (i.precoAprovado || 0), 0);
    renderFinanceiro(perfil ? (perfil.saldoTokens || 0) : 0, total);
});
