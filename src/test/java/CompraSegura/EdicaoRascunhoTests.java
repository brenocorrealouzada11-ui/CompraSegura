package CompraSegura;

import CompraSegura.anuncio.*;
import CompraSegura.confiabilidade.*;
import CompraSegura.evidencia.*;
import CompraSegura.usuario.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class EdicaoRascunhoTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AnuncioRepository anuncios;
    @Autowired CompraSegura.foto.FotoAnuncioRepository fotos;
    @Autowired PreparacaoAnuncioService preparacao;
    @Autowired EdicaoRascunhoService edicao;
    @Autowired EvidenciaService evidencias;
    @Autowired AvaliacaoRepository avaliacoes;
    @Autowired jakarta.persistence.EntityManager em;
    private UsuarioAutenticado dono, outro;
    private Long id;
    private String url;

    @BeforeEach void preparar() throws Exception {
        dono = new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario("Teste Edicao", UUID.randomUUID()+"@example.invalid", "hash-teste")));
        outro = new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario("Outra Conta", UUID.randomUUID()+"@example.invalid", "hash-teste")));
        var form = new AnuncioForm();
        form.setTitulo("Aparelho de demonstracao"); form.setPreco(new BigDecimal("199.90"));
        form.setTipo(TipoAparelho.SMARTPHONE); form.setMarca("Teste"); form.setModelo("Modelo");
        form.setCondicao(EstadoConservacao.BOM); form.setReparos(HistoricoReparos.SEM_REPAROS);
        form.setImeis("000000000000001\n000000000000002");
        id = preparacao.salvar(dono, form, imagem(0));
        fotos.saveAndFlush(new CompraSegura.foto.FotoAnuncio(anuncios.findById(id).orElseThrow(), TesteFotos.imagem()));
        url = "/minha-conta/anuncios/"+id+"/editar";
    }
    private MockMultipartFile imagem(int cor) throws Exception {
        var pixels = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
        pixels.setRGB(0, 0, cor);
        try (var bytes = new ByteArrayOutputStream()) {
            ImageIO.write(pixels, "png", bytes);
            return new MockMultipartFile("arquivo", "teste.png", "image/png", bytes.toByteArray());
        } finally { pixels.flush(); }
    }
    private MockMultipartHttpServletRequestBuilder envio(String destino, EdicaoRascunhoForm form) {
        return multipart(destino).with(user(dono)).with(csrf())
            .param("versao", form.getVersao().toString()).param("titulo", form.getTitulo())
            .param("preco", form.getPreco().toString()).param("tipo", form.getTipo().name())
            .param("marca", form.getMarca()).param("modelo", form.getModelo())
            .param("condicao", form.getCondicao().name()).param("reparos", form.getReparos().name())
            .param("descricao", form.getDescricao()).param("imeis", form.getImeis());
    }
    private RelatorioVerificacao ultimo() {
        return avaliacoes.findFirstByAnuncioIdAndAnuncioVendedorIdOrderByIdDesc(id, dono.getId()).orElseThrow().relatorio();
    }
    @Test void formularioPreenchidoEPermissoesEmTodasAsRotas() throws Exception {
        mvc.perform(get(url).with(user(dono))).andExpect(status().isOk())
            .andExpect(content().string(containsString("value=\"Aparelho de demonstracao\"")))
            .andExpect(content().string(containsString("Ver evidência atual")))
            .andExpect(content().string(containsString("Salvar alterações")));
        mvc.perform(get(url).with(user(outro))).andExpect(status().isNotFound());
        mvc.perform(post(url).with(user(outro)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(url+"/avaliar").with(user(outro)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get(url)).andExpect(status().is3xxRedirection());
        mvc.perform(post(url).with(user(dono))).andExpect(status().isForbidden());
    }
    @Test void alteraDadosSemDuplicarAnuncioPreservaImagemEHistorico() throws Exception {
        var antes = anuncios.findById(id).orElseThrow();
        Long aparelhoId = antes.getAparelho().getId();
        byte[] imagemAntes = evidencias.carregar(id, dono.getId()).conteudo();
        long totalAnuncios = anuncios.count(), totalAvaliacoes = avaliacoes.count();
        var form = edicao.carregar(id, dono);
        form.setModelo("Modelo atualizado"); form.setImeis("00000000-000003-0\n00000000-000001-0");
        mvc.perform(envio(url, form).param("status", "PUBLICADO").param("vendedor.id", outro.getId().toString()))
            .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("reavaliacaoNecessaria", true));
        em.flush(); em.clear();
        var salvo = anuncios.findById(id).orElseThrow();
        assertThat(salvo.getAparelho().getId()).isEqualTo(aparelhoId);
        assertThat(anuncios.findByIdAndVendedorId(id, dono.getId())).isPresent();
        assertThat(anuncios.findByIdAndVendedorId(id, outro.getId())).isEmpty();
        assertThat(salvo.getStatus()).isEqualTo("RASCUNHO");
        assertThat(salvo.getAparelho().getModelo().getNome()).isEqualTo("Modelo atualizado");
        assertThat(salvo.getAparelho().getImeis()).extracting(IMEI::getNumero).containsExactly("000000000000030", "000000000000010");
        assertThat(salvo.getVersao()).isGreaterThan(form.getVersao());
        assertThat(evidencias.carregar(id, dono.getId()).conteudo()).isEqualTo(imagemAntes);
        assertThat(anuncios.count()).isEqualTo(totalAnuncios);
        assertThat(avaliacoes.count()).isEqualTo(totalAvaliacoes);
        assertThat(ultimo().atual()).isFalse();
    }
    @Test void tituloPrecoEInversaoDosMesmosImeisPreservamAvaliacao() throws Exception {
        var form = edicao.carregar(id, dono);
        form.setTitulo("Titulo atualizado"); form.setPreco(new BigDecimal("250.00"));
        form.setImeis("000000000000002\n000000000000001");
        mvc.perform(envio(url, form)).andExpect(status().is3xxRedirection())
            .andExpect(flash().attribute("reavaliacaoNecessaria", false));
        em.flush(); em.clear();
        var salvo = anuncios.findById(id).orElseThrow();
        assertThat(salvo.getTitulo()).isEqualTo("Titulo atualizado");
        assertThat(salvo.getPreco()).isEqualByComparingTo("250.00");
        assertThat(salvo.getAparelho().getImeis()).extracting(IMEI::getNumero).containsExactly("000000000000002", "000000000000001");
        assertThat(ultimo().atual()).isTrue();
    }
    @Test void reparosExigemVinteCaracteresTambemNaEdicao() throws Exception {
        var form = edicao.carregar(id, dono);
        form.setReparos(HistoricoReparos.COM_REPAROS); form.setDescricao("Tela trocada");
        mvc.perform(envio(url, form)).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("anuncio", "descricao"));
        assertThat(edicao.carregar(id, dono).getReparos()).isEqualTo(HistoricoReparos.SEM_REPAROS);
        form.setDescricao("A tela foi substituída por assistência técnica.");
        mvc.perform(envio(url, form)).andExpect(status().is3xxRedirection());
        assertThat(ultimo().atual()).isFalse();
    }
    @Test void imagemInvalidaOuImeiInvalidoNaoAlteramRascunho() throws Exception {
        var form = edicao.carregar(id, dono);
        form.setTitulo("Nao deve ser salvo"); form.setImeis("123");
        mvc.perform(envio(url, form)).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("anuncio", "imeis"));
        form.setImeis("000000000000003");
        mvc.perform(envio(url, form).file(new MockMultipartFile("arquivo", "falsa.png", "image/png", new byte[]{1,2,3})))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("anuncio"));
        assertThat(edicao.carregar(id, dono).getTitulo()).isEqualTo("Aparelho de demonstracao");
        assertThat(edicao.carregar(id, dono).getVersao()).isEqualTo(form.getVersao());
        assertThat(ultimo().atual()).isTrue();
    }
    @Test void previaUsaImagemExistenteSemSalvarEEdicaoPodeSubstituiLa() throws Exception {
        var form = edicao.carregar(id, dono);
        long total = avaliacoes.count();
        byte[] antes = evidencias.carregar(id, dono.getId()).conteudo();
        mvc.perform(envio(url+"/avaliar", form)).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"));
        assertThat(edicao.previa(id, dono, form, null).avaliacao().pendencias().toString()).containsIgnoringCase("confer");
        assertThat(avaliacoes.count()).isEqualTo(total);
        assertThat(edicao.carregar(id, dono).getVersao()).isEqualTo(form.getVersao());
        mvc.perform(envio(url, form).file(imagem(0xffffff))).andExpect(status().is3xxRedirection());
        assertThat(evidencias.carregar(id, dono.getId()).conteudo()).isNotEqualTo(antes);
        assertThat(ultimo().atual()).isFalse();
    }
    @Test void abaAntigaNaoSobrescreveEdicaoNemSubstituicaoDeEvidencia() throws Exception {
        var antigo = edicao.carregar(id, dono);
        var novo = edicao.carregar(id, dono); novo.setTitulo("Titulo da outra aba");
        mvc.perform(envio(url, novo)).andExpect(status().is3xxRedirection());
        mvc.perform(envio(url, antigo)).andExpect(status().isOk()).andExpect(content().string(containsString("Reabra a edição")));
        assertThat(edicao.carregar(id, dono).getTitulo()).isEqualTo("Titulo da outra aba");
        var antesDaImagem = edicao.carregar(id, dono);
        evidencias.enviar(id, dono, imagem(0xffffff));
        mvc.perform(envio(url, antesDaImagem)).andExpect(status().isOk()).andExpect(model().attributeHasErrors("anuncio"));
        mvc.perform(envio(url+"/avaliar", antesDaImagem)).andExpect(status().isUnprocessableEntity());
    }
    @Test void somenteRascunhosPodemSerEditados() throws Exception {
        em.createNativeQuery("UPDATE anuncios SET status = 'PUBLICADO' WHERE id = :id").setParameter("id", id).executeUpdate();
        em.clear();
        mvc.perform(get(url).with(user(dono))).andExpect(status().isConflict());
        mvc.perform(post(url).with(user(dono)).with(csrf())).andExpect(status().isConflict());
    }
}
