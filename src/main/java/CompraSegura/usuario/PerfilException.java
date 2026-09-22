package CompraSegura.usuario;

public class PerfilException extends RuntimeException {
    private final String campo;
    public PerfilException(String campo, String mensagem) { super(mensagem); this.campo = campo; }
    public String getCampo() { return campo; }
}
