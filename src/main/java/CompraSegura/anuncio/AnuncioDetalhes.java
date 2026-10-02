package CompraSegura.anuncio;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public record AnuncioDetalhes(Long id, String titulo, String descricao, BigDecimal preco, String tipo,
                              String marca, String modelo, String condicao, String alteracoes, List<String> imeis, String status, long versao, java.time.LocalDateTime vendidoEm) {
    public boolean isRascunho() { return "RASCUNHO".equals(status); }
    public boolean isVendido() { return "VENDIDO".equals(status); }
    public String getVendaFormatada() { return vendidoEm == null ? "" : vendidoEm.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); }
    public boolean isPublicado() { return "PUBLICADO".equals(status); }
    public String getPrecoFormatado() { return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(preco); }
    public static AnuncioDetalhes de(Anuncio anuncio) {
        var aparelho = anuncio.getAparelho();
        return new AnuncioDetalhes(anuncio.getId(), anuncio.getTitulo(), anuncio.getDescricao(), anuncio.getPreco(),
            aparelho.getTipo().getDescricao(), aparelho.getModelo().getMarca(), aparelho.getModelo().getNome(),
            aparelho.getCondicao(), aparelho.getAlteracoes(), aparelho.getImeis().stream().map(IMEI::getNumero).toList(), anuncio.getStatus(), anuncio.getVersao(), anuncio.getVendidoEm());
    }
}
