package CompraSegura.foto;
import CompraSegura.anuncio.Anuncio;
import CompraSegura.evidencia.ImagemEvidencia;
import jakarta.persistence.*;
@Entity @Table(name = "fotos_anuncio")
public class FotoAnuncio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "anuncio_id") private Anuncio anuncio;
    @Lob @Column(nullable = false, columnDefinition = "MEDIUMBLOB") private byte[] conteudo;
    @Column(name = "tipo_conteudo", nullable = false, length = 30) private String tipoConteudo;
    protected FotoAnuncio() { }
    public FotoAnuncio(Anuncio anuncio, ImagemEvidencia imagem) { this.anuncio = anuncio; conteudo = imagem.conteudo(); tipoConteudo = imagem.tipoConteudo(); }
    public Long getId() { return id; }
    public ImagemEvidencia imagem() { return new ImagemEvidencia(conteudo, tipoConteudo); }
}
