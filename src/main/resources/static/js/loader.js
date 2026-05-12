/* ══════════════════════════════════════════════════════════════════
   Bibliotroca — Page Loader
   API pública: showLoader() / hideLoader()
   Intercepta automaticamente: fetch() e navegação de página
   ══════════════════════════════════════════════════════════════════ */

(function () {
    'use strict';

    /* ════════════════════════════════════════════════════════════
       100 CITAÇÕES DE AUTORES FAMOSOS
       ════════════════════════════════════════════════════════════ */
    var QUOTES = [
        { text: "Um leitor vive mil vidas antes de morrer. Quem não lê, vive apenas uma.", author: "George R.R. Martin" },
        { text: "Os livros são espelhos: só vemos neles o que já temos dentro de nós.", author: "Carlos Ruiz Zafón" },
        { text: "Não existe amigo mais fiel do que um livro.", author: "Ernest Hemingway" },
        { text: "Ler é viver duas vezes.", author: "Umberto Eco" },
        { text: "Uma história bem contada pode mudar o mundo.", author: "Paulo Coelho" },
        { text: "Os livros são o avião, o trem e a estrada. São o destino e a jornada.", author: "Anna Quindlen" },
        { text: "Leia muito. Leia tudo o que você puder.", author: "William Faulkner" },
        { text: "Palavras são, na minha humilde opinião, nossa mais inexaurível fonte de magia.", author: "J.K. Rowling" },
        { text: "Não existe lugar melhor para ir quando você precisa sonhar do que a página de um livro.", author: "Judith Krantz" },
        { text: "A leitura é para a mente o que o exercício é para o corpo.", author: "Joseph Addison" },
        { text: "Escreva o livro que você gostaria de ler.", author: "Toni Morrison" },
        { text: "Só existe uma maneira de ler um livro: com todo o ser.", author: "Doris Lessing" },
        { text: "Em todo livro há um sonho esperando para ser vivido.", author: "Neil Gaiman" },
        { text: "Os livros que o mundo chama de imorais são aqueles que lhe mostram sua própria vergonha.", author: "Oscar Wilde" },
        { text: "Ao ler um livro, você está sonhando com a mente de outra pessoa.", author: "Jorge Luis Borges" },
        { text: "Nunca confie em alguém que não tem livros em casa.", author: "Lemony Snicket" },
        { text: "Se você tiver um jardim e uma biblioteca, terá tudo de que precisa.", author: "Cícero" },
        { text: "As histórias que nos tocam profundamente são aquelas que revelam a nossa humanidade.", author: "Chimamanda Ngozi Adichie" },
        { text: "Cada livro que você lê transforma você em uma pessoa diferente.", author: "Haruki Murakami" },
        { text: "A leitura de bons livros é uma conversa com os melhores espíritos dos séculos passados.", author: "René Descartes" },
        { text: "Livros são a civilização. Sem livros, a história é silenciosa.", author: "Barbara Tuchman" },
        { text: "Ler é a coisa mais maravilhosa que um ser humano pode fazer.", author: "Roald Dahl" },
        { text: "O mundo pertence àqueles que leem.", author: "Rick Holland" },
        { text: "Escrever é desenhar o mapa de uma viagem que nunca foi feita.", author: "José Saramago" },
        { text: "Os livros têm o mesmo inimigo que as pessoas: a falta de tempo.", author: "Paul Valéry" },
        { text: "Uma casa sem livros é como um corpo sem alma.", author: "Cícero" },
        { text: "A imaginação é mais importante que o conhecimento.", author: "Albert Einstein" },
        { text: "Toda história é sobre uma pessoa tentando encontrar o seu lugar no mundo.", author: "Terry Pratchett" },
        { text: "Livros não são feitos para enfeitar; são para serem lidos.", author: "Thomas Jefferson" },
        { text: "Em algum lugar algo incrível está esperando para ser descoberto.", author: "Carl Sagan" },
        { text: "Comece pelo começo — e continue até chegar ao fim. Então pare.", author: "Lewis Carroll" },
        { text: "A educação é a arma mais poderosa que você pode usar para mudar o mundo.", author: "Nelson Mandela" },
        { text: "Não basta aprender a ler — é preciso amar a leitura.", author: "Voltaire" },
        { text: "Um bom livro é aquele que abre com expectativa e fecha com lucro.", author: "Amos Bronson Alcott" },
        { text: "A palavra escrita é a mais longa duração do ser humano.", author: "Clarice Lispector" },
        { text: "A leitura nos dá um lugar para ir quando temos que ficar onde estamos.", author: "Mason Cooley" },
        { text: "Um bom livro é sempre atual.", author: "Somerset Maugham" },
        { text: "Os livros são os únicos objetos de que nunca nos cansamos.", author: "Virginia Woolf" },
        { text: "Você se torna o que você lê.", author: "Abraham Lincoln" },
        { text: "A literatura é a arte de descobrir algo extraordinário sobre pessoas comuns.", author: "Boris Pasternak" },
        { text: "As histórias nunca terminam — apenas mudam de forma.", author: "Terry Pratchett" },
        { text: "Um clássico é um livro que nunca terminou de dizer o que tem a dizer.", author: "Ítalo Calvino" },
        { text: "A função da arte é fazer que o invisível se torne visível.", author: "Paul Klee" },
        { text: "Os livros são os guardiões do passado e os professores do futuro.", author: "Francis Bacon" },
        { text: "Quem lê, viaja sem sair do lugar.", author: "Monteiro Lobato" },
        { text: "Todo livro é uma viagem, e o leitor é o explorador.", author: "Henry David Thoreau" },
        { text: "A palavra é o homem mesmo.", author: "Guimarães Rosa" },
        { text: "Nenhuma hora dedicada à leitura é hora perdida.", author: "Chateaubriand" },
        { text: "É nos livros que encontramos a humanidade reunida.", author: "Denis Diderot" },
        { text: "A literatura é o amor tornado linguagem.", author: "Rubem Braga" },
        { text: "Quando leio um bom livro, sinto que nunca estou sozinho.", author: "C.S. Lewis" },
        { text: "Todo grande escritor foi primeiro um grande leitor.", author: "Ralph Waldo Emerson" },
        { text: "Aquele que lê muito e anda muito, vê muito e sabe muito.", author: "Miguel de Cervantes" },
        { text: "Ler é o remédio mais barato e eficaz contra a ignorância.", author: "Carl Sagan" },
        { text: "Um livro é uma versão do mundo.", author: "Salman Rushdie" },
        { text: "Ler é cultivar o jardim da mente.", author: "Umberto Eco" },
        { text: "Cada livro que lemos planta uma semente de curiosidade.", author: "Isaac Asimov" },
        { text: "A escrita é uma maneira de falar sem ser interrompido.", author: "Jules Renard" },
        { text: "A melhor maneira de prever o futuro é inventá-lo.", author: "Alan Kay" },
        { text: "O conhecimento é a única coisa que cresce quando dividido.", author: "Francis Bacon" },
        { text: "Um leitor hoje é um líder amanhã.", author: "Harry S. Truman" },
        { text: "A boa literatura é insubstituível na formação de seres humanos completos.", author: "Mario Vargas Llosa" },
        { text: "As histórias que mais nos tocam são as que já conhecemos no coração.", author: "C.S. Lewis" },
        { text: "Todo livro é uma conversa entre o escritor e o leitor.", author: "Ítalo Calvino" },
        { text: "Quem tem medo de um livro tem medo da vida.", author: "Ray Bradbury" },
        { text: "A leitura abre portas que o tempo fecha.", author: "Fernando Sabino" },
        { text: "O que fazemos por amor está além do bem e do mal.", author: "Friedrich Nietzsche" },
        { text: "Uma boa história é aquela que nos faz sentir menos sozinhos.", author: "David Foster Wallace" },
        { text: "Os livros são janelas para mundos que ainda não existem.", author: "Brandon Sanderson" },
        { text: "O livro é o único objeto inanimado que pode nos conceder a imortalidade.", author: "Gabriel García Márquez" },
        { text: "Livros são barcos que navegam pelos mares do tempo.", author: "Francis Bacon" },
        { text: "A palavra escrita une os corações separados pelo tempo e pela distância.", author: "Jorge Luis Borges" },
        { text: "Cada vez que lemos um clássico, é como se o lêssemos pela primeira vez.", author: "Ítalo Calvino" },
        { text: "Enquanto houver livros, haverá esperança para a humanidade.", author: "H.G. Wells" },
        { text: "Não há nada mais profundo que uma página escrita com sinceridade.", author: "Fiódor Dostoiévski" },
        { text: "O homem que não lê não tem vantagem sobre o homem que não sabe ler.", author: "Mark Twain" },
        { text: "Uma boa história não termina — ela ressoa.", author: "F. Scott Fitzgerald" },
        { text: "Numa boa livraria, você pode encontrar o mundo inteiro.", author: "Graham Greene" },
        { text: "Ler é sair de si mesmo para entrar nos outros.", author: "Marguerite Yourcenar" },
        { text: "Os livros são os melhores companheiros de viagem.", author: "Charles Lamb" },
        { text: "Ler é o mais nobre dos prazeres solitários.", author: "Henry David Thoreau" },
        { text: "Uma página por dia é um livro por ano.", author: "proverbio dos leitores" },
        { text: "O maior presente que se pode dar a alguém é um livro.", author: "Malala Yousafzai" },
        { text: "A ficção revela verdades que a realidade obscurece.", author: "Jessamyn West" },
        { text: "Alguns livros devem ser provados, outros devorados.", author: "Francis Bacon" },
        { text: "A leitura é a respiração da alma.", author: "George Sand" },
        { text: "Cada livro novo é uma nova aventura para o espírito.", author: "Voltaire" },
        { text: "Só quem lê sabe o que não sabe.", author: "Gabriel Perissé" },
        { text: "Toda vez que leio um livro, parte dele se torna parte de mim.", author: "Cassandra Clare" },
        { text: "Um livro é como um jardim que você pode carregar no bolso.", author: "proverbio chinês" },
        { text: "Ler é o passaporte para incontáveis aventuras.", author: "Mary Pope Osborne" },
        { text: "Amo os livros pelas mesmas razões que amo a chuva: renovam o mundo.", author: "Jorge Luis Borges" },
        { text: "Não existe livro tão ruim que não ensine alguma coisa boa.", author: "Plínio, o Jovem" },
        { text: "Quem lê não precisa de asas para voar.", author: "José Saramago" },
        { text: "O silêncio entre as páginas é onde vivem os sonhos.", author: "Gaston Bachelard" },
        { text: "Livros são portas que nunca se fecham para quem quer aprender.", author: "Paulo Freire" },
        { text: "Na leitura habita a liberdade que nenhum tirano pode roubar.", author: "Federico García Lorca" },
        { text: "Um livro mal lido é um livro não lido.", author: "Ezra Pound" },
        { text: "A leitura é um ato revolucionário.", author: "Paulo Freire" },
        { text: "O escritor escreve um livro para que o leitor escreva o seu.", author: "Umberto Eco" }
    ];

    /* ════════════════════════════════════════════════════════════
       ESTADO INTERNO
       ════════════════════════════════════════════════════════════ */
    var _count      = 0;     // número de operações em andamento
    var _quoteTimer = null;
    var _quoteIdx   = 0;

    /* ════════════════════════════════════════════════════════════
       API PÚBLICA: showLoader / hideLoader
       ════════════════════════════════════════════════════════════ */
    function showLoader() {
        _count++;
        if (_count !== 1) return;   // já estava visível
        var overlay = document.getElementById('bib-loader-overlay');
        if (!overlay) return;
        overlay.style.display = 'flex';
        overlay.offsetHeight;       // força reflow para ativar a transition
        overlay.style.opacity = '1';
        _startQuotes();
    }

    function hideLoader() {
        _count = Math.max(0, _count - 1);
        if (_count !== 0) return;   // ainda há operações em andamento
        var overlay = document.getElementById('bib-loader-overlay');
        if (!overlay) return;
        overlay.style.opacity = '0';
        _stopQuotes();
        setTimeout(function () {
            if (_count === 0) overlay.style.display = 'none';
        }, 260);
    }

    /* ════════════════════════════════════════════════════════════
       CITAÇÕES — rotação a cada 7,5 s com fade suave
       ════════════════════════════════════════════════════════════ */
    function _startQuotes() {
        _quoteIdx = Math.floor(Math.random() * QUOTES.length);
        _renderQuote(true);
        if (_quoteTimer) clearInterval(_quoteTimer);
        _quoteTimer = setInterval(_rotateQuote, 7500);
    }

    function _stopQuotes() {
        if (_quoteTimer) { clearInterval(_quoteTimer); _quoteTimer = null; }
    }

    function _renderQuote(instant) {
        var q    = QUOTES[_quoteIdx];
        var qEl  = document.getElementById('bibLoaderQuote');
        var aEl  = document.getElementById('bibLoaderAuthor');
        if (!qEl) return;
        if (instant) { qEl.style.transition = 'none'; if (aEl) aEl.style.transition = 'none'; }
        qEl.textContent = '\u201c' + q.text + '\u201d';
        if (aEl) aEl.textContent = '\u2014 ' + q.author;
        if (instant) {
            qEl.offsetHeight;   // reflow
            qEl.style.transition = '';
            if (aEl) aEl.style.transition = '';
        }
    }

    function _rotateQuote() {
        var qEl = document.getElementById('bibLoaderQuote');
        var aEl = document.getElementById('bibLoaderAuthor');
        if (!qEl) return;
        // fade out
        qEl.style.opacity = '0';
        if (aEl) aEl.style.opacity = '0';
        setTimeout(function () {
            _quoteIdx = (_quoteIdx + 1) % QUOTES.length;
            _renderQuote(false);
            // fade in
            qEl.style.opacity = '1';
            if (aEl) aEl.style.opacity = '1';
        }, 460);
    }

    /* ════════════════════════════════════════════════════════════
       INTERCEPTOR GLOBAL DE fetch()
       Exibe o loader para todas as chamadas relevantes;
       ignora polling de badge e chamadas externas.
       ════════════════════════════════════════════════════════════ */
    var SKIP_PATTERNS = [
        '/api/notificacoes/nao-lidas',
        '/api/notificacoes/marcar-lidas',
        '/api/notificacoes',
        '/clientes/meu-perfil-json',
        '/api/admin/me',
        '/api/admin/cancelamentos/pendentes/count',
        'googleapis.com',
        'google.com',
        'via.placeholder.com',
        'fonts.g'   // Google Fonts
    ];

    var _origFetch = window.fetch;
    window.fetch = function () {
        var url = '';
        try {
            if (typeof arguments[0] === 'string') {
                url = arguments[0];
            } else if (arguments[0] && arguments[0].url) {
                url = arguments[0].url;
            }
        } catch (_e) {}

        var skip = SKIP_PATTERNS.some(function (p) { return url.indexOf(p) !== -1; });
        if (!skip) showLoader();

        return _origFetch.apply(this, arguments).finally(function () {
            if (!skip) hideLoader();
        });
    };

    /* ════════════════════════════════════════════════════════════
       NAVEGAÇÃO DE PÁGINA (links e submits nativos)
       ════════════════════════════════════════════════════════════ */
    window.addEventListener('beforeunload', function () {
        showLoader();
    });

    /* Esconde o loader se a página vier do bfcache (botão voltar) */
    window.addEventListener('pageshow', function (e) {
        if (e.persisted) {
            _count = 0;
            var overlay = document.getElementById('bib-loader-overlay');
            if (overlay) { overlay.style.opacity = '0'; overlay.style.display = 'none'; }
            _stopQuotes();
        }
    });

    /* ════════════════════════════════════════════════════════════
       EXPÕE A API GLOBALMENTE
       ════════════════════════════════════════════════════════════ */
    window.showLoader = showLoader;
    window.hideLoader = hideLoader;

}());
