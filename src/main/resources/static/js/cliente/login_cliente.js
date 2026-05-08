(function () {
    var paleta = [
        "#4A5D23", "#5a7029", "#3d4e1d", "#6b8535", "#2C241B",
        "#722F37", "#8a3a44", "#c8c0b0", "#d4cec5", "#e8e3da",
        "#b5a890", "#9aab6a", "#7a9050", "#ddd8ce", "#c0bab0",
    ];
    var container = document.getElementById("bgBooks");
    if (!container) return;
    var n = Math.ceil(window.innerWidth / 8);
    var html = "";
    for (var i = 0; i < n; i++) {
        var h = 40 + Math.random() * 55;
        var w = 5 + Math.random() * 10;
        var c = paleta[Math.floor(Math.random() * paleta.length)];
        html += '<div class="bg-book" style="width:' + w + "px;height:" + h +
            "px;background:" + c + '"></div>';
    }
    container.innerHTML = html;
})();

(function () {
    var SVG_ABERTO = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>';
    var SVG_FECHADO = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';

    window.toggleSenhaLogin = function (inputId, btn) {
        var input = document.getElementById(inputId);
        if (!input) return;
        var mostrar = input.type === 'password';
        input.type = mostrar ? 'text' : 'password';
        btn.innerHTML = mostrar ? SVG_FECHADO : SVG_ABERTO;
        btn.setAttribute('aria-label', mostrar ? 'Ocultar senha' : 'Mostrar senha');
    };
})();
