// Simulação de um "Estado Global" na página
let userState = {
    saldo: 0.0,
    nome: ""
};

// 1. Função para carregar dados do perfil (incluindo o saldo inicial)
async function loadUserProfile() {
    const token = localStorage.getItem('token');
    if (!token) return;

    try {
        const response = await fetch('https://localhost:8443/clientes/meu-perfil', {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.ok) {
            const data = await response.json();
            userState.saldo = data.saldoTokens || 0.0;
            userState.nome = data.nome;
            updateUI(); // Atualiza o HTML
        }
    } catch (error) {
        console.error("Erro ao carregar perfil:", error);
    }
}

// 2. Função de Compra (conecta com o Controller que criámos)
async function efetuarCompra() {
    const valorInput = document.getElementById('valorTokens').value;
    const cartaoInput = document.getElementById('numeroCartao').value;
    const token = localStorage.getItem('token');

    if (!valorInput || valorInput <= 0) {
        alert("Insira um valor válido.");
        return;
    }

    // Feedback visual de "A processar..."
    const btn = document.getElementById('btnComprar');
    btn.innerText = "A processar pagamento...";
    btn.disabled = true;

    try {
        const response = await fetch('https://localhost:8443/clientes/tokens/comprar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                valor: parseFloat(valorInput),
                numeroCartao: cartaoInput
            })
        });

        if (response.ok) {
            const clienteAtualizado = await response.json();
            
            // ATUALIZAÇÃO DO ESTADO: O ponto mais importante
            userState.saldo = clienteAtualizado.saldoTokens;
            
            alert("Compra realizada com sucesso!");
            updateUI(); // Reflete o novo saldo no ecrã imediatamente
        } else {
            const msg = await response.text();
            alert("Falha: " + msg);
        }
    } catch (error) {
        alert("Erro de conexão com o servidor.");
    } finally {
        btn.innerText = "Confirmar Compra";
        btn.disabled = false;
    }
}

// 3. Função que manipula o DOM (HTML)
function updateUI() {
    document.getElementById('displaySaldo').innerText = userState.saldo.toFixed(2);
    document.getElementById('displayNome').innerText = userState.nome;
}

// Inicialização ao carregar a página
window.onload = loadUserProfile;

let metodoSelecionado = 'CARTAO'; // Padrão

document.addEventListener("DOMContentLoaded", () => {
    verificarCartao();

    // Seleção de Método
    document.getElementById('btnSelectCartao').addEventListener('click', () => {
        metodoSelecionado = 'CARTAO';
        document.getElementById('sectionCartao').classList.remove('hidden');
        document.getElementById('msgPix').classList.add('hidden');
    });

    document.getElementById('btnSelectPix').addEventListener('click', () => {
        metodoSelecionado = 'PIX';
        document.getElementById('sectionCartao').classList.add('hidden');
        document.getElementById('msgPix').classList.remove('hidden');
    });

    document.getElementById('formCompra').addEventListener('submit', efetuarCompra);
});

async function verificarCartao() {
    const token = localStorage.getItem('token');
    const res = await fetch('/clientes/meu-perfil/tem-cartao', {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    const temCartao = await res.json();

    if (!temCartao) {
        alert("Atenção: Você não possui cartões cadastrados. Use PIX ou cadastre um cartão no perfil.");
        document.getElementById('btnSelectCartao').disabled = true;
    }
}

async function efetuarCompra(e) {
    e.preventDefault();
    const token = localStorage.getItem('token');

    const payload = {
        valor: parseFloat(document.getElementById('valor').value),
        metodoPagamento: metodoSelecionado,
        numeroCartao: metodoSelecionado === 'CARTAO' ? document.getElementById('numCartao').value : null
    };

    const res = await fetch('/clientes/tokens/comprar', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload)
    });

    if (res.ok) {
        alert("Compra registrada com sucesso!");
        location.reload();
    } else {
        const erro = await res.text();
        alert("Erro: " + erro);
    }
}