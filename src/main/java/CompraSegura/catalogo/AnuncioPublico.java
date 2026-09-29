package CompraSegura.catalogo;

import CompraSegura.anuncio.Anuncio;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Somente dados publicos: nenhum email, credencial, IMEI completo ou arquivo de evidencia.
public record AnuncioPublico(Long id, String titulo, String descricao, BigDecimal preco, String tipo,
        String marca, String modelo, String condicao, String reparos, Long vendedorId, String vendedorNome, LocalDateTime publicadoEm, java.util.List<Long> fotos, boolean fotoPerfil) {
    public static AnuncioPublico de(Anuncio a, java.util.List<Long> fotos, boolean fotoPerfil) {
        var p = a.getAparelho();
        return new AnuncioPublico(a.getId(), a.getTitulo(), a.getDescricao(), a.getPreco(), p.getTipo().getDescricao(),
            p.getModelo().getMarca(), p.getModelo().getNome(), p.getCondicao(), p.getAlteracoes(), a.getVendedor().getId(), a.getVendedor().getNome(), a.getPublicadoEm(), java.util.List.copyOf(fotos), fotoPerfil);
    }
    public Long getCapa() { return fotos.isEmpty() ? null : fotos.get(0); }
    public String getPrecoFormatado() { return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(preco); }
    public String getDataFormatada() { return publicadoEm == null ? "Data não registrada" : publicadoEm.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
}
