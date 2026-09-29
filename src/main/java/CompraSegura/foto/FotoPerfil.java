package CompraSegura.foto;
import CompraSegura.usuario.Usuario;
import CompraSegura.evidencia.ImagemEvidencia;
import jakarta.persistence.*;
@Entity @Table(name = "fotos_perfil")
public class FotoPerfil {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", unique = true) private Usuario usuario;
    @Lob @Column(nullable = false, columnDefinition = "MEDIUMBLOB") private byte[] conteudo;
    @Column(name = "tipo_conteudo", nullable = false, length = 30) private String tipoConteudo;
    protected FotoPerfil() { }
    public FotoPerfil(Usuario usuario, ImagemEvidencia imagem) { this.usuario = usuario; atualizar(imagem); }
    public void atualizar(ImagemEvidencia imagem) { conteudo = imagem.conteudo(); tipoConteudo = imagem.tipoConteudo(); }
    public ImagemEvidencia imagem() { return new ImagemEvidencia(conteudo, tipoConteudo); }
}
