package CompraSegura.anuncio;

import jakarta.persistence.*;

@Embeddable
public class ModeloAparelho {
    @Column(nullable = false, length = 80)
    private String marca;
    @Column(name = "modelo", nullable = false, length = 100)
    private String nome;
    protected ModeloAparelho() { }
    public ModeloAparelho(String marca, String nome) { this.marca = marca; this.nome = nome; }
    public String getMarca() { return marca; }
    public String getNome() { return nome; }
}
