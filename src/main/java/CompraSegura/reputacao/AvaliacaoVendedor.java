package CompraSegura.reputacao;

import CompraSegura.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "avaliacoes_vendedor")
public class AvaliacaoVendedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vendedor_id") private Usuario vendedor;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "autor_id") private Usuario autor;
    @Column(nullable = false) private int nota;
    @Column(nullable = false, length = 1000) private String comentario;
    @Column(name = "criada_em", nullable = false) private LocalDateTime criadaEm;
    @Column(name = "atualizada_em", nullable = false) private LocalDateTime atualizadaEm;
    protected AvaliacaoVendedor() { }
    public AvaliacaoVendedor(Usuario vendedor, Usuario autor, AvaliacaoPerfilForm form, LocalDateTime agora) {
        this.vendedor = vendedor; this.autor = autor; criadaEm = agora.withNano(0); atualizar(form, agora);
    }
    public void atualizar(AvaliacaoPerfilForm form, LocalDateTime agora) { nota = form.getNota(); comentario = form.getComentario(); atualizadaEm = agora.withNano(0); }
    public Usuario getAutor() { return autor; }
    public int getNota() { return nota; }
    public String getComentario() { return comentario; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
}
