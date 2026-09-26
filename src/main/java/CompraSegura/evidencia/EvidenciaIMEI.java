package CompraSegura.evidencia;

import CompraSegura.anuncio.Anuncio;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evidencias_imei")
public class EvidenciaIMEI {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anuncio_id", nullable = false, unique = true)
    private Anuncio anuncio;
    @Column(name = "tipo_conteudo", nullable = false, length = 20)
    private String tipoConteudo;
    @Column(name = "tamanho_bytes", nullable = false)
    private int tamanhoBytes;
    @Column(name = "enviada_em", nullable = false)
    private LocalDateTime enviadaEm;
    @Lob @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] conteudo;
    @Version
    private long versao;

    protected EvidenciaIMEI() { }
    public EvidenciaIMEI(Anuncio anuncio, ImagemEvidencia imagem) {
        this.anuncio = anuncio;
        atualizar(imagem);
    }
    public void atualizar(ImagemEvidencia imagem) {
        this.conteudo = imagem.conteudo();
        this.tipoConteudo = imagem.tipoConteudo();
        this.tamanhoBytes = this.conteudo.length;
        this.enviadaEm = LocalDateTime.now();
    }
    public byte[] getConteudo() { return conteudo.clone(); }
    public String getTipoConteudo() { return tipoConteudo; }
}
