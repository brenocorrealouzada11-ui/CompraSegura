package CompraSegura;

import CompraSegura.anuncio.*;
import CompraSegura.confiabilidade.*;
import CompraSegura.usuario.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class PreviaAvaliacaoTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AnuncioRepository anuncios;
    @Autowired AvaliacaoRepository avaliacoes;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    private UsuarioAutenticado dono, outro;
    @BeforeEach void preparar() {
        dono = new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario("Teste Previa",UUID.randomUUID()+"@example.invalid","hash-apenas-teste")));
        outro = new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario("Outra Conta",UUID.randomUUID()+"@example.invalid","hash-apenas-teste")));
    }
    private MockMultipartFile imagem() throws Exception {
        var pixels = new BufferedImage(3,3,BufferedImage.TYPE_INT_RGB);
        try(var bytes = new ByteArrayOutputStream()) { ImageIO.write(pixels,"png",bytes); return new MockMultipartFile("arquivo","teste.png","image/png",bytes.toByteArray()); }
        finally { pixels.flush(); }
    }
    private MockMultipartHttpServletRequestBuilder dados(String url) throws Exception {
        return multipart(url).file(imagem()).with(user(dono)).with(csrf())
            .param("titulo","Aparelho de demonstracao").param("preco","199.90").param("tipo","SMARTPHONE")
            .param("marca","Teste").param("modelo","Modelo").param("condicao","BOM").param("reparos","SEM_REPAROS")
            .param("descricao","").param("imeis","000000000000001\n000000000000002");
    }
    @Test void previaNaMesmaPaginaNaoCriaAnuncioOuEvidencia() throws Exception {
        long antes = anuncios.count();
        long antesAvaliacoes = avaliacoes.count();
        Long antesEvidencias = jdbc.queryForObject("SELECT COUNT(*) FROM evidencias_imei",Long.class);
        mvc.perform(get("/minha-conta/anuncios/novo").with(user(dono))).andExpect(status().isOk())
            .andExpect(content().string(containsString("Verificar aparelho")));
        mvc.perform(dados("/minha-conta/anuncios/novo/avaliar")).andExpect(status().isOk())
            .andExpect(content().string(containsString("SIMULAÇÃO")))
            .andExpect(content().string(containsString("Risco alto identificado")))
            .andExpect(content().string(containsString("000000000000001")))
            .andExpect(content().string(containsString("000000000000002")));
        assertThat(anuncios.count()).isEqualTo(antes);
        assertThat(avaliacoes.count()).isEqualTo(antesAvaliacoes);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evidencias_imei",Long.class)).isEqualTo(antesEvidencias);
    }
    @Test void salvarPersisteResultadoRecalculadoEImagemNaMesmaTransacao() throws Exception {
        String url = mvc.perform(dados("/minha-conta/anuncios/novo").param("risco","BAIXO"))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        Long id = Long.valueOf(url.substring(url.lastIndexOf('/')+1));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evidencias_imei WHERE anuncio_id = ?",Long.class,id)).isEqualTo(1);
        var relatorio = avaliacoes.findFirstByAnuncioIdAndAnuncioVendedorIdOrderByIdDesc(id,dono.getId()).orElseThrow().relatorio();
        assertThat(relatorio.avaliacao().risco()).isEqualTo(AvaliacaoConfiabilidade.NivelRisco.ALTO);
        assertThat(relatorio.consultas()).hasSize(2);
        mvc.perform(get(url).with(user(dono))).andExpect(status().isOk()).andExpect(content().string(containsString("SIMULAÇÃO")));
        mvc.perform(post(url+"/avaliar").with(user(outro)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(multipart(url+"/evidencia").file(imagem()).with(user(dono)).with(csrf()))
            .andExpect(flash().attribute("evidenciaSalva",true));
        assertThat(avaliacoes.findFirstByAnuncioIdAndAnuncioVendedorIdOrderByIdDesc(id,dono.getId()).orElseThrow().relatorio().atual()).isFalse();
        mvc.perform(post(url+"/avaliar").with(user(dono)).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM avaliacoes_confiabilidade WHERE anuncio_id = ?",Long.class,id)).isEqualTo(2);
        assertThat(avaliacoes.findFirstByAnuncioIdAndAnuncioVendedorIdOrderByIdDesc(id,dono.getId()).orElseThrow().relatorio().atual()).isTrue();
        mvc.perform(post(url+"/excluir").with(user(dono)).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM avaliacoes_confiabilidade WHERE anuncio_id = ?",Long.class,id)).isZero();
    }
    @Test void previaRejeitaDadosInvalidosESemCsrf() throws Exception {
        mvc.perform(post("/minha-conta/anuncios/novo/avaliar").with(user(dono))).andExpect(status().isForbidden());
        mvc.perform(dados("/minha-conta/anuncios/novo/avaliar").with(req -> {req.setParameter("imeis","123");return req;}))
            .andExpect(status().isUnprocessableEntity()).andExpect(content().string(containsString("15 dígitos")));
    }
}
