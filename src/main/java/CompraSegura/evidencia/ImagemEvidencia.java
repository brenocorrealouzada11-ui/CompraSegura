package CompraSegura.evidencia;

public record ImagemEvidencia(byte[] conteudo, String tipoConteudo) {
    public ImagemEvidencia { conteudo = conteudo.clone(); }
    @Override public byte[] conteudo() { return conteudo.clone(); }
}
