package CompraSegura.anuncio;

import jakarta.persistence.*;

@Embeddable
public class IMEI {
    @Column(name = "numero", nullable = false, length = 15)
    private String numero;
    protected IMEI() { }
    public IMEI(String numero) { this.numero = numero; }
    public String getNumero() { return numero; }
}
