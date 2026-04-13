let userState = { saldo: 0.0, nome: "" };

// Ao carregar a página
document.addEventListener("DOMContentLoaded", () => {
    loadUserProfile();
    carregarHistorico();

    const metodoSelect = document.getElementById('metodoPagamento');
    const sectionCartao = document.getElementById('sectionCartao');

    // Alternar campos de cartão
    metodoSelect.addEventListener('change', (e) => {
        sectionCartao.classList.toggle('hidden', e.target.value === 'PIX');
    });

    // Interceptar envio do formulário
    document.getElementById('formCompra').addEventListener('submit', efetuarCompra);
});

async function loadUserProfile() {
    try {
        const response = await fetch('/clientes/meu-perfil');
        if (response.ok) {
            const data = await response.json();
            userState.saldo = data.saldoTokens || 0.0;
            userState.nome = data.nome;
            updateUI();
        }
    } catch (error) { console.error("Erro ao carregar perfil:", error); }
}
async function efetuarCompra(e) {
    e.preventDefault();
    const btn = document.getElementById('btnComprar');
    const valor = document.getElementById('valor').value;
    const metodo = document.getElementById('metodoPagamento').value;
    const cartao = document.getElementById('numCartao').value;

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-circle-notch animate-spin"></i> Processando...';

    try {
        const response = await fetch('/api/tokens/comprar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                valor: parseFloat(valor),
                metodoPagamento: metodo,
                numeroCartao: metodo === 'CARTAO' ? cartao : null
            })
        });

        const data = await response.json();

        if (response.ok) {
            // VERIFICA SE É PIX
            if (data.qrCodeBase64) {
                document.getElementById('imgQrCode').src = data.qrCodeBase64;
                document.getElementById('textoCopiaECola').value = data.pixCopiaECola;
                document.getElementById('modalPix').classList.remove('hidden');

                iniciarVerificacao(data.pagamentoId);

            } else {
                // SUCESSO DIRETO (CARTÃO)
                userState.saldo = data.saldoTokens;
                updateUI();
                carregarHistorico();
                alert("Sucesso! Seus tokens já estão disponíveis.");
                e.target.reset();
            }
        } else {
            alert("Erro no pagamento: " + (data.message || "Verifique os dados."));
        }
    } catch (error) {
        console.error(error);
        alert("Erro técnico de conexão.");
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-check-double"></i> Confirmar e Pagar';
    }
}

// Função para copiar o código PIX
function copyPix() {
    const input = document.getElementById('textoCopiaECola');
    input.select();
    navigator.clipboard.writeText(input.value);
    alert("Código copiado!");
}

function fecharModalPix() {
    document.getElementById('modalPix').classList.add('hidden');
    loadUserProfile();
    carregarHistorico();
}

async function carregarHistorico() {
    const tbody = document.getElementById('lista-transacoes');
    try {
        const res = await fetch('/api/tokens/historico');
        if (res.ok) {
            const transacoes = await res.json();
            tbody.innerHTML = transacoes.map(t => `
                <tr class="border-b border-slate-100 hover:bg-slate-50 transition">
                    <td class="p-4">${new Date(t.dataHora).toLocaleString('pt-BR')}</td>
                    <td class="p-4 font-bold text-indigo-600">T$ ${t.valor.toFixed(2)}</td>
                    <td class="p-4"><span class="px-2 py-1 bg-slate-100 rounded text-xs">${t.metodoPagamento}</span></td>
                    <td class="p-4 text-xs text-gray-400">${t.finalCartao ? 'Final ****' + t.finalCartao : '-'}</td>
                </tr>
            `).join('');
        }
    } catch (e) { console.error("Erro histórico:", e); }
}

let intervaloCheck; // Variável global para controlar o timer

function iniciarVerificacao(pagamentoId) {
    if (intervaloCheck) clearInterval(intervaloCheck);

    console.log("Iniciando verificação do PIX: " + pagamentoId);

    intervaloCheck = setInterval(async () => {
        try {
            const response = await fetch(`/api/tokens/verificar-pagamento/${pagamentoId}`);
            const data = await response.json();

            if (data.status === "APROVADO") {
                console.log("Pagamento aprovado detectado!");

                // 1. Para o cronômetro
                clearInterval(intervaloCheck);

                // 2. Fecha o modal (Garante que a classe 'hidden' seja adicionada)
                const modal = document.getElementById('modalPix');
                modal.classList.add('hidden');

                // 3. Feedback para o usuário
                alert("🚀 Sucesso! O pagamento foi compensado e seus tokens já estão disponíveis.");

                // 4. Atualiza a tela sem dar F5
                if (typeof loadUserProfile === "function") loadUserProfile();
                if (typeof carregarHistorico === "function") carregarHistorico();
            }
        } catch (e) {
            console.error("Erro ao verificar status do PIX:", e);
        }
    }, 3000); // Tenta a cada 3 segundos
}

function confirmarPagamentoSucesso() {
    document.getElementById('modalPix').classList.add('hidden');
    alert("🚀 Pagamento detectado com sucesso! Seus tokens foram adicionados.");
    loadUserProfile();
    carregarHistorico();
}

async function simularAvisoBanco() {
    const fullCode = document.getElementById('textoCopiaECola').value;

    const regex = /PX-\d+/;
    const match = fullCode.match(regex);
    const pagamentoId = match ? match[0] : null;

    if (!pagamentoId) {
        console.error("ID de pagamento não encontrado no código PIX");
        return;
    }

    try {
        const response = await fetch(`/api/tokens/simular-webhook/${pagamentoId}`);
        if (response.ok) {
            console.log("Comando de aprovação enviado para o ID: " + pagamentoId);
        }
    } catch (e) {
        console.error("Erro ao simular aprovação", e);
    }
}

function updateUI() {
    var saldoEl = document.getElementById('displaySaldo');
    if (saldoEl) saldoEl.innerText = userState.saldo.toFixed(2);
    var nomeEl = document.getElementById('displayNome');
    if (nomeEl) nomeEl.innerText = userState.nome;
    var navSaldo = document.getElementById('navSaldo');
    if (navSaldo) navSaldo.textContent = 'T$ ' + userState.saldo.toFixed(2);
}