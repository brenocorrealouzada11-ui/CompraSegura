package CompraSegura.anuncio;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "aparelhos")
public class Aparelho {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TipoAparelho tipo;
    @Embedded
    private ModeloAparelho modelo;
    @Column(nullable = false, length = 1000)
    private String condicao;
    @Column(nullable = false, length = 1000)
    private String alteracoes;
    @ElementCollection
    @CollectionTable(name = "aparelho_imeis", joinColumns = @JoinColumn(name = "aparelho_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_aparelho_imei", columnNames = {"aparelho_id", "numero"}))
    @OrderColumn(name = "ordem")
    private List<IMEI> imeis = new ArrayList<>();
    protected Aparelho() { }
    public Aparelho(TipoAparelho tipo, ModeloAparelho modelo, String condicao, String alteracoes, List<String> numeros) {
        this.tipo = tipo; this.modelo = modelo; this.condicao = condicao; this.alteracoes = alteracoes;
        numeros.forEach(numero -> this.imeis.add(new IMEI(numero)));
    }
    public Long getId() { return id; }
    public TipoAparelho getTipo() { return tipo; }
    public ModeloAparelho getModelo() { return modelo; }
    public String getCondicao() { return condicao; }
    public String getAlteracoes() { return alteracoes; }
    public List<IMEI> getImeis() { return imeis; }
}
