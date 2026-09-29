package CompraSegura.chat;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "mensagens")
public class Mensagem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "conversa_id") private Conversa conversa;
    // O servico determina o remetente pela sessao e confere sua participacao.
    @Column(name = "remetente_id", nullable = false) private Long remetenteId;
    @Column(nullable = false, length = 2000) private String texto;
    @Column(name = "chave_envio", nullable = false, length = 36) private String chaveEnvio;
    @Column(name = "enviada_em", nullable = false) private LocalDateTime enviadaEm;
    protected Mensagem() { }
    public Mensagem(Conversa conversa, Long remetenteId, String texto, String chaveEnvio, LocalDateTime agora) {
        this.conversa = conversa; this.remetenteId = remetenteId; this.texto = texto; this.chaveEnvio = chaveEnvio; enviadaEm = agora;
    }
    public Long getId() { return id; }
    public Long getRemetenteId() { return remetenteId; }
    public String getTexto() { return texto; }
    public LocalDateTime getEnviadaEm() { return enviadaEm; }
}
