/* ================================================================
   alerta-endereco.js — Toast de endereço não cadastrado
   Incluído em: vitrine, vender, livro_detalhe, meu-perfil
   Verifica /clientes/meu-perfil-json; se sem endereço mostra
   toast persistente no canto inferior direito (por sessão/tela).
   ================================================================ */
(function () {

    var PERFIL_URL  = '/clientes/meu-perfil-json';
    var DESTINO_URL = '/clientes/meu-perfil?tab=enderecos';
    /* Chave única por página — fechar em vitrine não oculta em vender */
    var SS_KEY = 'bib_ae_' + window.location.pathname.replace(/[^a-z0-9]/gi, '_');

    /* ── Estilos injetados uma vez ── */
    function injetarEstilos() {
        if (document.getElementById('ae-style')) return;
        var s = document.createElement('style');
        s.id  = 'ae-style';
        s.textContent = [
            '#ae-toast{',
            '  position:fixed;bottom:1.5rem;right:1.5rem;z-index:9999;',
            '  width:300px;max-width:calc(100vw - 2rem);',
            '  background:#fff;border-left:3px solid #722f37;border-radius:8px;',
            '  box-shadow:0 4px 20px rgba(44,36,27,.16),0 1px 4px rgba(44,36,27,.08);',
            '  padding:14px 14px 14px 14px;',
            '  display:flex;gap:10px;align-items:flex-start;',
            '  animation:aeIn .3s ease;',
            '}',
            '@keyframes aeIn{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}',
            '#ae-toast.ae-out{animation:aeOut .25s ease forwards}',
            '@keyframes aeOut{to{opacity:0;transform:translateY(12px)}}',
            '#ae-toast .ae-icon{font-size:1.25rem;line-height:1;flex-shrink:0;margin-top:1px}',
            '#ae-toast .ae-corpo{flex:1;min-width:0}',
            '#ae-toast .ae-texto{',
            '  font-family:"DM Sans",sans-serif;font-size:.83rem;',
            '  color:#2c241b;line-height:1.45;margin-bottom:6px;',
            '}',
            '#ae-toast .ae-link{',
            '  font-family:"DM Sans",sans-serif;font-size:.82rem;',
            '  font-weight:600;color:#722f37;text-decoration:none;',
            '}',
            '#ae-toast .ae-link:hover{text-decoration:underline}',
            '#ae-toast .ae-fechar{',
            '  background:none;border:none;cursor:pointer;',
            '  color:#9a8a80;font-size:.95rem;line-height:1;',
            '  padding:0;flex-shrink:0;margin-top:1px;',
            '}',
            '#ae-toast .ae-fechar:hover{color:#2c241b}'
        ].join('');
        document.head.appendChild(s);
    }

    /* ── Monta e exibe o toast ── */
    function mostrarToast() {
        if (document.getElementById('ae-toast')) return;
        injetarEstilos();

        var div = document.createElement('div');
        div.id = 'ae-toast';
        div.innerHTML =
            '<span class="ae-icon">📍</span>' +
            '<div class="ae-corpo">' +
              '<div class="ae-texto">Cadastre seu endereço de entrega para comprar livros</div>' +
              '<a class="ae-link" href="' + DESTINO_URL + '">Cadastrar agora →</a>' +
            '</div>' +
            '<button class="ae-fechar" aria-label="Fechar" onclick="window.__aeFechar()">✕</button>';
        document.body.appendChild(div);
    }

    /* ── Fecha e persiste estado na sessão ── */
    window.__aeFechar = function () {
        var el = document.getElementById('ae-toast');
        if (!el) return;
        el.classList.add('ae-out');
        setTimeout(function () { el && el.parentNode && el.parentNode.removeChild(el); }, 270);
        try { sessionStorage.setItem(SS_KEY, '1'); } catch (_) {}
    };

    /* ── Verificação principal ── */
    function verificar() {
        try { if (sessionStorage.getItem(SS_KEY)) return; } catch (_) {}

        fetch(PERFIL_URL, { credentials: 'include' })
            .then(function (r) { return r.ok ? r.json() : null; })
            .then(function (c) {
                if (!c) return;                           /* não logado → silencioso */
                var addrs = c.enderecos || [];
                if (addrs.length === 0) mostrarToast();
            })
            .catch(function () {});                       /* falha de rede → silencioso */
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', verificar);
    } else {
        verificar();
    }

}());
