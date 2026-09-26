package CompraSegura.anuncio;

public class PreparacaoAnuncioException extends RuntimeException {
    private final String campo;
    public PreparacaoAnuncioException(String campo, String mensagem) { super(mensagem); this.campo = campo; }
    public String getCampo() { return campo; }
}
