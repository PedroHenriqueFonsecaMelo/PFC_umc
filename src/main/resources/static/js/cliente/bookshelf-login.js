/* ════════════════════════════════════════════════════════════
   bookshelf-login.js — Estante INTERATIVA · Login
   20 livros · hover sobe 10px · clique mostra tooltip
   ════════════════════════════════════════════════════════════ */
(function () {

    var TODOS_LIVROS = [
        {titulo:'Dom Casmurro',            autor:'Machado de Assis',        cor:'#722f37', trecho:'Olhai para mim e dizei se a Capitu da praia da Glória é a mesma do morro de Santa Teresa.'},
        {titulo:'O Alquimista',            autor:'Paulo Coelho',             cor:'#4a5d23', trecho:'Quando você quer algo, todo o universo conspira para que você realize o seu desejo.'},
        {titulo:'1984',                    autor:'George Orwell',            cor:'#2c3e6b', trecho:'Era um dia frio e luminoso de abril, e os relógios davam treze horas.'},
        {titulo:'O Pequeno Príncipe',      autor:'Saint-Exupéry',            cor:'#6b4a2c', trecho:'O essencial é invisível aos olhos. Só se vê bem com o coração.'},
        {titulo:'Crime e Castigo',         autor:'Dostoiévski',              cor:'#5a3e6b', trecho:'A dor e o sofrimento são inevitáveis para uma consciência larga.'},
        {titulo:'Sapiens',                 autor:'Yuval Harari',             cor:'#8b6914', trecho:'A história não tolera o vácuo. Se você não preencher o espaço, outros o farão.'},
        {titulo:'Orgulho e Preconceito',   autor:'Jane Austen',              cor:'#3e5a8b', trecho:'É verdade universalmente reconhecida que um homem solteiro de boa fortuna necessita de uma esposa.'},
        {titulo:'O Hobbit',                autor:'J.R.R. Tolkien',           cor:'#2c6b4a', trecho:'Em um buraco no chão vivia um hobbit. Não era um buraco sujo e desagradável.'},
        {titulo:'Cem Anos de Solidão',     autor:'García Márquez',           cor:'#6b2c5a', trecho:'Muitos anos depois, diante do pelotão de fuzilamento, o coronel Aureliano Buendía havia de recordar aquela tarde remota.'},
        {titulo:'A Metamorfose',           autor:'Franz Kafka',              cor:'#722f37', trecho:'Quando Gregor Samsa acordou de sonhos intranquilos, encontrou-se transformado em monstruoso inseto.'},
        {titulo:'Dom Quixote',             autor:'Cervantes',                cor:'#4a6b2c', trecho:'Em algum lugar da Mancha, cujo nome não me ocorre, vivia não há muito um fidalgo.'},
        {titulo:'O Senhor dos Anéis',      autor:'J.R.R. Tolkien',           cor:'#2c4a6b', trecho:'Um anel para a todos governar, um anel para encontrá-los.'},
        {titulo:'Moby Dick',               autor:'Herman Melville',          cor:'#2c6b6b', trecho:'Chamai-me Ismael. Há alguns anos, não importa há quantos exatamente...'},
        {titulo:'Anna Karenina',           autor:'Tolstói',                  cor:'#6b5a2c', trecho:'Todas as famílias felizes se parecem; cada família infeliz é infeliz à sua maneira.'},
        {titulo:'Os Miseráveis',           autor:'Victor Hugo',              cor:'#3e2c6b', trecho:'Amar é agir. Amar é fazer o bem. Amar é ser útil.'},
        {titulo:'Hamlet',                  autor:'Shakespeare',              cor:'#2c2c6b', trecho:'Ser ou não ser, eis a questão.'},
        {titulo:'Frankenstein',            autor:'Mary Shelley',             cor:'#4a2c6b', trecho:'Nada é mais doloroso do que, após sentir que é amado, descobrir que foi enganado.'},
        {titulo:'A Divina Comédia',        autor:'Dante Alighieri',          cor:'#5a2c6b', trecho:'No meio do caminho de nossa vida, me vi numa selva escura.'},
        {titulo:'Guerra e Paz',            autor:'Tolstói',                  cor:'#6b2c2c', trecho:'A força mais poderosa é aquela de um homem que descobre por que está lutando.'},
        {titulo:'Madame Bovary',           autor:'Flaubert',                 cor:'#2c5a3e', trecho:'Ela pensava às vezes que eram os dias mais belos de sua vida.'},
        {titulo:'Jane Eyre',               autor:'Charlotte Brontë',         cor:'#3e6b5a', trecho:'Sou pobre, obscura e pequena; mas quando se trata de alma, sou igual a você.'},
        {titulo:'O Processo',              autor:'Franz Kafka',              cor:'#6b6b2c', trecho:'Alguém devia ter caluniado Josef K., pois uma manhã ele foi detido sem ter feito mal algum.'},
        {titulo:'O Nome da Rosa',          autor:'Umberto Eco',              cor:'#5a4a2c', trecho:'Os livros não foram feitos para ser acreditados, mas para serem submetidos à indagação.'},
        {titulo:'Admirável Mundo Novo',    autor:'Aldous Huxley',            cor:'#2c6b6b', trecho:'Todos pertencem a todos os outros. Não há razão para sofrimento algum.'},
        {titulo:'O Mestre e Margarida',    autor:'Bulgákov',                 cor:'#6b2c6b', trecho:'Covardia é o maior pecado de todos.'},
        {titulo:'Memórias Póstumas',       autor:'Machado de Assis',         cor:'#4a3e2c', trecho:'Ao verme que primeiro roeu as frias carnes do meu cadáver dedico estas Memórias Póstumas.'},
        {titulo:'Grande Sertão Veredas',   autor:'Guimarães Rosa',           cor:'#5a6b2c', trecho:'O diabo na rua, no meio do redemunho.'},
        {titulo:'Iracema',                 autor:'José de Alencar',          cor:'#2c4a5a', trecho:'Verdes mares bravios de minha terra natal, onde canta a jandaia nas frondes da carnaúba.'},
        {titulo:'O Cortiço',               autor:'Aluísio Azevedo',          cor:'#6b4a4a', trecho:'João Romão era um dos mais notáveis negociantes que enriqueceram no Brasil.'},
        {titulo:'Vidas Secas',             autor:'Graciliano Ramos',         cor:'#8b7a3e', trecho:'Na planície avermelhada os juazeiros alargavam duas manchas verdes.'}
    ];

    var ALTURAS = [125,135,140,130,145,128,138,132,142,127,136,133,141,129,137,131,143,126,139,134];

    function shuffle(arr) {
        var a = arr.slice();
        for (var i = a.length - 1; i > 0; i--) {
            var j = Math.floor(Math.random() * (i + 1));
            var tmp = a[i]; a[i] = a[j]; a[j] = tmp;
        }
        return a;
    }

    function esc(s) {
        return String(s)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    var selecionados = shuffle(TODOS_LIVROS).slice(0, 28);
    var gap = 4;
    var totalGaps = (selecionados.length - 1) * gap;
    var bookWidth = Math.floor((window.innerWidth - totalGaps) / 28);

    var container = document.getElementById('bookshelfContainer');
    var row = document.getElementById('booksRow');
    if (!container || !row) return;

    /* Habilita pointer-events na fileira */
    row.style.pointerEvents = 'auto';

    /* Hint */
    var hint = document.createElement('div');
    hint.className = 'bs-hint';
    hint.textContent = '✦ Clique nos livros para descobrir trechos';
    container.appendChild(hint);

    /* Tooltip */
    var tooltip = document.getElementById('bs-tooltip');
    if (!tooltip) {
        tooltip = document.createElement('div');
        tooltip.id = 'bs-tooltip';
        document.body.appendChild(tooltip);
    }

    /* Renderiza livros */
    selecionados.forEach(function (livro, i) {
        var el = document.createElement('div');
        el.className = 'book-item interactive';
        el.style.width  = bookWidth + 'px';
        el.style.height = ALTURAS[i % ALTURAS.length] + 'px';
        el.style.backgroundColor = livro.cor;

        var spine = document.createElement('div');
        spine.className = 'book-spine';
        el.appendChild(spine);

        var titulo = document.createElement('div');
        titulo.className = 'book-title-text';
        titulo.textContent = livro.titulo;
        el.appendChild(titulo);

        el.addEventListener('click', function (e) {
            e.stopPropagation();
            abrirTooltip(el, livro);
        });

        row.appendChild(el);
    });

    /* ── Tooltip ── */
    var livrAtivo = null;
    var hintGone  = false;

    function abrirTooltip(el, livro) {
        /* Fecha anterior */
        if (livrAtivo && livrAtivo !== el) {
            livrAtivo.classList.remove('active');
        }

        /* Remove hint na primeira interação */
        if (!hintGone) {
            hintGone = true;
            hint.classList.add('out');
            setTimeout(function () { hint.parentNode && hint.parentNode.removeChild(hint); }, 400);
        }

        if (livrAtivo === el) {
            fecharTooltip();
            return;
        }

        livrAtivo = el;
        el.classList.add('active');

        tooltip.innerHTML =
            '<div class="bs-tt-titulo">' + esc(livro.titulo) + '</div>' +
            '<div class="bs-tt-trecho">' + esc(livro.trecho) + '</div>' +
            '<div class="bs-tt-autor">— ' + esc(livro.autor) + '</div>';

        tooltip.style.display = 'block';
        tooltip.classList.remove('visible');

        /* Posiciona */
        var r  = el.getBoundingClientRect();
        var tw = tooltip.offsetWidth;
        var th = tooltip.offsetHeight;
        var left = Math.max(8, Math.min(r.left + r.width / 2 - tw / 2, window.innerWidth - tw - 8));
        var top  = r.top - th - 12;
        if (top < 8) top = r.bottom + 8;

        tooltip.style.left = left + 'px';
        tooltip.style.top  = top  + 'px';

        /* Anima */
        requestAnimationFrame(function () {
            tooltip.classList.add('visible');
        });
    }

    function fecharTooltip() {
        if (livrAtivo) {
            livrAtivo.classList.remove('active');
            livrAtivo = null;
        }
        tooltip.classList.remove('visible');
        setTimeout(function () {
            if (!tooltip.classList.contains('visible')) {
                tooltip.style.display = 'none';
            }
        }, 200);
    }

    document.addEventListener('click', function () { fecharTooltip(); });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') fecharTooltip(); });

}());
