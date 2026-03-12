document.addEventListener('DOMContentLoaded', () => {
    carregarSaldo();
    carregarLivros();
});

async function carregarSaldo() {
    try {
        const res = await fetch('/clientes/meu-perfil-json');
        if (res.ok) {
            const cliente = await res.json();
            document.getElementById('saldoUsuario').innerHTML = 
                `<i class="fa-solid fa-coins mr-1 text-yellow-400"></i> T$ ${cliente.saldoTokens.toFixed(2)}`;
        }
    } catch (err) { console.error("Erro ao carregar saldo", err); }
}

async function carregarLivros() {
    const grid = document.getElementById('gridLivros');
    
    try {
        const res = await fetch('/api/livros/todos');
        const livros = await res.json();

        if (livros.length === 0) {
            grid.innerHTML = `<p class="col-span-full text-center text-gray-400 py-10">Nenhum livro anunciado ainda.</p>`;
            return;
        }

        grid.innerHTML = livros.map(livro => `
            <div class="bg-white rounded-2xl shadow-sm hover:shadow-xl transition-all border border-gray-100 overflow-hidden group">
                <div class="relative h-56 bg-gray-200">
                    <img src="${livro.fotoUrl}" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300">
                    <span class="absolute top-3 right-3 bg-white/90 backdrop-blur px-3 py-1 rounded-full text-xs font-bold text-indigo-600 shadow-sm">
                        ${livro.estadoAprovado || livro.estado}
                    </span>
                </div>
                <div class="p-5">
                    <h3 class="font-bold text-gray-800 text-lg truncate">${livro.titulo}</h3>
                    <p class="text-sm text-gray-500 mb-4">por ${livro.autor}</p>
                    
                    <div class="flex items-center justify-between mt-auto">
                        <div class="text-indigo-600 font-extrabold text-xl">
                            T$ ${livro.precoAprovado.toFixed(2)}
                        </div>
                        <button onclick="comprarLivro(${livro.id}, ${livro.precoAprovado})" 
                                class="bg-gray-900 text-white px-4 py-2 rounded-xl hover:bg-indigo-600 transition-colors text-sm font-bold">
                            Comprar
                        </button>
                    </div>
                    <p class="text-[10px] text-gray-400 mt-3"><i class="fa-solid fa-user-tag mr-1"></i> Vendedor: ${livro.vendedor.nome}</p>
                </div>
            </div>
        `).join('');
    } catch (err) {
        grid.innerHTML = `<p class="col-span-full text-center text-red-400">Erro ao carregar vitrine.</p>`;
    }
}

async function comprarLivro(id, preco) {
    if (!confirm(`Confirmar a compra por T$ ${preco}?`)) return;

    try {
        const res = await fetch(`/api/livros/${id}/comprar`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}` // Se estiver usando JWT no Header
            }
        });

        if (res.ok) {
            alert("Compra realizada com sucesso!");
            location.reload(); // Atualiza a vitrine e o saldo
        } else {
            const erro = await res.text();
            alert("Falha na compra: " + erro);
        }
    } catch (err) {
        alert("Erro de conexão.");
    }
}
