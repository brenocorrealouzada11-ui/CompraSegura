package CompraSegura.anuncio;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public record AnuncioDetalhes(Long id, String titulo, String descricao, BigDecimal preco, String tipo,
                              String marca, String modelo, String condicao, String alteracoes, List<String> imeis) {
    public String getPrecoFormatado() { return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(preco); }
    public static AnuncioDetalhes de(Anuncio anuncio) {
        var aparelho = anuncio.getAparelho();
        return new AnuncioDetalhes(anuncio.getId(), anuncio.getTitulo(), anuncio.getDescricao(), anuncio.getPreco(),
            aparelho.getTipo().getDescricao(), aparelho.getModelo().getMarca(), aparelho.getModelo().getNome(),
            aparelho.getCondicao(), aparelho.getAlteracoes(), aparelho.getImeis().stream().map(IMEI::getNumero).toList());
    }
}
