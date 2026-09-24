package CompraSegura.anuncio;

public enum EstadoConservacao {
    PERFEITO("Em perfeito estado"),
    BOM("Em boas condições"),
    COM_MARCAS("Com marcas de uso"),
    COM_DEFEITOS("Funciona com defeitos"),
    PECAS("Para retirada de peças");

    private final String descricao;
    EstadoConservacao(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
