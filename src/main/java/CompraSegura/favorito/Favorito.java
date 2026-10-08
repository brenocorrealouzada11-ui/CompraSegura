package CompraSegura.favorito;

import CompraSegura.anuncio.Anuncio;
import CompraSegura.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "favoritos")
public class Favorito {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id") private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "anuncio_id") private Anuncio anuncio;
    @Column(name = "titulo_salvo", nullable = false, length = 120) private String tituloSalvo;
    @Column(name = "salvo_em", nullable = false) private LocalDateTime salvoEm;
    protected Favorito() { }
    public Favorito(Usuario usuario, Anuncio anuncio, LocalDateTime agora) {
        this.usuario = usuario; this.anuncio = anuncio; tituloSalvo = anuncio.getTitulo(); salvoEm = agora.withNano(0);
    }
    public Long getId() { return id; }
    public Anuncio getAnuncio() { return anuncio; }
    public String getTituloSalvo() { return tituloSalvo; }
    public LocalDateTime getSalvoEm() { return salvoEm; }
}
