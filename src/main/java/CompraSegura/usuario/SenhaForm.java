package CompraSegura.usuario;

import jakarta.validation.constraints.*;

public class SenhaForm {
    @NotBlank(message = "Informe a senha atual.")
    @Size(max = 128, message = "A senha deve ter até 128 caracteres.")
    private String senhaAtual;
    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 12, max = 128, message = "A nova senha deve ter de 12 a 128 caracteres.")
    private String novaSenha;
    @NotBlank(message = "Confirme a nova senha.")
    @Size(max = 128, message = "A confirmação deve ter até 128 caracteres.")
    private String confirmacaoSenha;

    public String getSenhaAtual() { return senhaAtual; }
    public void setSenhaAtual(String valor) { senhaAtual = valor; }
    public String getNovaSenha() { return novaSenha; }
    public void setNovaSenha(String valor) { novaSenha = valor; }
    public String getConfirmacaoSenha() { return confirmacaoSenha; }
    public void setConfirmacaoSenha(String valor) { confirmacaoSenha = valor; }
    public void limpar() { senhaAtual = null; novaSenha = null; confirmacaoSenha = null; }
}
