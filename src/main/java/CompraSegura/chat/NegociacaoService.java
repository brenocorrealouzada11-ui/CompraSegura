package CompraSegura.chat;

import CompraSegura.usuario.*;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NegociacaoService {
    public enum Papel { TODOS, COMPRANDO, VENDENDO }
    public enum Situacao { TODAS, EM_ANDAMENTO, ANUNCIO_VENDIDO, INDISPONIVEL }
    private final ConversaRepository conversas;
    private final MensagemRepository mensagens;
    private final UsuarioRepository usuarios;
    public NegociacaoService(ConversaRepository conversas, MensagemRepository mensagens, UsuarioRepository usuarios) {
        this.conversas = conversas; this.mensagens = mensagens; this.usuarios = usuarios;
    }
    public record Item(Long conversaId, Long anuncioId, String titulo, String pessoa, boolean comprando, String situacao, String previa, String atualizadaEm, long naoLidas) {
        public String getSituacaoDescricao() { return switch (situacao) { case "EM_ANDAMENTO" -> "Em andamento · anúncio disponível"; case "ANUNCIO_VENDIDO" -> "Anúncio vendido"; default -> "Anúncio indisponível"; }; }
    }
    @Transactional(readOnly = true)
    public Page<Item> listar(UsuarioAutenticado principal, Papel papel, Situacao situacao, int pagina) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findById(principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (usuario.getVersaoCredenciais() != principal.getVersaoCredenciais()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return conversas.negociacoes(principal.getId(), papel.name(), situacao.name(), PageRequest.of(Math.max(0, Math.min(10000, pagina)), 12, Sort.by("atualizadaEm", "id").descending()))
            .map(c -> new Item(c.getId(), c.anuncioPublico(), c.getAnuncioTitulo(), c.outro(principal.getId()).getNome(), c.isComprador(principal.getId()),
                c.situacaoNegociacao(), c.getUltimaPrevia(), c.getAtualizadaEm().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                mensagens.countByConversaIdAndRemetenteIdNotAndIdGreaterThan(c.getId(), principal.getId(), c.lidaAte(principal.getId()))));
    }
}
