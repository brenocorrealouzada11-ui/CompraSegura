package CompraSegura.anuncio;

public class EdicaoDesatualizadaException extends RuntimeException {
    public EdicaoDesatualizadaException() {
        super("Este rascunho foi alterado em outra aba. Reabra a edição para carregar os dados atuais antes de salvar.");
    }
}
