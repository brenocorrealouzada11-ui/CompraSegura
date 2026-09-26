package CompraSegura.confiabilidade;

import CompraSegura.anuncio.Anuncio;
import CompraSegura.consulta.ConsultaIMEI;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity @Table(name = "avaliacoes_confiabilidade")
public class RegistroAvaliacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "anuncio_id", nullable = false) private Anuncio anuncio;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AvaliacaoConfiabilidade.NivelRisco risco;
    @Column(name = "contem_simulacao", nullable = false) private boolean contemSimulacao;
    @Column(name = "versao_regra", nullable = false, length = 80) private String versaoRegra;
    @Column(name = "avaliada_em", nullable = false) private LocalDateTime avaliadaEm;
    @Column(nullable = false) private boolean atual;
    @Column(nullable = false, columnDefinition = "TEXT") private String motivos;
    @Column(nullable = false, columnDefinition = "TEXT") private String pendencias;
    @Column(nullable = false, columnDefinition = "TEXT") private String contexto;
    @Column(name = "evidencia_sha256", length = 64) private String evidenciaSha256;
    @OneToMany(mappedBy = "avaliacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ConsultaIMEI> consultas = new ArrayList<>();
    protected RegistroAvaliacao() { }
    public RegistroAvaliacao(Anuncio anuncio, RelatorioVerificacao relatorio, ContextoAvaliacao contexto, String evidenciaSha256) {
        this.anuncio = anuncio;
        this.evidenciaSha256 = evidenciaSha256;
        var resultado = relatorio.avaliacao();
        risco = resultado.risco(); contemSimulacao = resultado.contemSimulacao(); versaoRegra = resultado.versaoRegra();
        avaliadaEm = LocalDateTime.ofInstant(resultado.avaliadaEm(), ZoneOffset.UTC); atual = true;
        motivos = String.join("\n", resultado.motivos()); pendencias = String.join("\n", resultado.pendencias());
        this.contexto = contexto.toString();
        relatorio.consultas().forEach(c -> consultas.add(new ConsultaIMEI(this, c)));
    }
    public RelatorioVerificacao relatorio() {
        var resultado = new AvaliacaoConfiabilidade(risco, motivos.lines().toList(), pendencias.lines().toList(), contemSimulacao,
            versaoRegra, avaliadaEm.toInstant(ZoneOffset.UTC));
        boolean recente = resultado.avaliadaEm().isAfter(Instant.now().minus(Duration.ofHours(24)));
        return new RelatorioVerificacao(consultas.stream().map(ConsultaIMEI::resultado).toList(), resultado, atual && recente);
    }
    public void desatualizar() { atual = false; }
}
