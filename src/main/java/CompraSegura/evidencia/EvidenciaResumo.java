package CompraSegura.evidencia;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record EvidenciaResumo(String tipoConteudo, int tamanhoBytes, LocalDateTime enviadaEm) {
    public String getDataFormatada() { return enviadaEm.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")); }
}
