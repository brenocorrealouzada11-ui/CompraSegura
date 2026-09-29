package CompraSegura.catalogo;

import CompraSegura.anuncio.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class FiltroPesquisa {
    public enum Ordem { RECENTES, MENOR_PRECO, MAIOR_PRECO }
    @Size(max = 120, message = "Pesquise com até 120 caracteres.") private String q = "";
    private TipoAparelho tipo;
    private EstadoConservacao condicao;
    @DecimalMin(value = "0", message = "O preço mínimo não pode ser negativo.") @Digits(integer = 10, fraction = 2)
    private BigDecimal minimo;
    @DecimalMin(value = "0", message = "O preço máximo não pode ser negativo.") @Digits(integer = 10, fraction = 2)
    private BigDecimal maximo;
    @NotNull private Ordem ordem = Ordem.RECENTES;
    @Min(value = 0, message = "A página deve ser zero ou maior.") @Max(10000) private int pagina;
    @AssertTrue(message = "O preço mínimo não pode ser maior que o máximo.")
    public boolean isFaixaValida() { return minimo == null || maximo == null || minimo.compareTo(maximo) <= 0; }
    public String getQ() { return q; } public void setQ(String q) { this.q = q == null ? "" : q.strip(); }
    public TipoAparelho getTipo() { return tipo; } public void setTipo(TipoAparelho tipo) { this.tipo = tipo; }
    public EstadoConservacao getCondicao() { return condicao; } public void setCondicao(EstadoConservacao condicao) { this.condicao = condicao; }
    public BigDecimal getMinimo() { return minimo; } public void setMinimo(BigDecimal minimo) { this.minimo = minimo; }
    public BigDecimal getMaximo() { return maximo; } public void setMaximo(BigDecimal maximo) { this.maximo = maximo; }
    public Ordem getOrdem() { return ordem; } public void setOrdem(Ordem ordem) { this.ordem = ordem; }
    public int getPagina() { return pagina; } public void setPagina(int pagina) { this.pagina = pagina; }
}
