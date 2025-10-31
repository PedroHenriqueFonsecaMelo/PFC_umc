package umc.exs.model.DTO;

public class SignupDTO {
    private String email;
    private String senha; // usar campo em português conforme controllers do projeto
    private String password; // compatibilidade se algum controller esperar 'password'
    private String nome;
    private String datanasc;
    private String gen;
    private Boolean termsAccepted;
    private Boolean privacyAccepted;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha != null ? senha : password; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDatanasc() { return datanasc; }
    public void setDatanasc(String datanasc) { this.datanasc = datanasc; }

    public String getGen() { return gen; }
    public void setGen(String gen) { this.gen = gen; }

    public Boolean getTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(Boolean termsAccepted) { this.termsAccepted = termsAccepted; }

    public Boolean getPrivacyAccepted() { return privacyAccepted; }
    public void setPrivacyAccepted(Boolean privacyAccepted) { this.privacyAccepted = privacyAccepted; }
}