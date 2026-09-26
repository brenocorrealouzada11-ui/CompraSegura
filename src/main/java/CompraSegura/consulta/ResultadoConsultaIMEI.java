package CompraSegura.consulta;

import java.time.Instant;
import java.util.Objects;

// Contrato para futuros provedores; nenhuma consulta e executada nesta etapa.
public record ResultadoConsultaIMEI(String imei, SituacaoIMEI situacao, OrigemConsulta origem,
                                     String provedor, Instant consultadoEm, String observacao) {
    public ResultadoConsultaIMEI {
        if (imei == null || !imei.matches("[0-9]{15}")) throw new IllegalArgumentException("IMEI deve conter 15 digitos.");
        Objects.requireNonNull(situacao);
        Objects.requireNonNull(origem);
        Objects.requireNonNull(consultadoEm);
        if (provedor == null || provedor.isBlank()) throw new IllegalArgumentException("Informe a fonte da consulta.");
        observacao = observacao == null ? "" : observacao;
    }
}
