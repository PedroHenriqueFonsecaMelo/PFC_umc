/**
 * script.js — Helpers globais do cliente · Bibliotroca
 * Inclui: validação de senha, máscara CPF, data nascimento,
 * toggle de senha, modais de termos, formulários dinâmicos
 * e confirmação de ações destrutivas.
 */

document.addEventListener("DOMContentLoaded", () => {
    initDynamicForms();

    const campoCpf = document.getElementById('cpf');
    if (campoCpf) campoCpf.addEventListener('input', mascaraCpf);

    initTermosModal();

    const btnLogout = document.getElementById('btnLogout');
    if (btnLogout) btnLogout.addEventListener('click', logout);

    initValidacaoSenha();
    initDataNascimento();

    document.querySelectorAll('[data-confirm]').forEach(function (el) {
        el.addEventListener('click', function (e) {
            const msg = this.dataset.confirm || 'Tem certeza? Esta operação não pode ser desfeita.';
            if (!confirm(msg)) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    });
});

/* ============================================================
   VALIDAÇÃO VISUAL DE SENHA
   ============================================================ */

function initValidacaoSenha() {
    const campo = document.getElementById('senha');
    const lista = document.getElementById('senha-requisitos');
    if (!campo || !lista) return;

    campo.addEventListener('focus', () => { lista.style.display = 'block'; });

    campo.addEventListener('input', () => {
        const v = campo.value;
        atualizarReq('req-tamanho',   v.length >= 8 && v.length <= 20);
        atualizarReq('req-maiuscula', /[A-Z]/.test(v));
        atualizarReq('req-minuscula', /[a-z]/.test(v));
        atualizarReq('req-numero',    /[0-9]/.test(v));
        atualizarReq('req-especial',  /[@#$%^&+=!]/.test(v));
        atualizarReq('req-espaco',    v.length > 0 && !/\s/.test(v));
    });
}

function atualizarReq(id, valido) {
    const el = document.getElementById(id);
    if (!el) return;
    const texto = el.textContent.replace(/^[✓✗] /, '');
    el.textContent = (valido ? '✓ ' : '✗ ') + texto;
    el.style.color = valido ? '#2e7d32' : '#c62828';
}

/* ============================================================
   DATA DE NASCIMENTO — máscara DD/MM/AAAA, valida data futura e menor de 18
   ============================================================ */

function initDataNascimento() {
    const campo = document.getElementById('datanasc');
    if (!campo) return;

    if (/^\d{4}-\d{2}-\d{2}$/.test(campo.value)) {
        const [y, m, d] = campo.value.split('-');
        campo.value = `${d}/${m}/${y}`;
    }

    campo.addEventListener('input', (e) => {
        let v = e.target.value.replace(/\D/g, '').slice(0, 8);
        if (v.length > 4) v = v.replace(/(\d{2})(\d{2})(\d{0,4})/, '$1/$2/$3');
        else if (v.length > 2) v = v.replace(/(\d{2})(\d{0,2})/, '$1/$2');
        e.target.value = v;
        validarDataNascimento();
    });

    campo.addEventListener('blur', validarDataNascimento);
}

function validarDataNascimento() {
    const campo = document.getElementById('datanasc');
    const avisoFutura = document.getElementById('aviso-data-futura');
    const avisoIdade  = document.getElementById('aviso-menor-idade');
    if (!campo || !avisoFutura || !avisoIdade) return;

    avisoFutura.style.display = 'none';
    avisoIdade.style.display  = 'none';

    const val = campo.value;
    if (!val || val.length < 10) return;

    const [dia, mes, ano] = val.split('/').map(Number);
    if (!dia || !mes || !ano || ano < 1000) return;

    const nasc = new Date(ano, mes - 1, dia);
    if (nasc.getDate() !== dia || nasc.getMonth() !== mes - 1) return;

    const hoje = new Date();
    hoje.setHours(0, 0, 0, 0);

    if (nasc >= hoje) { avisoFutura.style.display = 'block'; return; }

    let idade = hoje.getFullYear() - nasc.getFullYear();
    const diffMes = hoje.getMonth() - nasc.getMonth();
    if (diffMes < 0 || (diffMes === 0 && hoje.getDate() < nasc.getDate())) idade--;

    if (idade < 18) avisoIdade.style.display = 'block';
}

/* ============================================================
   TOGGLE SENHA
   ============================================================ */

const SVG_OLHO_ABERTO = `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>`;
const SVG_OLHO_FECHADO = `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>`;

function toggleSenha(inputId, btn) {
    const input = document.getElementById(inputId);
    if (!input) return;
    const mostrar = input.type === 'password';
    input.type = mostrar ? 'text' : 'password';
    btn.innerHTML = mostrar ? SVG_OLHO_FECHADO : SVG_OLHO_ABERTO;
    btn.setAttribute('aria-label', mostrar ? 'Ocultar senha' : 'Mostrar senha');
}

/* ============================================================
   MÁSCARA DE CPF
   ============================================================ */

function mascaraCpf(e) {
    let v = e.target.value.replace(/\D/g, '').slice(0, 11);
    if (v.length > 9) v = v.replace(/(\d{3})(\d{3})(\d{3})(\d{0,2})/, '$1.$2.$3-$4');
    else if (v.length > 6) v = v.replace(/(\d{3})(\d{3})(\d{0,3})/, '$1.$2.$3');
    else if (v.length > 3) v = v.replace(/(\d{3})(\d{0,3})/, '$1.$2');
    e.target.value = v;
}

/* ============================================================
   MODAIS DE TERMOS
   ============================================================ */

function initTermosModal() {
    const btnFecharTermos = document.getElementById('btnFecharTermos');
    if (btnFecharTermos) {
        btnFecharTermos.addEventListener('click', () => {
            document.getElementById('modal').style.display = 'none';
            const cb = document.getElementById('termsAccepted');
            if (cb) cb.checked = true;
        });
    }

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
