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
