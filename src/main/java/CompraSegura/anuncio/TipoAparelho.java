package CompraSegura.anuncio;

public enum TipoAparelho {
    SMARTPHONE("Smartphone"), TABLET("Tablet com conexão celular");
    private final String descricao;
    TipoAparelho(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
