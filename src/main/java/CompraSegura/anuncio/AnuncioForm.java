package CompraSegura.anuncio;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@ReparosDescritos
public class AnuncioForm {
    @NotBlank(message = "Informe o título.") @Size(min = 5, max = 120, message = "Use de 5 a 120 caracteres no título.")
    private String titulo;
    @Size(max = 3000, message = "Use até 3.000 caracteres nas observações.")
    private String descricao = "";
    @NotNull(message = "Informe o preço.") @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
    @Digits(integer = 10, fraction = 2, message = "Informe um preço válido com até duas casas decimais.")
    private BigDecimal preco;
    @NotNull(message = "Selecione o tipo do aparelho.")
    private TipoAparelho tipo;
    @NotBlank(message = "Informe a marca.") @Size(max = 80, message = "A marca deve ter até 80 caracteres.")
    private String marca;
    @NotBlank(message = "Informe o modelo.") @Size(max = 100, message = "O modelo deve ter até 100 caracteres.")
    private String modelo;
    @NotNull(message = "Selecione o estado de conservação.")
    private EstadoConservacao condicao;
    @NotNull(message = "Selecione o histórico de reparos.")
    private HistoricoReparos reparos;
    @NotBlank(message = "Informe todos os IMEIs do aparelho.") @Size(max = 1000, message = "O campo de IMEIs deve ter até 1.000 caracteres.")
    private String imeis;

    private String limpar(String texto) { return texto == null ? null : texto.strip(); }
    public String getTitulo() { return titulo; }
    public void setTitulo(String value) { titulo = limpar(value); }
    public String getDescricao() { return descricao; }
    public void setDescricao(String value) { descricao = value == null ? "" : value.strip(); }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal value) { preco = value; }
    public TipoAparelho getTipo() { return tipo; }
    public void setTipo(TipoAparelho value) { tipo = value; }
    public String getMarca() { return marca; }
    public void setMarca(String value) { marca = limpar(value); }
    public String getModelo() { return modelo; }
    public void setModelo(String value) { modelo = limpar(value); }
    public EstadoConservacao getCondicao() { return condicao; }
    public void setCondicao(EstadoConservacao value) { condicao = value; }
    public HistoricoReparos getReparos() { return reparos; }
    public void setReparos(HistoricoReparos value) { reparos = value; }
    public String getImeis() { return imeis; }
    public void setImeis(String value) { imeis = limpar(value); }
}
