/* ════════════════════════════════════════════════════════════
   CITAÇÕES ROTATIVAS — capa do livro (fade a cada 8 s)
   ════════════════════════════════════════════════════════════ */
(function () {
    var QUOTES = [
        {
            text:
                "Um leitor vive mil vidas antes de morrer. Quem não lê, vive apenas uma.",
            author: "George R.R. Martin",
        },
        {
            text:
                "Os livros são espelhos: só vemos neles o que já temos dentro de nós.",
            author: "Carlos Ruiz Zafón",
        },
        {
            text: "Não existe amigo mais fiel do que um livro.",
            author: "Ernest Hemingway",
        },
        { text: "Ler é viver duas vezes.", author: "Umberto Eco" },
        {
            text: "Uma história bem contada pode mudar o mundo.",
            author: "Paulo Coelho",
        },
        {
            text:
                "Os livros são o avião, o trem e a estrada. São o destino e a jornada.",
            author: "Anna Quindlen",
        },
        {
            text: "Leia muito. Leia tudo o que você puder.",
            author: "William Faulkner",
        },
        {
            text:
                "Palavras são, na minha humilde opinião, nossa mais inexaurível fonte de magia.",
            author: "J.K. Rowling",
        },
        {
            text:
                "Não existe lugar melhor para ir quando você precisa sonhar do que a página de um livro.",
            author: "Judith Krantz",
        },
        {
            text: "A leitura é para a mente o que o exercício é para o corpo.",
            author: "Joseph Addison",
        },
        {
            text: "Escreva o livro que você gostaria de ler.",
            author: "Toni Morrison",
        },
        {
            text: "Só existe uma maneira de ler um livro: com todo o ser.",
            author: "Doris Lessing",
        },
        {
            text: "Em todo livro há um sonho esperando para ser vivido.",
            author: "Neil Gaiman",
        },
        {
            text:
                "Os livros que o mundo chama de imorais são aqueles que lhe mostram sua própria vergonha.",
            author: "Oscar Wilde",
        },
        {
            text:
                "Ao ler um livro, você está sonhando com a mente de outra pessoa.",
            author: "Jorge Luis Borges",
        },
        {
            text: "Nunca confie em alguém que não tem livros em casa.",
            author: "Lemony Snicket",
        },
        {
            text:
                "Se você tiver um jardim e uma biblioteca, terá tudo de que precisa.",
            author: "Cícero",
        },
        {
            text:
                "As histórias que nos tocam profundamente são aquelas que revelam a nossa humanidade.",
            author: "Chimamanda Ngozi Adichie",
        },
        {
            text:
                "Cada livro que você lê transforma você em uma pessoa diferente.",
            author: "Haruki Murakami",
        },
        {
            text:
                "A leitura de bons livros é uma conversa com os melhores espíritos dos séculos passados.",
            author: "René Descartes",
        },
        {
            text:
                "Livros são a civilização. Sem livros, a história é silenciosa.",
            author: "Barbara Tuchman",
        },
        {
            text:
                "Ler é a coisa mais maravilhosa que um ser humano pode fazer.",
            author: "Roald Dahl",
        },
        { text: "O mundo pertence àqueles que leem.", author: "Rick Holland" },
        {
            text:
                "Escrever é desenhar o mapa de uma viagem que nunca foi feita.",
            author: "José Saramago",
        },
        {
            text:
                "Os livros têm o mesmo inimigo que as pessoas: a falta de tempo.",
            author: "Paul Valéry",
        },
        {
            text: "Uma casa sem livros é como um corpo sem alma.",
            author: "Cícero",
        },
        {
            text: "A imaginação é mais importante que o conhecimento.",
            author: "Albert Einstein",
        },
        {
            text:
                "Toda história é sobre uma pessoa tentando encontrar o seu lugar no mundo.",
            author: "Terry Pratchett",
        },
        {
            text: "Livros não são feitos para enfeitar; são para serem lidos.",
            author: "Thomas Jefferson",
        },
        {
            text:
                "Em algum lugar algo incrível está esperando para ser descoberto.",
            author: "Carl Sagan",
        },
        {
            text:
                "Comece pelo começo — e continue até chegar ao fim. Então pare.",
            author: "Lewis Carroll",
        },
        {
            text:
                "A educação é a arma mais poderosa que você pode usar para mudar o mundo.",
            author: "Nelson Mandela",
        },
        {
            text: "Não basta aprender a ler — é preciso amar a leitura.",
            author: "Voltaire",
        },
        {
            text:
                "Um bom livro é aquele que abre com expectativa e fecha com lucro.",
            author: "Amos Bronson Alcott",
        },
        {
            text: "A palavra escrita é a mais longa duração do ser humano.",
            author: "Clarice Lispector",
        },
        {
            text:
                "A leitura nos dá um lugar para ir quando temos que ficar onde estamos.",
            author: "Mason Cooley",
        },
        { text: "Um bom livro é sempre atual.", author: "Somerset Maugham" },
        {
            text: "Os livros são os únicos objetos de que nunca nos cansamos.",
            author: "Virginia Woolf",
        },
        { text: "Você se torna o que você lê.", author: "Abraham Lincoln" },
        {
            text:
                "A literatura é a arte de descobrir algo extraordinário sobre pessoas comuns.",
            author: "Boris Pasternak",
        },
        {
            text: "As histórias nunca terminam — apenas mudam de forma.",
            author: "Terry Pratchett",
        },
        {
            text:
                "Um clássico é um livro que nunca terminou de dizer o que tem a dizer.",
            author: "Ítalo Calvino",
        },
        {
            text: "A função da arte é fazer que o invisível se torne visível.",
            author: "Paul Klee",
        },
        {
            text:
                "Os livros são os guardiões do passado e os professores do futuro.",
            author: "Francis Bacon",
        },
        {
            text: "Quem lê, viaja sem sair do lugar.",
            author: "Monteiro Lobato",
        },
        {
            text: "Todo livro é uma viagem, e o leitor é o explorador.",
            author: "Henry David Thoreau",
        },
        { text: "A palavra é o homem mesmo.", author: "Guimarães Rosa" },
        {
            text: "Nenhuma hora dedicada à leitura é hora perdida.",
            author: "Chateaubriand",
        },
        {
            text: "É nos livros que encontramos a humanidade reunida.",
            author: "Denis Diderot",
        },
        {
            text: "A literatura é o amor tornado linguagem.",
            author: "Rubem Braga",
        },
        {
            text: "Quando leio um bom livro, sinto que nunca estou sozinho.",
            author: "C.S. Lewis",
        },
        {
            text: "Todo grande escritor foi primeiro um grande leitor.",
            author: "Ralph Waldo Emerson",
        },
        {
            text: "Aquele que lê muito e anda muito, vê muito e sabe muito.",
            author: "Miguel de Cervantes",
        },
        {
            text: "Ler é o remédio mais barato e eficaz contra a ignorância.",
            author: "Carl Sagan",
        },
        { text: "Um livro é uma versão do mundo.", author: "Salman Rushdie" },
        { text: "Ler é cultivar o jardim da mente.", author: "Umberto Eco" },
        {
            text: "Cada livro que lemos planta uma semente de curiosidade.",
            author: "Isaac Asimov",
        },
        {
            text: "A escrita é uma maneira de falar sem ser interrompido.",
            author: "Jules Renard",
        },
        {
            text: "A melhor maneira de prever o futuro é inventá-lo.",
            author: "Alan Kay",
        },
        {
            text: "O conhecimento é a única coisa que cresce quando dividido.",
            author: "Francis Bacon",
        },
        {
            text: "Um leitor hoje é um líder amanhã.",
            author: "Harry S. Truman",
        },
        {
            text:
                "A boa literatura é insubstituível na formação de seres humanos completos.",
            author: "Mario Vargas Llosa",
        },
        {
            text:
                "As histórias que mais nos tocam são as que já conhecemos no coração.",
            author: "C.S. Lewis",
        },
        {
            text: "Todo livro é uma conversa entre o escritor e o leitor.",
            author: "Ítalo Calvino",
        },
        {
            text: "Quem tem medo de um livro tem medo da vida.",
            author: "Ray Bradbury",
        },
        {
            text: "A leitura abre portas que o tempo fecha.",
            author: "Fernando Sabino",
        },
        {
            text: "O que fazemos por amor está além do bem e do mal.",
            author: "Friedrich Nietzsche",
        },
        {
            text:
                "Uma boa história é aquela que nos faz sentir menos sozinhos.",
            author: "David Foster Wallace",
        },
        {
            text: "Os livros são janelas para mundos que ainda não existem.",
            author: "Brandon Sanderson",
        },
        {
            text:
                "O livro é o único objeto inanimado que pode nos conceder a imortalidade.",
            author: "Gabriel García Márquez",
        },
        {
            text: "Livros são barcos que navegam pelos mares do tempo.",
            author: "Francis Bacon",
        },
        {
            text:
                "A palavra escrita une os corações separados pelo tempo e pela distância.",
            author: "Jorge Luis Borges",
        },
        {
            text:
                "Cada vez que lemos um clássico, é como se o lêssemos pela primeira vez.",
            author: "Ítalo Calvino",
        },
        {
            text: "Enquanto houver livros, haverá esperança para a humanidade.",
            author: "H.G. Wells",
        },
        {
            text:
                "Não há nada mais profundo que uma página escrita com sinceridade.",
            author: "Fiódor Dostoiévski",
        },
        {
            text:
                "O homem que não lê não tem vantagem sobre o homem que não sabe ler.",
            author: "Mark Twain",
        },
        {
            text: "Uma boa história não termina — ela ressoa.",
            author: "F. Scott Fitzgerald",
        },
        {
            text: "Numa boa livraria, você pode encontrar o mundo inteiro.",
            author: "Graham Greene",
        },
        {
            text: "Ler é sair de si mesmo para entrar nos outros.",
            author: "Marguerite Yourcenar",
        },
        {
            text: "Os livros são os melhores companheiros de viagem.",
            author: "Charles Lamb",
        },
        {
            text: "Ler é o mais nobre dos prazeres solitários.",
            author: "Henry David Thoreau",
        },
        {
            text: "Uma página por dia é um livro por ano.",
            author: "Provérbio dos leitores",
        },
        {
            text: "O maior presente que se pode dar a alguém é um livro.",
            author: "Malala Yousafzai",
        },
        {
            text: "A ficção revela verdades que a realidade obscurece.",
            author: "Jessamyn West",
        },
        {
            text: "Alguns livros devem ser provados, outros devorados.",
            author: "Francis Bacon",
        },
        { text: "A leitura é a respiração da alma.", author: "George Sand" },
        {
            text: "Cada livro novo é uma nova aventura para o espírito.",
            author: "Voltaire",
        },
        { text: "Só quem lê sabe o que não sabe.", author: "Gabriel Perissé" },
        {
            text:
                "Toda vez que leio um livro, parte dele se torna parte de mim.",
            author: "Cassandra Clare",
        },
        {
            text: "Um livro é como um jardim que você pode carregar no bolso.",
            author: "Provérbio chinês",
        },
        {
            text: "Ler é o passaporte para incontáveis aventuras.",
            author: "Mary Pope Osborne",
        },
        {
            text:
                "Amo os livros pelas mesmas razões que amo a chuva: renovam o mundo.",
            author: "Jorge Luis Borges",
        },
        {
            text: "Não existe livro tão ruim que não ensine alguma coisa boa.",
            author: "Plínio, o Jovem",
        },
        {
            text: "Quem lê não precisa de asas para voar.",
            author: "José Saramago",
        },
        {
            text: "O silêncio entre as páginas é onde vivem os sonhos.",
            author: "Gaston Bachelard",
        },
        {
            text:
                "Livros são portas que nunca se fecham para quem quer aprender.",
            author: "Paulo Freire",
        },
        {
            text:
                "Na leitura habita a liberdade que nenhum tirano pode roubar.",
            author: "Federico García Lorca",
        },
        {
            text: "Um livro mal lido é um livro não lido.",
            author: "Ezra Pound",
        },
        { text: "A leitura é um ato revolucionário.", author: "Paulo Freire" },
        {
            text:
                "O escritor escreve um livro para que o leitor escreva o seu.",
            author: "Umberto Eco",
        },
    ];

    var quoteContainer = document.getElementById("cover-quote");
    var quoteEl = document.getElementById("quote-texto");
    var authorEl = document.getElementById("quote-autor");
    if (!quoteContainer || !quoteEl || !authorEl) return;

    // Garante opacidade inicial visível
    quoteContainer.style.opacity = "1";

    // Começa em citação aleatória diferente do placeholder do HTML
    var idx = Math.floor(Math.random() * QUOTES.length);

    function aplicarCitacao(q) {
        quoteEl.textContent = "\u201c" + q.text + "\u201d";
        authorEl.textContent = "\u2014 " + q.author;
    }

    // Aplica imediatamente
    aplicarCitacao(QUOTES[idx]);

    // Rotaciona a cada 8 s com fade
    setInterval(function () {
        quoteContainer.style.opacity = "0";
        setTimeout(function () {
            var next;
            do {
                next = Math.floor(Math.random() * QUOTES.length);
            } while (next === idx && QUOTES.length > 1);
            idx = next;
            aplicarCitacao(QUOTES[idx]);
            quoteContainer.style.opacity = "1";
        }, 650);
    }, 8000);
})();

/* ════════════════════════════════════════════════════════════
   LIVROS DE FUNDO (gerados dinamicamente)
   ════════════════════════════════════════════════════════════ */
(function () {
    var paleta = [
        "#4A5D23",
        "#5a7029",
        "#3d4e1d",
        "#6b8535",
        "#2C241B",
        "#722F37",
        "#8a3a44",
        "#c8c0b0",
        "#d4cec5",
        "#e8e3da",
        "#b5a890",
        "#9aab6a",
        "#7a9050",
        "#ddd8ce",
        "#c0bab0",
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
    var SVG_ABERTO =
        '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>';
    var SVG_FECHADO =
        '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';

    window.toggleSenhaLogin = function (inputId, btn) {
        var input = document.getElementById(inputId);
        if (!input) return;
        var mostrar = input.type === "password";
        input.type = mostrar ? "text" : "password";
        btn.innerHTML = mostrar ? SVG_FECHADO : SVG_ABERTO;
        btn.setAttribute(
            "aria-label",
            mostrar ? "Ocultar senha" : "Mostrar senha",
        );
    };
})();
