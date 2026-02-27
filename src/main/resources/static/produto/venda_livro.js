const { useState } = React;

function App() {
    const [formData, setFormData] = useState({
        titulo: '',
        autor: '',
        isbn: '',
        precoTokens: '',
        estado: 'NOVO'
    });
    const [foto, setFoto] = useState(null);
    const [preview, setPreview] = useState(null);
    const [loading, setLoading] = useState(false);
    const [buscandoIsbn, setBuscandoIsbn] = useState(false);

    // Busca automática na API da Open Library ao digitar ISBN
    const buscarDadosLivro = async (valorIsbn) => {
        const isbnLimpo = valorIsbn.replace(/-/g, "").trim();
        // A busca só dispara se tiver um tamanho mínimo de ISBN (10 ou 13)
        if (isbnLimpo.length < 10) return;

        setBuscandoIsbn(true);
        try {
            const res = await fetch(`https://openlibrary.org/api/books?bibkeys=ISBN:${isbnLimpo}&format=json&jscmd=data`);
            const data = await res.json();
            const info = data[`ISBN:${isbnLimpo}`];

            if (info) {
                setFormData(prev => ({
                    ...prev,
                    titulo: info.title || prev.titulo,
                    autor: info.authors ? info.authors[0].name : prev.autor
                }));
            }
        } catch (error) {
            console.error("Erro ao buscar livro:", error);
        } finally {
            setBuscandoIsbn(false);
        }
    };

    const handleInputChange = (e) => {
        const { id, value } = e.target;
        setFormData(prev => ({ ...prev, [id]: value }));

        // Se o campo for ISBN, tenta buscar os dados
        if (id === 'isbn' && (value.length >= 10)) {
            buscarDadosLivro(value);
        }
    };

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setFoto(file);
            setPreview(URL.createObjectURL(file));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!foto) {
            alert("Por favor, anexe uma foto do livro.");
            return;
        }

        setLoading(true);
        const data = new FormData();
        
        // Monta o objeto exatamente como o seu LivroRequestDTO espera
        const dadosLivro = {
            titulo: formData.titulo,
            autor: formData.autor,
            isbn: formData.isbn,
            precoTokens: parseFloat(formData.precoTokens),
            estado: formData.estado
        };

        // O segredo para o Spring Boot aceitar o JSON junto com o arquivo:
        // Enviar o JSON como um Blob de tipo application/json
        data.append("dados", new Blob([JSON.stringify(dadosLivro)], { type: "application/json" }));
        data.append("foto", foto);

        try {
            const response = await fetch('/api/livros/vender', {
                method: 'POST',
                body: data
                // IMPORTANTE: Não defina headers de Content-Type aqui. 
                // O navegador fará isso automaticamente para o Multipart.
            });

            if (response.ok) {
                alert("Parabéns! Seu anúncio foi publicado com sucesso.");
                window.location.href = "/clientes/meu-perfil";
            } else {
                const erroTexto = await response.text();
                alert("Erro ao publicar anúncio: " + erroTexto);
            }
        } catch (error) {
            console.error("Erro na requisição:", error);
            alert("Erro de conexão com o servidor.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="bg-white p-8 rounded-2xl shadow-xl">
            <div className="flex items-center gap-3 mb-8">
                <div className="bg-indigo-600 p-3 rounded-lg text-white">
                    <i className="fa-solid fa-book-open text-2xl"></i>
                </div>
                <div>
                    <h2 className="text-2xl font-bold text-gray-800">Anunciar Livro</h2>
                    <p className="text-gray-500">React + Busca Automática via ISBN</p>
                </div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="md:col-span-2">
                        <label className="block text-sm font-semibold text-gray-700 mb-1">ISBN</label>
                        <input type="text" id="isbn" value={formData.isbn} onChange={handleInputChange}
                            className="w-full border border-gray-300 p-3 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none"
                            placeholder="Digite o ISBN para buscar informações" />
                        {buscandoIsbn && <p className="text-xs text-indigo-500 animate-pulse mt-1">Consultando banco de dados de livros...</p>}
                    </div>

                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-1">Título</label>
                        <input type="text" id="titulo" value={formData.titulo} onChange={handleInputChange} required
                            className="w-full border border-gray-300 p-3 rounded-xl outline-none" />
                    </div>

                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-1">Autor</label>
                        <input type="text" id="autor" value={formData.autor} onChange={handleInputChange} required
                            className="w-full border border-gray-300 p-3 rounded-xl outline-none" />
                    </div>

                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-1">Preço (Tokens)</label>
                        <input type="number" id="precoTokens" value={formData.precoTokens} onChange={handleInputChange} required
                            className="w-full border border-gray-300 p-3 rounded-xl outline-none" />
                    </div>

                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-1">Estado</label>
                        <select id="estado" value={formData.estado} onChange={handleInputChange}
                            className="w-full border border-gray-300 p-3 rounded-xl bg-white outline-none cursor-pointer">
                            <option value="NOVO">Novo (Nunca usado)</option>
                            <option value="OTIMO">Ótimo (Sem marcas)</option>
                            <option value="BOM">Bom (Marcas leves)</option>
                            <option value="DESGASTADO">Desgastado (Com avarias)</option>
                        </select>
                    </div>
                </div>

                <div className="relative border-2 border-dashed border-gray-300 p-6 text-center rounded-2xl hover:border-indigo-500 transition cursor-pointer">
                    <input type="file" onChange={handleFileChange} accept="image/*" className="absolute inset-0 w-full opacity-0 cursor-pointer" />
                    {!preview ? (
                        <div>
                            <i className="fa-solid fa-cloud-arrow-up text-3xl text-indigo-500 mb-2"></i>
                            <p className="text-gray-600">Clique ou arraste a foto do livro aqui</p>
                        </div>
                    ) : (
                        <div className="relative inline-block">
                            <img src={preview} className="max-h-48 rounded-lg shadow-md" />
                            <p className="text-xs text-gray-400 mt-2 font-semibold">Clique para trocar a foto</p>
                        </div>
                    )}
                </div>

                <button type="submit" disabled={loading}
                    className="w-full bg-indigo-600 text-white py-4 rounded-xl font-bold text-lg hover:bg-indigo-700 transition flex items-center justify-center gap-2 shadow-lg shadow-indigo-100 disabled:bg-gray-400">
                    {loading ? (
                        <><i className="fa-solid fa-spinner animate-spin"></i> Processando...</>
                    ) : (
                        <><span>Publicar Anúncio</span> <i className="fa-solid fa-paper-plane"></i></>
                    )}
                </button>
            </form>
        </div>
    );
}

// Renderiza a aplicação na div #root do seu HTML
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);