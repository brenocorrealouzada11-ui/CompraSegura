package CompraSegura.reputacao;

import CompraSegura.chat.ConversaRepository;
import CompraSegura.usuario.*;
import jakarta.validation.Valid;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service @Validated
public class AvaliacaoPerfilService {
    private final AvaliacaoVendedorRepository avaliacoes;
    private final UsuarioRepository usuarios;
    private final ConversaRepository conversas;
    private final Clock relogio;
    public AvaliacaoPerfilService(AvaliacaoVendedorRepository avaliacoes, UsuarioRepository usuarios, ConversaRepository conversas, Clock relogio) {
        this.avaliacoes = avaliacoes; this.usuarios = usuarios; this.conversas = conversas; this.relogio = relogio;
    }
    public record Resumo(long total, String media) { }
    public static Resumo resumo(AvaliacaoVendedorRepository.Estatistica e) {
        return new Resumo(e.getTotal(), e.getMedia() == null ? "Sem avaliações" : String.format(Locale.forLanguageTag("pt-BR"), "%.1f", e.getMedia()));
    }
    public record Item(String autor, int nota, String comentario, String data) { }
    public record Perfil(Resumo resumo, Page<Item> itens, boolean podeAvaliar, boolean propria, boolean existente, AvaliacaoPerfilForm formulario) { }
    private void autenticar(UsuarioAutenticado principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var u = usuarios.findById(principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (u.getVersaoCredenciais() != principal.getVersaoCredenciais()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    @Transactional(readOnly = true)
    public void verificarPermissao(Long id, UsuarioAutenticado principal) {
        autenticar(principal);
        if (!usuarios.perfilPublico(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (id.equals(principal.getId()) || !conversas.houveTrocaDeMensagens(id, principal.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você precisa ter trocado mensagens com este vendedor para avaliá-lo.");
    }
    @Transactional(readOnly = true)
    public Perfil perfil(Long id, UsuarioAutenticado principal, int pagina) {
        if (!usuarios.perfilPublico(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (principal != null) autenticar(principal);
        var propria = principal != null && id.equals(principal.getId());
        var existente = principal == null ? java.util.Optional.<AvaliacaoVendedor>empty() : avaliacoes.findByVendedorIdAndAutorId(id, principal.getId());
        var form = new AvaliacaoPerfilForm(); existente.ifPresent(a -> { form.setNota(a.getNota()); form.setComentario(a.getComentario()); });
        var itens = avaliacoes.findByVendedorId(id, PageRequest.of(Math.max(0, Math.min(10000, pagina)), 10, Sort.by("atualizadaEm", "id").descending()))
            .map(a -> new Item(a.getAutor().getNome(), a.getNota(), a.getComentario(), a.getAtualizadaEm().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        return new Perfil(resumo(avaliacoes.estatistica(id)), itens, principal != null && !propria && conversas.houveTrocaDeMensagens(id, principal.getId()), propria, existente.isPresent(), form);
    }
    private void bloquearParticipantes(Long id, UsuarioAutenticado principal) {
        autenticar(principal);
        // A mesma ordem do chat evita inversão dos bloqueios entre os participantes.
        for (Long usuarioId : java.util.stream.Stream.of(id, principal.getId()).distinct().sorted().toList())
            usuarios.findParaAtualizacao(usuarioId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @Transactional
    public void salvar(Long id, UsuarioAutenticado principal, @Valid AvaliacaoPerfilForm form) {
        bloquearParticipantes(id, principal); verificarPermissao(id, principal);
        var atual = avaliacoes.findByVendedorIdAndAutorId(id, principal.getId()).orElse(null);
        var agora = LocalDateTime.now(relogio);
        if (atual == null) atual = new AvaliacaoVendedor(usuarios.getReferenceById(id), usuarios.getReferenceById(principal.getId()), form, agora);
        else atual.atualizar(form, agora);
        avaliacoes.saveAndFlush(atual);
    }
    @Transactional
    public boolean remover(Long id, UsuarioAutenticado principal) {
        bloquearParticipantes(id, principal);
        // A remoção usa sempre o autor da sessão, nunca um ID fornecido pelo formulário.
        var atual = avaliacoes.findByVendedorIdAndAutorId(id, principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        avaliacoes.delete(atual); avaliacoes.flush();
        return usuarios.perfilPublico(id);
    }
}
