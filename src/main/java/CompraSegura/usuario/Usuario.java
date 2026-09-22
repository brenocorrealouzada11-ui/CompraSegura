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
