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
        const response = await fetch('/clientes/meu-perfil'); // Ajustado para rota relativa
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

        if (response.ok) {
            const data = await response.json();
            userState.saldo = data.saldoTokens;
            updateUI();
            carregarHistorico(); // Atualiza a tabela imediatamente
            alert("Sucesso! Seus tokens já estão disponíveis.");
            e.target.reset();
        } else {
            const erro = await response.text();
            alert("Erro no pagamento: " + erro);
        }
    } catch (error) {
        alert("Erro técnico de conexão.");
    } finally {
        btn.disabled = false;
        btn.innerText = "Confirmar Compra";
    }
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

function updateUI() {
    document.getElementById('displaySaldo').innerText = userState.saldo.toFixed(2);
    document.getElementById('displayNome').innerText = userState.nome;
}