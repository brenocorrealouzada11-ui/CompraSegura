package CompraSegura.catalogo;

import jakarta.validation.constraints.*;

public class FiltroVendedor {
    @Size(max = 100, message = "Pesquise o nome com até 100 caracteres.")
    private String nome = "";
    @Min(value = 0, message = "A página deve ser zero ou maior.") @Max(value = 10000, message = "Página fora do limite.")
    private int pagina;
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome == null ? "" : nome.strip(); }
    public int getPagina() { return pagina; }
    public void setPagina(int pagina) { this.pagina = pagina; }
}
