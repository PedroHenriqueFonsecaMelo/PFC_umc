// Função para buscar CEP usando a API ViaCEP
async function buscarCep(index) {
    const cep = document.getElementById(`cep-${index}`).value.replace(/\D/g, '');

    if (cep.length !== 8) return;

    try {
        const response = await fetch(`https://viacep.com.br/ws/${cep}/json/`);
        const data = await response.json();

        if (!data.erro) {
            document.getElementById(`rua-${index}`).value = data.logradouro;
            document.getElementById(`bairro-${index}`).value = data.bairro;
            document.getElementById(`cidade-${index}`).value = data.localidade;
            document.getElementById(`estado-${index}`).value = data.uf;
            document.getElementById(`pais-${index}`).value = "Brasil";
            // Foca no número após preencher
            document.getElementById(`numero-${index}`).focus();
        } else {
            alert("CEP não encontrado.");
        }
    } catch (error) {
        console.error("Erro ao buscar CEP:", error);
    }
}

// Função para adicionar novo bloco de endereço
function adicionarEndereco() {
    const container = document.getElementById('enderecos-list-container');
    const index = enderecoIndex; // Usa a variável global iniciada no HTML

    const novoEnderecoHtml = `
        <div class="endereco-item item-card novo-endereco animate-fade-in">
            <fieldset>
                <legend>Novo Endereço</legend>
                <div class="form-grid">
                    <div class="form-group">
                        <label>CEP:</label>
                        <input type="text" id="cep-${index}" name="enderecos[${index}].cep" 
                               onblur="buscarCep(${index})" placeholder="00000-000" required />
                    </div>
                    <div class="form-group">
                        <label>Rua:</label>
                        <input type="text" id="rua-${index}" name="enderecos[${index}].rua" required />
                    </div>
                    <div class="form-group">
                        <label>Número:</label>
                        <input type="text" name="enderecos[${index}].numero" id="numero-${index}" required />
                    </div>
                    <div class="form-group">
                        <label>Bairro:</label>
                        <input type="text" id="bairro-${index}" name="enderecos[${index}].bairro" required />
                    </div>
                    <div class="form-group">
                        <label>Cidade:</label>
                        <input type="text" id="cidade-${index}" name="enderecos[${index}].cidade" required />
                    </div>
                    <div class="form-group">
                        <label>Estado:</label>
                        <input type="text" id="estado-${index}" name="enderecos[${index}].estado" maxlength="2" required />
                    </div>
                    <div class="form-group">
                        <label>País:</label>
                        <input type="text" id="pais-${index}" name="enderecos[${index}].pais" value="Brasil" required />
                    </div>
                    <div class="form-group">
                        <label>Tipo de residência:</label>
                        <input type="text" name="enderecos[${index}].tipoResidencia" placeholder="Ex: Casa, Apto" />
                    </div>
                </div>
                <button type="button" class="btn-remover-js" onclick="this.closest('.endereco-item').remove()">
                    ❌ Cancelar Novo Endereço
                </button>
            </fieldset>
        </div>
    `;

    container.insertAdjacentHTML('beforeend', novoEnderecoHtml);
    enderecoIndex++; // Incrementa para o próximo endereço
}

// Função para adicionar novo cartão (mantendo a lógica do seu formulário)
function adicionarCartao() {
    const container = document.getElementById('cartoes-list-container');
    const index = cartaoIndex;

    const novoCartaoHtml = `
        <div class="cartao-item item-card novo-cartao">
            <fieldset>
                <legend>Novo Cartão</legend>
                <div class="form-grid">
                    <div class="form-group">
                        <label>Número do Cartão:</label>
                        <input type="text" name="cartoes[${index}].numero" required />
                    </div>
                    <div class="form-group">
                        <label>Nome no Cartão:</label>
                        <input type="text" name="cartoes[${index}].nomeTitular" required />
                    </div>
                    <div class="form-group">
                        <label>Validade:</label>
                        <input type="text" name="cartoes[${index}].validade" placeholder="MM/AAAA" required />
                    </div>
                </div>
                <button type="button" class="btn-remover-js" onclick="this.closest('.cartao-item').remove()">
                    ❌ Cancelar Novo Cartão
                </button>
            </fieldset>
        </div>
    `;

    container.insertAdjacentHTML('beforeend', novoCartaoHtml);
    cartaoIndex++;
}