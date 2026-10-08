package CompraSegura.chat;

import CompraSegura.anuncio.Anuncio;
import CompraSegura.usuario.Usuario;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "conversas")
@org.hibernate.annotations.DynamicUpdate
public class Conversa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "anuncio_id") private Anuncio anuncio;
    @Column(name = "anuncio_titulo", nullable = false, length = 120) private String anuncioTitulo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "comprador_id") private Usuario comprador;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vendedor_id") private Usuario vendedor;
    @Column(name = "criada_em", nullable = false) private LocalDateTime criadaEm;
    @Column(name = "atualizada_em", nullable = false) private LocalDateTime atualizadaEm;
    @Column(name = "ultima_previa", nullable = false, length = 160) private String ultimaPrevia = "";
    @Column(name = "lida_comprador", nullable = false) private long lidaComprador;
    @Column(name = "lida_vendedor", nullable = false) private long lidaVendedor;
    protected Conversa() { }
    public Conversa(Anuncio anuncio, Usuario comprador, LocalDateTime agora) {
        this.anuncio = anuncio; this.anuncioTitulo = anuncio.getTitulo(); this.comprador = comprador;
        vendedor = anuncio.getVendedor(); criadaEm = agora; atualizadaEm = agora;
    }
    public Long getId() { return id; }
    public String getAnuncioTitulo() { return anuncioTitulo; }
    public Usuario outro(Long usuarioId) { return comprador.getId().equals(usuarioId) ? vendedor : comprador; }
    public boolean isComprador(Long usuarioId) { return comprador.getId().equals(usuarioId); }
    public long lidaAte(Long usuarioId) { return isComprador(usuarioId) ? lidaComprador : lidaVendedor; }
    public String getUltimaPrevia() { return ultimaPrevia; }
    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
    public String situacaoNegociacao() { return anuncio == null || "RASCUNHO".equals(anuncio.getStatus()) ? "INDISPONIVEL" : "VENDIDO".equals(anuncio.getStatus()) ? "ANUNCIO_VENDIDO" : "EM_ANDAMENTO"; }
    public Long anuncioPublico() { return "INDISPONIVEL".equals(situacaoNegociacao()) ? null : anuncio.getId(); }
    public Long anuncioDisponivel() { return anuncio != null && "PUBLICADO".equals(anuncio.getStatus()) ? anuncio.getId() : null; }
    public void registrarMensagem(String texto, LocalDateTime agora) {
        int fim = Math.min(160, texto.length());
        if (fim > 0 && Character.isHighSurrogate(texto.charAt(fim-1))) fim--;
        ultimaPrevia = texto.substring(0, fim); atualizadaEm = agora;
    }
    public void marcarLida(Long usuarioId, long mensagemId) {
        if (isComprador(usuarioId)) lidaComprador = Math.max(lidaComprador, mensagemId);
        else lidaVendedor = Math.max(lidaVendedor, mensagemId);
    }
}
