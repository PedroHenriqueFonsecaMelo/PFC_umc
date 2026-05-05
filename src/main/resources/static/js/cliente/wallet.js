const TOKENS_POR_REAL = 2;

let intervaloCheck = null;

document.addEventListener("DOMContentLoaded", () => {
    carregarSaldo();
    carregarHistorico();
    document.getElementById('formCompra').addEventListener('submit', efetuarCompra);
});

function atualizarPreview() {
    const valor = parseFloat(document.getElementById('valor').value) || 0;
    const preview = document.getElementById('tokensPreview');
    const previewTokens = document.getElementById('previewTokens');

    if (valor >= 1) {
        previewTokens.textContent = (valor * TOKENS_POR_REAL).toFixed(0);
        preview.style.display = 'flex';
    } else {
        preview.style.display = 'none';
    }
}

async function carregarSaldo() {
    try {
        const res = await fetch('/clientes/meu-perfil-json', { credentials: 'include' });
        if (res.ok) {
            const data = await res.json();
            document.getElementById('displaySaldo').textContent = (data.saldoTokens || 0).toFixed(2);
            const navSaldo = document.getElementById('navSaldo');
            if (navSaldo) navSaldo.textContent = 'T$ ' + (data.saldoTokens || 0).toFixed(2);
        }
    } catch (e) {
        console.error("Erro ao carregar saldo:", e);
    }
}

async function efetuarCompra(e) {
    e.preventDefault();
    const btn = document.getElementById('btnComprar');
    const valor = parseFloat(document.getElementById('valor').value);

    if (!valor || valor < 1) {
        alert("Informe um valor mínimo de R$ 1,00.");
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Gerando PIX...';

    try {
        const res = await fetch('/api/tokens/comprar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ valor })
        });

        const data = await res.json();

        if (!res.ok) {
            alert(data.message || data || "Erro ao gerar o PIX.");
            return;
        }

        // Exibe modal com QR Code
        const tokens = (valor * TOKENS_POR_REAL).toFixed(0);
        document.getElementById('pixValorInfo').textContent =
            `R$ ${valor.toFixed(2)} → T$ ${tokens} tokens`;
        document.getElementById('imgQrCode').src = data.qrCodeBase64;
        document.getElementById('textoCopiaECola').value = data.pixCopiaECola;
        document.getElementById('pixStatus').innerHTML =
            '<i class="fa-solid fa-circle-notch fa-spin"></i> Aguardando confirmação do pagamento...';
        document.getElementById('modalPix').style.display = 'flex';

        iniciarVerificacao(data.pagamentoId);

    } catch (err) {
        console.error(err);
        alert("Erro de conexão. Tente novamente.");
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-qrcode"></i> Gerar QR Code PIX';
    }
}

function copyPix() {
    const input = document.getElementById('textoCopiaECola');
    input.select();
    navigator.clipboard.writeText(input.value)
        .then(() => alert("Código copiado!"))
        .catch(() => {
            document.execCommand('copy');
            alert("Código copiado!");
        });
}

function fecharModalPix() {
    if (intervaloCheck) clearInterval(intervaloCheck);
    document.getElementById('modalPix').style.display = 'none';
    carregarSaldo();
    carregarHistorico();
}

function iniciarVerificacao(pagamentoId) {
    if (intervaloCheck) clearInterval(intervaloCheck);

    intervaloCheck = setInterval(async () => {
        try {
            const res = await fetch(`/api/tokens/verificar-pagamento/${pagamentoId}`, {
                credentials: 'include'
            });
            const data = await res.json();

            if (data.status === "APROVADO") {
                clearInterval(intervaloCheck);
                document.getElementById('pixStatus').innerHTML =
                    '<i class="fa-solid fa-check-circle" style="color:#4a5d23"></i> Pagamento confirmado! Tokens creditados.';

                setTimeout(() => {
                    fecharModalPix();
                }, 2500);
            }
        } catch (e) {
            console.error("Erro ao verificar PIX:", e);
        }
    }, 4000);
}

async function carregarHistorico() {
    const tbody = document.getElementById('lista-transacoes');
    try {
        const res = await fetch('/api/tokens/historico', { credentials: 'include' });
        if (!res.ok) return;

        const transacoes = await res.json();

        if (!transacoes.length) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:#999;padding:1.5rem;">Nenhuma transação ainda.</td></tr>';
            return;
        }

        tbody.innerHTML = transacoes.map(t => {
            const statusClass = t.status === 'CONCLUIDO' ? 'status-ok' : 'status-pendente';
            const statusLabel = t.status === 'CONCLUIDO' ? 'Confirmado' : 'Pendente';
            const data = new Date(t.dataHora).toLocaleString('pt-BR');
            const id = t.pagamentoId ? t.pagamentoId.substring(0, 12) + '...' : '-';
            return `
                <tr>
                    <td>${data}</td>
                    <td class="valor-tokens">T$ ${t.valor.toFixed(2)}</td>
                    <td><span class="badge-status ${statusClass}">${statusLabel}</span></td>
                    <td class="ref-id" title="${t.pagamentoId || ''}">${id}</td>
                </tr>`;
        }).join('');
    } catch (e) {
        console.error("Erro ao carregar histórico:", e);
    }
}
