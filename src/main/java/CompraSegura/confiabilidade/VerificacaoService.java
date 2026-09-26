package CompraSegura.confiabilidade;

import CompraSegura.anuncio.*;
import CompraSegura.consulta.*;
import CompraSegura.evidencia.EvidenciaRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VerificacaoService {
    private final ProvedorConsultaIMEI provedor;
    private final AvaliadorConfiabilidade avaliador;
    private final AvaliacaoRepository avaliacoes;
    private final AnuncioRepository anuncios;
    private final EvidenciaRepository evidencias;
    public VerificacaoService(ProvedorConsultaIMEI provedor, AvaliadorConfiabilidade avaliador,
                              AvaliacaoRepository avaliacoes, AnuncioRepository anuncios, EvidenciaRepository evidencias) {
        this.provedor = provedor; this.avaliador = avaliador; this.avaliacoes = avaliacoes;
        this.anuncios = anuncios; this.evidencias = evidencias;
    }

    public VerificacaoPreparada avaliar(List<String> imeis, String condicao, String reparos, String observacoes, boolean temEvidencia) {
        var consultas = new ArrayList<ResultadoConsultaIMEI>();
        for (String imei : imeis) {
            try {
                var resultado = provedor.consultar(imei);
                if (resultado == null || !imei.equals(resultado.imei()) || resultado.origem() != provedor.origem()) {
                    throw new IllegalStateException("Resposta incompatível com a consulta.");
                }
                consultas.add(resultado);
            } catch (RuntimeException ex) {
                consultas.add(new ResultadoConsultaIMEI(imei, SituacaoIMEI.INDISPONIVEL,
                    provedor.origem(), provedor.nome(), Instant.now(), "A consulta não pôde ser concluída. Tente novamente depois."));
            }
        }
        var contexto = new ContextoAvaliacao(imeis, consultas,
            temEvidencia ? ContextoAvaliacao.SituacaoEvidencia.AGUARDANDO_CONFERENCIA : ContextoAvaliacao.SituacaoEvidencia.NAO_ENVIADA,
            condicao, reparos, observacoes, ContextoAvaliacao.HistoricoVendedor.NAO_DISPONIVEL);
        return new VerificacaoPreparada(contexto, new RelatorioVerificacao(consultas, avaliador.avaliar(contexto), true));
    }

    @Transactional
    public void registrar(Long anuncioId, Long vendedorId, VerificacaoPreparada verificacao) {
        var anuncio = anuncios.findAutorizadoParaAtualizacao(anuncioId, vendedorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        avaliacoes.findByAnuncioIdAndAtualTrue(anuncioId).forEach(RegistroAvaliacao::desatualizar);
        String imagemHash = evidencias.findByAnuncioIdAndAnuncioVendedorId(anuncioId, vendedorId)
            .map(e -> hashImagem(e.getConteudo())).orElse(null);
        avaliacoes.saveAndFlush(new RegistroAvaliacao(anuncio, verificacao.relatorio(), verificacao.contexto(), imagemHash));
    }

    private static String hashImagem(byte[] conteudo) {
        try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(conteudo)); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 não disponível.", ex); }
    }

    @Transactional
    public void reavaliar(Long anuncioId, Long vendedorId) {
        var anuncio = anuncios.findAutorizadoParaAtualizacao(anuncioId, vendedorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var dados = AnuncioDetalhes.de(anuncio);
        var verificacao = avaliar(dados.imeis(), dados.condicao(), dados.alteracoes(), dados.descricao(), evidencias.resumo(anuncioId, vendedorId).isPresent());
        registrar(anuncioId, vendedorId, verificacao);
    }

    @Transactional(readOnly = true)
    public RelatorioVerificacao ultimo(Long anuncioId, Long vendedorId) {
        return avaliacoes.findFirstByAnuncioIdAndAnuncioVendedorIdOrderByIdDesc(anuncioId, vendedorId)
            .map(RegistroAvaliacao::relatorio).orElse(null);
    }
}
