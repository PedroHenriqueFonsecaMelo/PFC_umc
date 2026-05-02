/**
 * APP.JS - Lógica Centralizada
 */

document.addEventListener("DOMContentLoaded", () => {
    // 1. Inicializa componentes de formulários dinâmicos (Endereços/Cartões)
    initDynamicForms();

    // 2. Máscara de CPF no cadastro
    const campoCpf = document.getElementById('cpf');
    if (campoCpf) campoCpf.addEventListener('input', mascaraCpf);

    // 3. Auto-marcar checkboxes ao fechar modais de termos
    initTermosModal();

    // 4. Escuta o formulário de Compra de Tokens
    const compraForm = document.getElementById('compraTokensForm');
    if (compraForm) compraForm.addEventListener('submit', handleCompraTokens);

    // 5. Escuta o botão de Logout
    const btnLogout = document.getElementById('btnLogout');
    if (btnLogout) btnLogout.addEventListener('click', logout);

    // 6. Se houver um display de saldo, carrega os dados do usuário logado
    if (document.getElementById('displaySaldo')) {
        carregarDadosUsuario();
    }
});

/* ============================================================
   MÁSCARA DE CPF  →  000.000.000-00
   ============================================================ */

function mascaraCpf(e) {
    let v = e.target.value.replace(/\D/g, '').slice(0, 11);
    if (v.length > 9) v = v.replace(/(\d{3})(\d{3})(\d{3})(\d{0,2})/, '$1.$2.$3-$4');
    else if (v.length > 6) v = v.replace(/(\d{3})(\d{3})(\d{0,3})/, '$1.$2.$3');
    else if (v.length > 3) v = v.replace(/(\d{3})(\d{0,3})/, '$1.$2');
    e.target.value = v;
}

/* ============================================================
   MODAIS DE TERMOS — fecha e marca o checkbox automaticamente
   ============================================================ */

function initTermosModal() {
    // Botão fechar modal Termos de Uso
    const btnFecharTermos = document.getElementById('btnFecharTermos');
    if (btnFecharTermos) {
        btnFecharTermos.addEventListener('click', () => {
            document.getElementById('modal').style.display = 'none';
            const cb = document.getElementById('termsAccepted');
            if (cb) cb.checked = true;
        });
    }

    // Botão fechar modal Política de Privacidade
    const btnFecharPrivacidade = document.getElementById('btnFecharPrivacidade');
    if (btnFecharPrivacidade) {
        btnFecharPrivacidade.addEventListener('click', () => {
            document.getElementById('modal2').style.display = 'none';
            const cb = document.getElementById('privacyAccepted');
            if (cb) cb.checked = true;
        });
    }
}

/* ============================================================
   AUTENTICAÇÃO
   ============================================================ */

function logout() {
    localStorage.removeItem('token');
    window.location.href = '/clientes/sair';
}

/* ============================================================
   LÓGICA DE CARREGAMENTO DE DADOS
   ============================================================ */

async function carregarDadosUsuario() {
    const token = localStorage.getItem('token');
    const displaySaldo = document.getElementById('displaySaldo');

    if (!token) return;

    try {
        const response = await fetch('/clientes/meu-perfil-json', {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            const cliente = await response.json();
            if (displaySaldo) displaySaldo.innerText = cliente.saldoTokens.toFixed(2);

            const welcomeMsg = document.getElementById('welcomeMsg');
            if (welcomeMsg) welcomeMsg.innerText = `Olá, ${cliente.nome}!`;
        } else if (response.status === 401) {
            logout();
        }
    } catch (error) {
        console.error("Erro ao carregar dados do usuário:", error);
    }
}

/* ============================================================
   LÓGICA DE COMPRA DE TOKENS
   ============================================================ */

async function handleCompraTokens(event) {
    event.preventDefault();
    const token = localStorage.getItem('token');

    if (!token) {
        alert("Sessão expirada. Faça login novamente.");
        window.location.href = '/clientes/login';
        return;
    }

    const payload = {
        valor: parseFloat(document.getElementById('valorTokens').value),
        metodoPagamento: document.getElementById('metodoPagamento').value,
        numeroCartao: document.getElementById('numeroCartao')?.value || ""
    };

    try {
        const response = await fetch('/api/tokens/comprar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            const data = await response.json();
            alert("Tokens creditados com sucesso!");
            if (document.getElementById('displaySaldo')) {
                document.getElementById('displaySaldo').innerText = data.saldoTokens.toFixed(2);
            }
        } else {
            const erro = await response.text();
            alert("Erro no pagamento: " + erro);
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

function adicionarEndereco() { adicionarItem('enderecos'); }
function adicionarCartao() { adicionarItem('cartoes'); }

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

    wrapper.querySelector('.btn-remover').addEventListener('click', () => {
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
            // Salva o token para as próximas requisições
            localStorage.setItem('token', data.token);
            // Redireciona para a vitrine (ou home)
            window.location.href = '/vitrine';
        } else {
            if (msgErro) msgErro.classList.remove('hidden');
        }
    } catch (error) {
        console.error("Erro no login:", error);
        alert("Erro ao conectar com o servidor.");
    } finally {
        btn.disabled = false;
    }
}

function logout() {
    localStorage.removeItem('token');
    window.location.href = '/clientes/login';
}

/* ============================================================
   LÓGICA DE CARREGAMENTO DE DADOS
   ============================================================ */

async function carregarDadosUsuario() {
    const token = localStorage.getItem('token');
    const displaySaldo = document.getElementById('displaySaldo');

    if (!token) return;

    try {
        const response = await fetch('/clientes/meu-perfil-json', {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            const cliente = await response.json();
            if (displaySaldo) displaySaldo.innerText = cliente.saldoTokens.toFixed(2);

            const welcomeMsg = document.getElementById('welcomeMsg');
            if (welcomeMsg) welcomeMsg.innerText = `Olá, ${cliente.nome}!`;
        } else if (response.status === 401) {
            logout();
        }
    } catch (error) {
        console.error("Erro ao carregar dados do usuário:", error);
    }
}

/* ============================================================
   LÓGICA DE COMPRA DE TOKENS
   ============================================================ */

async function handleCompraTokens(event) {
    event.preventDefault();
    const token = localStorage.getItem('token');

    if (!token) {
        alert("Sessão expirada. Faça login novamente.");
        window.location.href = '/clientes/login';
        return;
    }

    const payload = {
        valor: parseFloat(document.getElementById('valorTokens').value),
        metodoPagamento: document.getElementById('metodoPagamento').value, // Certifique-se que este ID existe no HTML
        numeroCartao: document.getElementById('numeroCartao')?.value || ""
    };

    try {
        const response = await fetch('/api/tokens/comprar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            const data = await response.json();
            alert("Tokens creditados com sucesso!");
            if (document.getElementById('displaySaldo')) {
                document.getElementById('displaySaldo').innerText = data.saldoTokens.toFixed(2);
            }
        } else {
            const erro = await response.text();
            alert("Erro no pagamento: " + erro);
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

    wrapper.querySelector('.btn-remover').addEventListener('click', () => {
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
            // Substitui nomeArray[X] por nomeArray[novoIndex]
            const newName = name.replace(new RegExp(arrayName + "\\[[0-9]+\\]"), `${arrayName}[${idx}]`);
            field.setAttribute("name", newName);
        });
    });
}