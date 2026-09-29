package CompraSegura.usuario;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(name = "uk_usuarios_email", columnNames = "email"))
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "versao_credenciais", nullable = false)
    private long versaoCredenciais;

    @Version
    @Column(name = "versao_registro", nullable = false)
    private long versaoRegistro;

    @Column(name = "exclusoes_rapidas", nullable = false)
    private int exclusoesRapidas;
    @Column(name = "bloqueado_ate")
    private java.time.LocalDateTime bloqueadoAte;
    public int getExclusoesRapidas() { return exclusoesRapidas; }
    public java.time.LocalDateTime getBloqueadoAte() { return bloqueadoAte; }
    public boolean publicacaoBloqueada(java.time.LocalDateTime agora) { return bloqueadoAte != null && agora.isBefore(bloqueadoAte); }
    public void expirarBloqueio(java.time.LocalDateTime agora) {
        if (bloqueadoAte != null && !agora.isBefore(bloqueadoAte)) { bloqueadoAte = null; exclusoesRapidas = 0; }
    }
    public void registrarExclusao(java.time.LocalDateTime publicadoEm, java.time.LocalDateTime agora) {
        agora = agora.withNano(0); // Mesma precisao de segundos usada no banco.
        expirarBloqueio(agora);
        // Um rascunho nunca publicado nao aumenta nem apaga a sequencia.
        if (publicadoEm == null || publicacaoBloqueada(agora)) return;
        boolean rapida = !agora.isBefore(publicadoEm) && !agora.isAfter(publicadoEm.plusMinutes(5));
        exclusoesRapidas = rapida ? exclusoesRapidas + 1 : 0;
        if (exclusoesRapidas >= 3) bloqueadoAte = agora.plusHours(24).withNano(0);
    }
    public String avisoPublicacao(java.time.LocalDateTime agora) {
        if (publicacaoBloqueada(agora)) return "Novas publicações estão bloqueadas até " + bloqueadoAte.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")) + ". Você pode continuar editando rascunhos e excluindo anúncios.";
        int quantidade = bloqueadoAte != null ? 0 : exclusoesRapidas;
        return "Exclusões rápidas consecutivas: " + quantidade + "/2 permitidas. Excluir até 5 minutos após publicar conta como exclusão rápida. Na terceira seguida, novas publicações ficam bloqueadas por 24 horas.";
    }

    protected Usuario() { }

    public Usuario(String nome, String email, String senhaHash) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public long getVersaoCredenciais() { return versaoCredenciais; }

    public void atualizarPerfil(String nome, String email) {
        this.nome = nome;
        if (!this.email.equals(email)) {
            this.email = email;
            this.versaoCredenciais++;
        }
    }

    public void alterarSenha(String senhaHash) {
        this.senhaHash = senhaHash;
        this.versaoCredenciais++;
    }
}
