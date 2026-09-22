package CompraSegura.usuario;

import java.util.Locale;
import jakarta.validation.constraints.*;

public class PerfilForm {
    @NotBlank(message = "Informe seu nome.")
    @Size(min = 2, max = 100, message = "O nome deve ter de 2 a 100 caracteres.")
    private String nome;
    @NotBlank(message = "Informe seu e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 254, message = "O e-mail deve ter até 254 caracteres.")
    private String email;
    @Size(max = 128, message = "A senha deve ter até 128 caracteres.")
    private String senhaAtual;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome == null ? null : nome.strip(); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.strip().toLowerCase(Locale.ROOT); }
    public String getSenhaAtual() { return senhaAtual; }
    public void setSenhaAtual(String senhaAtual) { this.senhaAtual = senhaAtual; }
}
