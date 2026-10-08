package CompraSegura.reputacao;

import jakarta.validation.constraints.*;

public class AvaliacaoPerfilForm {
    @NotNull(message = "Escolha uma nota de 1 a 5.") @Min(value = 1, message = "A nota deve ser de 1 a 5.") @Max(value = 5, message = "A nota deve ser de 1 a 5.")
    private Integer nota;
    @Size(max = 1000, message = "O comentário pode ter até 1.000 caracteres.")
    private String comentario = "";
    public Integer getNota() { return nota; }
    public void setNota(Integer nota) { this.nota = nota; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario == null ? "" : comentario.strip(); }
}
