/* ════════════════════════════════════════════════════════════
   bookshelf.js — Estante decorativa rodapé · Cadastro / Recuperar Senha
   18 livros diários · sem interação
   ════════════════════════════════════════════════════════════ */
(function () {
    var BOOKS = [
        { t: "Dom Casmurro", c: "#722f37" },
        { t: "O Alquimista", c: "#4a5d23" },
        { t: "1984", c: "#2c3e6b" },
        { t: "O Pequeno Príncipe", c: "#6b4a2c" },
        { t: "Crime e Castigo", c: "#5a3e6b" },
        { t: "Sapiens", c: "#8b6914" },
        { t: "Orgulho e Preconceito", c: "#3e5a8b" },
        { t: "O Hobbit", c: "#2c6b4a" },
        { t: "Cem Anos de Solidão", c: "#6b2c5a" },
        { t: "A Metamorfose", c: "#722f37" },
        { t: "Dom Quixote", c: "#4a6b2c" },
        { t: "O Senhor dos Anéis", c: "#2c4a6b" },
        { t: "A Divina Comédia", c: "#5a2c6b" },
        { t: "Moby Dick", c: "#2c6b6b" },
        { t: "Anna Karenina", c: "#6b5a2c" },
        { t: "Guerra e Paz", c: "#3e2c6b" },
        { t: "Os Miseráveis", c: "#2c4a2c" },
        { t: "Madame Bovary", c: "#6b2c2c" },
        { t: "Hamlet", c: "#2c2c6b" },
        { t: "Frankenstein", c: "#4a2c6b" },
        { t: "Lolita", c: "#8b3a3a" },
        { t: "O Nome da Rosa", c: "#6b4a2c" },
        { t: "Admirável Mundo Novo", c: "#3e5a3e" },
        { t: "O Mestre e Margarida", c: "#6b2c4a" },
        { t: "Ulisses", c: "#2c3e6b" },
        { t: "Jane Eyre", c: "#5a3e8b" },
        { t: "Drácula", c: "#4a2c2c" },
        { t: "O Processo", c: "#3e3e6b" },
        { t: "O Apanhador no Centeio", c: "#2c6b3e" },
        { t: "O Grande Gatsby", c: "#6b6b2c" },
        { t: "O Retrato de Dorian Gray", c: "#722f5a" },
        { t: "Memórias do Subsolo", c: "#5a3e3e" },
        { t: "Siddhartha", c: "#8b6b2c" },
        { t: "O Lobo da Estepe", c: "#5a5a2c" },
        { t: "Fahrenheit 451", c: "#8b3a2c" },
        { t: "A Revolução dos Bichos", c: "#4a6b4a" },
        { t: "O Velho e o Mar", c: "#2c5a6b" },
        { t: "O Estrangeiro", c: "#6b6b3e" },
        { t: "A Peste", c: "#5a3e2c" },
        { t: "O Conde de Monte Cristo", c: "#2c4a8b" },
        { t: "Macunaíma", c: "#8b6b14" },
        { t: "Vidas Secas", c: "#8b5a2c" },
        { t: "A Hora da Estrela", c: "#722f5a" },
        { t: "Ensaio sobre a Cegueira", c: "#3e3e3e" },
        { t: "Memórias Póstumas", c: "#5a2c2c" },
        { t: "Grande Sertão: Veredas", c: "#6b4a14" },
        { t: "O Cortiço", c: "#8b5a3e" },
        { t: "Iracema", c: "#4a8b3e" },
        { t: "São Bernardo", c: "#5a3e2c" },
        { t: "Quincas Borba", c: "#722f37" },
        { t: "Capitães da Areia", c: "#8b6b14" },
        { t: "Os Irmãos Karamazov", c: "#4a2c6b" },
        { t: "O Idiota", c: "#3e3e8b" },
        { t: "O Diário de Anne Frank", c: "#2c6b4a" },
        { t: "Fausto", c: "#6b5a14" },
        { t: "O Castelo", c: "#5a5a5a" },
        { t: "Germinal", c: "#5a4a14" },
        { t: "Wuthering Heights", c: "#5a3e5a" },
        { t: "Persuasão", c: "#3e5a6b" },
        { t: "O Amor nos Tempos do Cólera", c: "#8b4a2c" },
        { t: "Pedro Páramo", c: "#6b6b2c" },
        { t: "Rayuela", c: "#2c5a8b" },
        { t: "A Casa dos Espíritos", c: "#8b2c5a" },
        { t: "Ficções", c: "#2c3e5a" },
        { t: "O Senhor das Moscas", c: "#5a4a2c" },
        { t: "Matadouro Cinco", c: "#4a4a8b" },
        { t: "Alice no País das Maravilhas", c: "#5a3e8b" },
        { t: "O Mundo de Sofia", c: "#3e6b5a" },
        { t: "Perfume", c: "#5a6b2c" },
        { t: "A Sombra do Vento", c: "#8b5a3e" },
        { t: "Stoner", c: "#5a5a3e" },
        { t: "Romeu e Julieta", c: "#8b2c3e" },
        { t: "Macbeth", c: "#3e2c4a" },
        { t: "Os Três Mosqueteiros", c: "#4a2c8b" },
        { t: "Vinte Mil Léguas", c: "#2c5a6b" },
        { t: "Robinson Crusoé", c: "#6b5a2c" },
        { t: "O Príncipe", c: "#5a2c2c" },
        { t: "Noites Brancas", c: "#6b6b8b" },
        { t: "O Jogador", c: "#6b3e2c" },
        { t: "O Tempo e o Vento", c: "#2c4a2c" },
        { t: "Gabriela, Cravo e Canela", c: "#8b6b2c" },
        { t: "Angústia", c: "#5a3e4a" },
        { t: "Sagarana", c: "#6b5a14" },
        { t: "O Quinze", c: "#8b5a14" },
        { t: "Os Sofrimentos de Werther", c: "#4a6b2c" },
        { t: "O Nome do Vento", c: "#5a3e2c" },
        { t: "A Morte em Veneza", c: "#6b2c6b" },
        { t: "Eva Luna", c: "#722f5a" },
        { t: "Tristão e Isolda", c: "#6b2c3e" },
        { t: "O Mágico de Oz", c: "#4a8b6b" },
        { t: "Menino de Engenho", c: "#8b6b14" },
        { t: "Beloved", c: "#6b2c2c" },
        { t: "Os Lusíadas", c: "#2c4a8b" },
        { t: "Mensagem", c: "#3e2c5a" },
        { t: "O Livro do Desassossego", c: "#5a3e3e" },
    ];

    /* ── Gerador pseudoaleatório com seed diário ── */
    function hashStr(s) {
        var h = 0;
        for (var i = 0; i < s.length; i++) {
            h = (Math.imul(31, h) + s.charCodeAt(i)) |
                0;
        }
        return (h >>> 0);
    }
    var _seed = hashStr(new Date().toDateString());
    function rand() {
        _seed = (_seed * 1664525 + 1013904223) >>> 0;
        return _seed / 0xffffffff;
    }
    function randInt(a, b) {
        return a + Math.floor(rand() * (b - a + 1));
    }
    function shuffle(arr) {
        var a = arr.slice();
        for (var i = a.length - 1; i > 0; i--) {
            var j = Math.floor(rand() * (i + 1));
            var tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
        return a;
    }

    /* ── 18 livros do dia ── */
    var daily = shuffle(BOOKS).slice(0, 18);

    /* ── Container ── */
    var wrap = document.getElementById("bsWrap") ||
        document.querySelector(".bookshelf-wrap");
    if (!wrap) return;

    wrap.innerHTML = "";

    /* Fade */
    var fade = document.createElement("div");
    fade.className = "bs-fade";
    wrap.appendChild(fade);

    /* Fileira de livros */
    var row = document.createElement("div");
    row.className = "bs-books";
    daily.forEach(function (b) {
        var el = document.createElement("div");
        el.className = "bs-book";
        el.style.cssText = "width:" + randInt(36, 42) + "px;height:" +
            randInt(120, 140) + "px;background-color:" + b.c + ";";
        var span = document.createElement("span");
        span.className = "bs-title";
        span.textContent = b.t;
        el.appendChild(span);
        row.appendChild(el);
    });
    wrap.appendChild(row);

    /* Prateleira */
    var plank = document.createElement("div");
    plank.className = "bs-plank";
    wrap.appendChild(plank);
})();
