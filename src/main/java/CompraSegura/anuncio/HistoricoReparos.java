package CompraSegura.anuncio;

public enum HistoricoReparos {
    SEM_REPAROS("Sem reparos ou alterações"),
    COM_REPAROS("Já passou por reparos ou alterações"),
    DESCONHECIDO("Não sei informar");

    private final String descricao;
    HistoricoReparos(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
