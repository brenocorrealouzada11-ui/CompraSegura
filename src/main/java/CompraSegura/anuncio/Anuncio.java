package CompraSegura.anuncio;

import CompraSegura.usuario.Usuario;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "anuncios")
public class Anuncio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "aparelho_id", nullable = false, unique = true)
    private Aparelho aparelho;
    @Column(nullable = false, length = 120)
    private String titulo;
    @Column(nullable = false, length = 3000)
    private String descricao;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;
    @Column(nullable = false, length = 20)
    private String status = "RASCUNHO";
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
    protected Anuncio() { }
    public Anuncio(Usuario vendedor, Aparelho aparelho, String titulo, String descricao, BigDecimal preco) {
        this.vendedor = vendedor; this.aparelho = aparelho; this.titulo = titulo; this.descricao = descricao; this.preco = preco;
    }
    public Long getId() { return id; }
    public Aparelho getAparelho() { return aparelho; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPreco() { return preco; }
    public String getStatus() { return status; }
}
