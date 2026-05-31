/* ════════════════════════════════════════════════════════════
   bookshelf-deco.js — Estante decorativa rodapé
   Cadastro / Recuperar Senha · sem interação
   ════════════════════════════════════════════════════════════ */
(function () {
    var TODOS_LIVROS = [
        { titulo: "Dom Casmurro", cor: "#722f37" },
        { titulo: "O Alquimista", cor: "#4a5d23" },
        { titulo: "1984", cor: "#2c3e6b" },
        { titulo: "O Pequeno Príncipe", cor: "#6b4a2c" },
        { titulo: "Crime e Castigo", cor: "#5a3e6b" },
        { titulo: "Sapiens", cor: "#8b6914" },
        { titulo: "Orgulho e Preconceito", cor: "#3e5a8b" },
        { titulo: "O Hobbit", cor: "#2c6b4a" },
        { titulo: "Cem Anos de Solidão", cor: "#6b2c5a" },
        { titulo: "A Metamorfose", cor: "#722f37" },
        { titulo: "Dom Quixote", cor: "#4a6b2c" },
        { titulo: "O Senhor dos Anéis", cor: "#2c4a6b" },
        { titulo: "Moby Dick", cor: "#2c6b6b" },
        { titulo: "Anna Karenina", cor: "#6b5a2c" },
        { titulo: "Os Miseráveis", cor: "#3e2c6b" },
        { titulo: "Hamlet", cor: "#2c2c6b" },
        { titulo: "Frankenstein", cor: "#4a2c6b" },
        { titulo: "A Divina Comédia", cor: "#5a2c6b" },
        { titulo: "Guerra e Paz", cor: "#6b2c2c" },
        { titulo: "Madame Bovary", cor: "#2c5a3e" },
        { titulo: "Jane Eyre", cor: "#3e6b5a" },
        { titulo: "O Processo", cor: "#6b6b2c" },
        { titulo: "O Nome da Rosa", cor: "#5a4a2c" },
        { titulo: "Admirável Mundo Novo", cor: "#2c6b6b" },
        { titulo: "O Mestre e Margarida", cor: "#6b2c6b" },
        { titulo: "Memórias Póstumas", cor: "#4a3e2c" },
        { titulo: "Grande Sertão Veredas", cor: "#5a6b2c" },
        { titulo: "Iracema", cor: "#2c4a5a" },
        { titulo: "O Cortiço", cor: "#6b4a4a" },
        { titulo: "Vidas Secas", cor: "#8b7a3e" },
    ];

    var ALTURAS = [
        125,
        135,
        140,
        130,
        145,
        128,
        138,
        132,
        142,
        127,
        136,
        133,
        141,
        129,
        137,
        131,
        143,
        126,
        139,
        134,
    ];

    function shuffle(arr) {
        var a = arr.slice();
        for (var i = a.length - 1; i > 0; i--) {
            var j = Math.floor(Math.random() * (i + 1));
            var tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
        return a;
    }

    var selecionados = shuffle(TODOS_LIVROS).slice(0, 28);
    var gap = 4;
    var totalGaps = (selecionados.length - 1) * gap;
    var bookWidth = Math.floor((window.innerWidth - totalGaps) / 28);

    var container = document.getElementById("bookshelfContainer");
    var row = document.getElementById("booksRow");
    if (!container || !row) return;

    selecionados.forEach(function (livro, i) {
        var el = document.createElement("div");
        el.className = "book-item";
        el.style.width = bookWidth + "px";
        el.style.height = ALTURAS[i % ALTURAS.length] + "px";
        el.style.backgroundColor = livro.cor;

        var spine = document.createElement("div");
        spine.className = "book-spine";
        el.appendChild(spine);

        var titulo = document.createElement("div");
        titulo.className = "book-title-text";
        titulo.textContent = livro.titulo;
        el.appendChild(titulo);

        row.appendChild(el);
    });
})();
