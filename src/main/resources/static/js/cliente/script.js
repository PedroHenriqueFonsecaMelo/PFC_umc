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

    initValidacaoSenha('senha',     'senha-requisitos');
    initValidacaoSenha('novaSenha', 'senha-requisitos');
    initDataNascimento();
    initForcaSenha('senha',     'forca-senha-container');
    initForcaSenha('novaSenha', 'forca-nova-senha-container');
    initConfirmarSenha();
    initAlterarSenha();
    initResetSenha();

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

function initValidacaoSenha(campoId, listaId) {
    const campo = document.getElementById(campoId || 'senha');
    const lista = document.getElementById(listaId || 'senha-requisitos');
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
   INDICADOR DE FORÇA DA SENHA — padrão NIST 2024, 4 níveis
   ============================================================ */

const SENHAS_PROIBIDAS = new Set([
    '123456','password','123456789','qwerty','abc123','111111',
    '12345678','iloveyou','admin','letmein','monkey','dragon',
    'master','sunshine','princess','welcome','shadow','superman',
    'michael','jessica'
]);

function avaliarForcaSenha(senha) {
    if (!senha) return null;
    if (senha.length < 8 || SENHAS_PROIBIDAS.has(senha.toLowerCase())) return 'fraca';

    const temMaiuscula = /[A-Z]/.test(senha);
    const temMinuscula = /[a-z]/.test(senha);
    const temNumero    = /[0-9]/.test(senha);
    const temEspecial  = /[@#$%^&+=!\-_.*]/.test(senha);
    const criterios    = [temMaiuscula, temMinuscula, temNumero, temEspecial].filter(Boolean).length;
    const temSequencia = /012|123|234|345|456|567|678|789|890|abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz|aaa|bbb|ccc|ddd|111|222|333|444|555|666|777|888|999/i.test(senha);

    if (senha.length >= 16 && criterios === 4)                      return 'muitoForte';
    if (senha.length >= 12 && criterios === 4 && !temSequencia)     return 'forte';
    if (senha.length >= 8  && criterios >= 3)                       return 'media';
    return 'fraca';
}

function initForcaSenha(campoId, containerId) {
    const campo     = document.getElementById(campoId);
    const container = document.getElementById(containerId);
    if (!campo || !container) return;

    const segs  = container.querySelectorAll('.forca-seg');
    const label = container.querySelector('.forca-label');

    const cfg = {
        fraca:      { ativo: 1, cor: '#c62828', texto: 'Senha fraca'       },
        media:      { ativo: 2, cor: '#f57c00', texto: 'Senha média'       },
        forte:      { ativo: 3, cor: '#7cb342', texto: 'Senha forte'       },
        muitoForte: { ativo: 4, cor: '#2e7d32', texto: 'Senha muito forte' }
    };

    campo.addEventListener('input', () => {
        const forca = avaliarForcaSenha(campo.value);
        if (!forca || !campo.value) { container.style.display = 'none'; return; }
        container.style.display = 'block';
        const c = cfg[forca];
        segs.forEach((seg, i) => { seg.style.background = i < c.ativo ? c.cor : '#e0e0e0'; });
        if (label) { label.textContent = c.texto; label.style.color = c.cor; }
    });
}

/* ============================================================
   GERADOR DE SENHA FORTE
   ============================================================ */

function gerarSenhaForte() {
    // Sem caracteres ambíguos: 0, O, l, 1, I
    const maiusculas = 'ABCDEFGHJKMNPQRSTUVWXYZ';
    const minusculas = 'abcdefghjkmnpqrstuvwxyz';
    const numeros    = '23456789';
    const especiais  = '@#$%&+=!*-_';
    const todos      = maiusculas + minusculas + numeros + especiais;

    // Garante pelo menos 2 de cada categoria
    const chars = [
        maiusculas[Math.floor(Math.random() * maiusculas.length)],
        maiusculas[Math.floor(Math.random() * maiusculas.length)],
        minusculas[Math.floor(Math.random() * minusculas.length)],
        minusculas[Math.floor(Math.random() * minusculas.length)],
        numeros[Math.floor(Math.random() * numeros.length)],
        numeros[Math.floor(Math.random() * numeros.length)],
        especiais[Math.floor(Math.random() * especiais.length)],
        especiais[Math.floor(Math.random() * especiais.length)]
    ];
    for (let i = chars.length; i < 16; i++) {
        chars.push(todos[Math.floor(Math.random() * todos.length)]);
    }
    // Fisher-Yates shuffle
    for (let i = chars.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [chars[i], chars[j]] = [chars[j], chars[i]];
    }
    return chars.join('');
}

function gerarEPreencher(senhaId, confirmarId, btnCopiarId) {
    const nova           = gerarSenhaForte();
    const campoSenha     = document.getElementById(senhaId);
    const campoConfirmar = document.getElementById(confirmarId);
    const btnCopiar      = document.getElementById(btnCopiarId);

    if (campoSenha) {
        campoSenha.value = nova;
        campoSenha.type  = 'text';
        campoSenha.dispatchEvent(new Event('input', { bubbles: true }));
    }
    if (campoConfirmar) {
        campoConfirmar.value = nova;
        campoConfirmar.dispatchEvent(new Event('input', { bubbles: true }));
    }
    if (btnCopiar) {
        btnCopiar.dataset.senha = nova;
        const wrap = btnCopiar.closest ? btnCopiar.closest('.copiar-container') : btnCopiar.parentElement;
        if (wrap) wrap.style.display = 'block';
    }
}

function copiarSenha(btnCopiarId) {
    const btn = document.getElementById(btnCopiarId);
    if (!btn) return;
    const senha = btn.dataset.senha;
    if (!senha) return;
    const marcarCopiado = () => {
        const orig = btn.textContent;
        btn.textContent     = 'Copiado ✓';
        btn.style.background = '#2e7d32';
        btn.style.color      = 'white';
        setTimeout(() => {
            btn.textContent     = orig;
            btn.style.background = '';
            btn.style.color      = '';
        }, 2000);
    };
    if (navigator.clipboard) {
        navigator.clipboard.writeText(senha).then(marcarCopiado).catch(() => {
            copiarFallback(senha); marcarCopiado();
        });
    } else { copiarFallback(senha); marcarCopiado(); }
}

function copiarFallback(texto) {
    const el = document.createElement('textarea');
    el.value = texto;
    el.style.position = 'fixed';
    el.style.opacity  = '0';
    document.body.appendChild(el);
    el.select();
    document.execCommand('copy');
    document.body.removeChild(el);
}

/* ============================================================
   CONFIRMAÇÃO DE SENHA EM TEMPO REAL — CADASTRO
   ============================================================ */

function initConfirmarSenha() {
    const campo = document.getElementById('confirmPassword');
    const senha = document.getElementById('senha');
    const msg   = document.getElementById('msg-confirmar-cadastro');
    const btn   = document.getElementById('btnCadastrar');
    if (!campo || !senha || !msg || !btn) return;

    btn.disabled = true;

    function podeSubmeter() {
        const forca    = avaliarForcaSenha(senha.value);
        const forcaOk  = forca !== null && forca !== 'fraca';
        const coincide = campo.value.length > 0 && campo.value === senha.value;
        btn.disabled = !(forcaOk && coincide);
    }

    function validar() {
        if (!campo.value) {
            msg.textContent = '';
            campo.style.borderColor = '';
            podeSubmeter();
            return;
        }
        if (campo.value === senha.value) {
            msg.textContent = '✓ Senhas coincidem';
            msg.style.color = '#2e7d32';
            campo.style.borderColor = '#2e7d32';
        } else {
            msg.textContent = '✗ Senhas não coincidem';
            msg.style.color = '#c62828';
            campo.style.borderColor = '#c62828';
        }
        podeSubmeter();
    }

    campo.addEventListener('input', validar);
    senha.addEventListener('input', () => { if (campo.value) validar(); else podeSubmeter(); });
}

/* ============================================================
   VALIDAÇÃO DE REDEFINIÇÃO DE SENHA — RESET VIA TOKEN
   ============================================================ */

function initResetSenha() {
    const senha     = document.getElementById('novaSenha');
    const confirmar = document.getElementById('confirmarSenha');
    const msg       = document.getElementById('msg-confirmar-reset');
    const btn       = document.getElementById('btn-redefinir');
    if (!senha || !confirmar || !msg || !btn) return;

    btn.disabled = true;

    function podeSubmeter() {
        const forca    = avaliarForcaSenha(senha.value);
        const forcaOk  = forca !== null && forca !== 'fraca';
        const coincide = confirmar.value.length > 0 && confirmar.value === senha.value;
        btn.disabled = !(forcaOk && coincide);
    }

    function validar() {
        if (!confirmar.value) {
            msg.textContent = '';
            confirmar.style.borderColor = '';
            podeSubmeter();
            return;
        }
        if (confirmar.value === senha.value) {
            msg.textContent = '✓ Senhas coincidem';
            msg.style.color = '#2e7d32';
            confirmar.style.borderColor = '#2e7d32';
        } else {
            msg.textContent = '✗ Senhas não coincidem';
            msg.style.color = '#c62828';
            confirmar.style.borderColor = '#c62828';
        }
        podeSubmeter();
    }

    confirmar.addEventListener('input', validar);
    senha.addEventListener('input', () => { if (confirmar.value) validar(); else podeSubmeter(); });
}

/* ============================================================
   VALIDAÇÃO DE ALTERAÇÃO DE SENHA — PERFIL
   ============================================================ */

function initAlterarSenha() {
    const senhaAtual     = document.getElementById('senhaAtual');
    const novaSenha      = document.getElementById('novaSenha');
    const confirmarSenha = document.getElementById('confirmarSenha');
    const msgConfirmar   = document.getElementById('msg-confirmar-perfil');
    const msgIgualAtual  = document.getElementById('msg-nova-igual-atual');
    const btn            = document.getElementById('btnAlterarSenha');
    if (!senhaAtual || !novaSenha || !confirmarSenha || !msgConfirmar || !btn) return;

    btn.disabled = true;

    function atualizarBtn() {
        const forca          = avaliarForcaSenha(novaSenha.value);
        const forcaOk        = forca !== null && forca !== 'fraca';
        const confirmacaoOk  = confirmarSenha.value.length > 0 && confirmarSenha.value === novaSenha.value;
        const novaIgualAtual = novaSenha.value.length > 0 && senhaAtual.value.length > 0 && novaSenha.value === senhaAtual.value;
        btn.disabled = !(forcaOk && confirmacaoOk) || novaIgualAtual;
    }

    function validarConfirmacao() {
        if (!confirmarSenha.value) {
            msgConfirmar.textContent = '';
            confirmarSenha.style.borderColor = '';
        } else if (confirmarSenha.value === novaSenha.value) {
            msgConfirmar.textContent = '✓ Senhas coincidem';
            msgConfirmar.style.color = '#2e7d32';
            confirmarSenha.style.borderColor = '#2e7d32';
        } else {
            msgConfirmar.textContent = '✗ Senhas não coincidem';
            msgConfirmar.style.color = '#c62828';
            confirmarSenha.style.borderColor = '#c62828';
        }
        atualizarBtn();
    }

    function validarNovaIgualAtual() {
        if (msgIgualAtual) {
            const igual = novaSenha.value.length > 0 && senhaAtual.value.length > 0 && novaSenha.value === senhaAtual.value;
            msgIgualAtual.style.display = igual ? 'block' : 'none';
        }
        atualizarBtn();
    }

    confirmarSenha.addEventListener('input', validarConfirmacao);
    novaSenha.addEventListener('input', () => {
        if (confirmarSenha.value) validarConfirmacao();
        else atualizarBtn();
        validarNovaIgualAtual();
    });
    senhaAtual.addEventListener('input', () => {
        if (novaSenha.value.length > 0) validarNovaIgualAtual();
    });
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
