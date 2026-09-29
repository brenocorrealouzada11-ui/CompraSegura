package CompraSegura.chat;

import CompraSegura.anuncio.AnuncioRepository;
import CompraSegura.usuario.*;
import jakarta.validation.Valid;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service @Validated
public class ChatService {
    private final ConversaRepository conversas;
    private final MensagemRepository mensagens;
    private final AnuncioRepository anuncios;
    private final UsuarioRepository usuarios;
    private final Clock relogio;
    private static final int TAMANHO = 50;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public ChatService(ConversaRepository conversas, MensagemRepository mensagens, AnuncioRepository anuncios, UsuarioRepository usuarios, Clock relogio) {
        this.conversas = conversas; this.mensagens = mensagens; this.anuncios = anuncios; this.usuarios = usuarios; this.relogio = relogio;
    }
    private Usuario autenticar(UsuarioAutenticado principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var usuario = usuarios.findById(principal.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (usuario.getVersaoCredenciais() != principal.getVersaoCredenciais()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return usuario;
    }
    private Conversa autorizar(Long id, UsuarioAutenticado principal, boolean bloquear) {
        autenticar(principal);
        return (bloquear ? conversas.autorizadaParaAtualizacao(id, principal.getId()) : conversas.autorizada(id, principal.getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @Transactional
    public Long iniciar(Long anuncioId, UsuarioAutenticado principal) {
        var comprador = autenticar(principal);
        Long vendedorId = anuncios.vendedorPublicado(anuncioId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (vendedorId.equals(comprador.getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Você não pode iniciar uma conversa consigo mesmo.");
        // Usuarios em ordem fixa antes do anuncio; evita criar conversas duplicadas
        // e inverte-los em duas negociacoes simultaneas entre as mesmas pessoas.
        for (Long participante : java.util.stream.Stream.of(comprador.getId(), vendedorId).sorted().toList()) {
            usuarios.findParaAtualizacao(participante).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        var anuncio = anuncios.publicadoParaConversa(anuncioId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return conversas.findByAnuncioIdAndCompradorId(anuncioId, comprador.getId())
            .orElseGet(() -> conversas.saveAndFlush(new Conversa(anuncio, comprador, LocalDateTime.now(relogio)))).getId();
    }
    public record Resumo(Long id, String pessoa, String titulo, String previa, String atualizadaEm, long naoLidas) { }
    public record MensagemVista(Long id, String texto, boolean propria, String horario) { }
    public record Lote(List<MensagemVista> mensagens, boolean temMais, Long anuncioId) {
        public long getPrimeiraId() { return mensagens.isEmpty() ? 0 : mensagens.get(0).id(); }
        public long getUltimaId() { return mensagens.isEmpty() ? 0 : mensagens.get(mensagens.size()-1).id(); }
    }
    public record ConversaVista(Long id, String pessoa, String titulo, boolean comprador, Long anuncioId, Lote lote) { }
    private MensagemVista vista(Mensagem mensagem, Long usuarioId) {
        return new MensagemVista(mensagem.getId(), mensagem.getTexto(), mensagem.getRemetenteId().equals(usuarioId), mensagem.getEnviadaEm().format(DATA));
    }
    @Transactional(readOnly = true)
    public Page<Resumo> listar(UsuarioAutenticado principal, int pagina) {
        autenticar(principal);
        return conversas.listar(principal.getId(), PageRequest.of(Math.max(0, Math.min(10000, pagina)), 12, Sort.by("atualizadaEm", "id").descending()))
            .map(c -> new Resumo(c.getId(), c.outro(principal.getId()).getNome(), c.getAnuncioTitulo(), c.getUltimaPrevia(), c.getAtualizadaEm().format(DATA),
                mensagens.countByConversaIdAndRemetenteIdNotAndIdGreaterThan(c.getId(), principal.getId(), c.lidaAte(principal.getId()))));
    }
    @Transactional(readOnly = true)
    public ConversaVista abrir(Long id, UsuarioAutenticado principal, Long antes) {
        var c = autorizar(id, principal, false);
        return new ConversaVista(id, c.outro(principal.getId()).getNome(), c.getAnuncioTitulo(), c.isComprador(principal.getId()), c.anuncioDisponivel(), historico(c, principal, antes));
    }
    private Lote historico(Conversa c, UsuarioAutenticado principal, Long antes) {
        if (antes != null && antes < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var dados = mensagens.findByConversaIdAndIdLessThanOrderByIdDesc(c.getId(), antes == null ? Long.MAX_VALUE : antes, PageRequest.of(0, TAMANHO+1));
        var itens = new ArrayList<>(dados.stream().limit(TAMANHO).map(m -> vista(m, principal.getId())).toList());
        Collections.reverse(itens);
        return new Lote(itens, dados.size() > TAMANHO, c.anuncioDisponivel());
    }
    @Transactional(readOnly = true)
    public Lote anteriores(Long id, UsuarioAutenticado principal, Long antes) { return historico(autorizar(id, principal, false), principal, antes); }
    @Transactional(readOnly = true)
    public Lote novas(Long id, UsuarioAutenticado principal, long apos) {
        if (apos < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var c = autorizar(id, principal, false);
        var dados = mensagens.findByConversaIdAndIdGreaterThanOrderByIdAsc(id, apos, PageRequest.of(0, TAMANHO+1));
        return new Lote(dados.stream().limit(TAMANHO).map(m -> vista(m, principal.getId())).toList(), dados.size() > TAMANHO, c.anuncioDisponivel());
    }
    @Transactional
    public MensagemVista enviar(Long id, UsuarioAutenticado principal, @Valid MensagemForm form) {
        var c = autorizar(id, principal, true);
        var existente = mensagens.findByConversaIdAndRemetenteIdAndChaveEnvio(id, principal.getId(), form.getChave());
        if (existente.isPresent()) {
            if (!existente.get().getTexto().equals(form.getTexto())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Este envio já foi recebido. Reabra a conversa antes de enviar outro texto.");
            return vista(existente.get(), principal.getId());
        }
        var agora = LocalDateTime.now(relogio);
        var mensagem = mensagens.saveAndFlush(new Mensagem(c, principal.getId(), form.getTexto(), form.getChave(), agora));
        c.registrarMensagem(form.getTexto(), agora);
        return vista(mensagem, principal.getId());
    }
    @Transactional
    public void marcarLida(Long id, UsuarioAutenticado principal, long ate) {
        var c = autorizar(id, principal, true);
        if (ate == 0) return;
        if (ate < 0 || !mensagens.existsByIdAndConversaId(ate, id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        c.marcarLida(principal.getId(), ate);
    }
    @Transactional(readOnly = true)
    public long naoLidas(UsuarioAutenticado principal) { autenticar(principal); return mensagens.naoLidas(principal.getId()); }
}
