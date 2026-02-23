/**
 * APP.JS - Lógica Centralizada (Sem JS no HTML)
 */

document.addEventListener("DOMContentLoaded", () => {
    // 1. Inicializa componentes de formulários dinâmicos
    initDynamicForms();

    // 2. Escuta o formulário de Login
    const loginForm = document.getElementById('loginForm');
    if (loginForm) loginForm.addEventListener('submit', handleLogin);

    // 3. Escuta o formulário de Compra de Tokens
    const compraForm = document.getElementById('compraTokensForm');
    if (compraForm) compraForm.addEventListener('submit', handleCompraTokens);
    
    // 4. Se houver um display de saldo, carrega os dados do usuário
    if (document.getElementById('displaySaldo')) {
        carregarDadosUsuario();
    }
});

/* ============================================================
   LÓGICA DE AUTENTICAÇÃO (JWT)
   ============================================================ */

async function handleLogin(event) {
    event.preventDefault();
    const btn = event.target.querySelector('button');
    const msgErro = document.getElementById('msg-erro');
    
    const loginData = {
        email: document.getElementById('email').value,
        senha: document.getElementById('senha').value
    };

    try {
        btn.disabled = true;
        const response = await fetch('/clientes/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(loginData)
        });

        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('token', data.token);
            window.location.href = '/home';
        } else {
            if (msgErro) msgErro.classList.remove('hidden');
        }
    } catch (error) {
        console.error("Erro no login:", error);
    } finally {
        btn.disabled = false;
    }
}

function logout() {
    localStorage.removeItem('token');
    window.location.href = '/login';
}

/* ============================================================
   LÓGICA DE COMPRA DE TOKENS
   ============================================================ */

async function handleCompraTokens(event) {
    event.preventDefault();
    const token = localStorage.getItem('token');

    if (!token) {
        alert("Sessão expirada ou usuário não logado.");
        window.location.href = '/login';
        return;
    }

    const payload = {
        valor: parseFloat(document.getElementById('valorTokens').value),
        numeroCartao: document.getElementById('numeroCartao').value
    };

    try {
        const response = await fetch('/clientes/tokens/comprar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            const data = await response.json();
            alert("Tokens creditados!");
            document.getElementById('displaySaldo').innerText = data.saldoTokens.toFixed(2);
        } else {
            const erro = await response.text();
            alert("Erro: " + erro);
        }
    } catch (error) {
        console.error("Falha na compra:", error);
    }
}

/* ============================================================
   GERENCIAMENTO DINÂMICO DE ENDEREÇOS E CARTÕES
   ============================================================ */

function initDynamicForms() {
    const btnAddEndereco = document.getElementById('btnAddEndereco');
    const btnAddCartao = document.getElementById('btnAddCartao');

    if (btnAddEndereco) btnAddEndereco.addEventListener('click', () => adicionarItem('enderecos'));
    if (btnAddCartao) btnAddCartao.addEventListener('click', () => adicionarItem('cartoes'));
}

function adicionarItem(tipo) {
    const containerId = tipo === 'enderecos' ? 'enderecos-list-container' : 'cartoes-list-container';
    const container = document.getElementById(containerId);
    if (!container) return;

    const wrapper = document.createElement("div");
    wrapper.className = `${tipo.slice(0, -1)}-item item-card`;

    const htmlEndereco = `
        <fieldset>
            <legend>Endereço</legend>
            <div class="form-grid">
                <input type="text" name="${tipo}[0].cep" placeholder="CEP"/>
                <input type="text" name="${tipo}[0].rua" placeholder="Rua"/>
                <input type="text" name="${tipo}[0].numero" placeholder="Nº"/>
                <button type="button" class="btn-remover">Remover</button>
            </div>
        </fieldset>`;

    const htmlCartao = `
        <fieldset>
            <legend>Cartão</legend>
            <div class="form-grid">
                <input type="text" name="${tipo}[0].numero" placeholder="Número do Cartão"/>
                <input type="text" name="${tipo}[0].nomeTitular" placeholder="Titular"/>
                <button type="button" class="btn-remover">Remover</button>
            </div>
        </fieldset>`;

    wrapper.innerHTML = tipo === 'enderecos' ? htmlEndereco : htmlCartao;
    
    // Evento de remoção (Sem JS no HTML)
    wrapper.querySelector('.btn-remover').addEventListener('click', (e) => {
        wrapper.remove();
        reorganizarIndices(`#${containerId}`, `.${tipo.slice(0, -1)}-item`, tipo);
    });

    container.appendChild(wrapper);
    reorganizarIndices(`#${containerId}`, `.${tipo.slice(0, -1)}-item`, tipo);
}

function reorganizarIndices(selectorContainer, itemSelector, arrayName) {
    const container = document.querySelector(selectorContainer);
    if (!container) return;
    const items = container.querySelectorAll(itemSelector);

    items.forEach((item, idx) => {
        item.querySelectorAll("[name]").forEach(field => {
            const name = field.getAttribute("name");
            if (!name) return;
            const newName = name.replace(new RegExp(arrayName + "\\[[0-9]+\\]"), `${arrayName}[${idx}]`);
            field.setAttribute("name", newName);
        });
    });
}