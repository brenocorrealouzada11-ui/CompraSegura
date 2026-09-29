package CompraSegura.chat;
import jakarta.validation.constraints.*;
import java.util.UUID;
public class MensagemForm {
    @NotBlank(message = "Escreva uma mensagem.")
    @Size(max = 2000, message = "Use até 2.000 caracteres por mensagem.")
    private String texto = "";
    @NotBlank(message = "Reabra a conversa antes de enviar.")
    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", message = "Reabra a conversa antes de enviar.")
    private String chave = UUID.randomUUID().toString();
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto == null ? "" : texto.strip(); }
    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }
}
