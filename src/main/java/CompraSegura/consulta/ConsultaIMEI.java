package CompraSegura.consulta;

import CompraSegura.confiabilidade.RegistroAvaliacao;
import jakarta.persistence.*;
import java.time.*;

@Entity @Table(name = "consultas_imei")
public class ConsultaIMEI {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "avaliacao_id", nullable = false) private RegistroAvaliacao avaliacao;
    @Column(nullable = false, length = 15) private String imei;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SituacaoIMEI situacao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OrigemConsulta origem;
    @Column(nullable = false, length = 100) private String provedor;
    @Column(name = "consultado_em", nullable = false) private LocalDateTime consultadoEm;
    @Column(nullable = false, length = 1000) private String observacao;
    protected ConsultaIMEI() { }
    public ConsultaIMEI(RegistroAvaliacao avaliacao, ResultadoConsultaIMEI resultado) {
        this.avaliacao = avaliacao; imei = resultado.imei(); situacao = resultado.situacao(); origem = resultado.origem();
        provedor = resultado.provedor(); consultadoEm = LocalDateTime.ofInstant(resultado.consultadoEm(), ZoneOffset.UTC); observacao = resultado.observacao();
    }
    public ResultadoConsultaIMEI resultado() {
        return new ResultadoConsultaIMEI(imei, situacao, origem, provedor, consultadoEm.toInstant(ZoneOffset.UTC), observacao);
    }
}
