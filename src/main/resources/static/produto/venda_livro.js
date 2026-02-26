document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('formVenda');
    const inputFoto = document.getElementById('foto');
    const dropzone = document.getElementById('dropzone');
    const preview = document.getElementById('preview');
    const uploadPlaceholder = document.getElementById('uploadPlaceholder');
    const btnRemove = document.getElementById('btnRemove');

    // Acionar input de arquivo ao clicar na dropzone
    dropzone.onclick = (e) => {
        if (e.target.id !== 'btnRemove' && !e.target.closest('#btnRemove')) {
            inputFoto.click();
        }
    };

    // Preview da Imagem
    inputFoto.onchange = () => {
        const file = inputFoto.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (e) => {
                preview.src = e.target.result;
                preview.classList.remove('hidden');
                uploadPlaceholder.classList.add('hidden');
                btnRemove.classList.remove('hidden');
            };
            reader.readAsDataURL(file);
        }
    };

    // Remover Foto
    btnRemove.onclick = (e) => {
        e.stopPropagation();
        inputFoto.value = "";
        preview.src = "";
        preview.classList.add('hidden');
        uploadPlaceholder.classList.remove('hidden');
        btnRemove.classList.add('hidden');
    };

    // Submissão do Formulário
    form.onsubmit = async (e) => {
        e.preventDefault();

        if (!inputFoto.files[0]) {
            alert("Por favor, envie uma foto do livro.");
            return;
        }

        const btnSubmit = document.getElementById('btnSubmit');
        btnSubmit.disabled = true;
        btnSubmit.innerHTML = `<i class="fa-solid fa-spinner animate-spin"></i> Processando...`;

        const formData = new FormData();

        // Construindo o objeto de dados (JSON) conforme o LivroRequestDTO
        const dadosLivro = {
            titulo: document.getElementById('titulo').value,
            autor: document.getElementById('autor').value,
            isbn: document.getElementById('isbn').value,
            precoTokens: parseFloat(document.getElementById('precoTokens').value),
            estado: document.getElementById('estado').value
        };

        // Adiciona o JSON como um Blob para que o Spring identifique como application/json
        formData.append("dados", new Blob([JSON.stringify(dadosLivro)], {
            type: "application/json"
        }));

        // Adiciona o arquivo da foto
        formData.append("foto", inputFoto.files[0]);

        try {
            const response = await fetch('/api/livros/vender', {
                method: 'POST',
                body: formData // Nota: O navegador define o boundary do Multipart automaticamente
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
            btnSubmit.disabled = false;
            btnSubmit.innerHTML = `<span>Publicar Anúncio</span> <i class="fa-solid fa-paper-plane"></i>`;
        }
    };
});