package CompraSegura;

import CompraSegura.anuncio.*;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Conferencia restrita ao incremento de evidencias e observacoes condicionais.
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class EvidenciaEReparosTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AnuncioService anuncios;
    @Autowired EvidenciaRepository evidencias;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    private UsuarioAutenticado dono, outro;
    private Long id;

    @BeforeEach void preparar() {
        dono = new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario("Teste Evidencia", UUID.randomUUID()+"@example.invalid", "hash-apenas-teste")));
        outro = new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario("Outra Conta", UUID.randomUUID()+"@example.invalid", "hash-apenas-teste")));
        var form = new AnuncioForm();
        form.setTitulo("Aparelho para teste"); form.setPreco(new BigDecimal("199.90"));
        form.setTipo(TipoAparelho.SMARTPHONE); form.setMarca("Teste"); form.setModelo("Modelo");
        form.setCondicao(EstadoConservacao.BOM); form.setReparos(HistoricoReparos.SEM_REPAROS);
        form.setImeis("123456789012345");
        id = anuncios.criar(dono, form);
    }
    private String rota() { return "/minha-conta/anuncios/" + id + "/evidencia"; }
    private byte[] imagem(String formato) throws Exception {
        var imagem = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        try(var saida = new ByteArrayOutputStream()) {
            ImageIO.write(imagem, formato, saida);
            return saida.toByteArray();
        } finally { imagem.flush(); }
    }
    private MockHttpServletRequestBuilder cadastro(String descricao) {
        return post("/minha-conta/anuncios/novo").with(user(dono)).with(csrf())
            .param("titulo", "Aparelho reparado").param("descricao", descricao).param("preco", "199.90")
            .param("tipo", "SMARTPHONE").param("marca", "Teste").param("modelo", "Modelo")
            .param("condicao", "BOM").param("reparos", "COM_REPAROS").param("imeis", "123456789012345");
    }
    @Test void reparosExigemVinteCaracteresMesmoSemJavascript() throws Exception {
        mvc.perform(get("/minha-conta/anuncios/novo").with(user(dono))).andExpect(status().isOk());
        for (String descricao : new String[]{"", "1234567890123456789", "   1234567890123456789   "}) {
            mvc.perform(cadastro(descricao)).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("anuncio", "descricao"))
                .andExpect(content().string(containsString("minlength=\"20\"")))
                .andExpect(content().string(containsString("Informe os reparos e alterações")));
        }
        mvc.perform(cadastro("12345678901234567890")).andExpect(status().is3xxRedirection());
    }
    @Test void envioEVisualizacaoExigemDonoECsrf() throws Exception {
        var arquivo = new MockMultipartFile("arquivo", "captura.png", "image/png", imagem("png"));
        mvc.perform(multipart(rota()).file(arquivo).with(user(dono))).andExpect(status().isForbidden());
        mvc.perform(multipart(rota()).file(arquivo).with(user(outro)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(multipart(rota()).file(arquivo).with(user(dono)).with(csrf()))
            .andExpect(flash().attribute("evidenciaSalva", true));
        mvc.perform(get(rota()).with(user(outro))).andExpect(status().isNotFound());
        mvc.perform(get(rota())).andExpect(redirectedUrl("/login"));
        mvc.perform(get(rota()).with(user(dono))).andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(get("/minha-conta/anuncios/" + id).with(user(dono)))
            .andExpect(status().isOk()).andExpect(content().string(containsString("aguardando conferência")));
    }
    @Test void rejeitaArquivoFalsoOuGrandeEPreservaImagemAnterior() throws Exception {
        mvc.perform(multipart(rota()).file(new MockMultipartFile("arquivo", "captura.png", "image/png", imagem("png")))
            .with(user(dono)).with(csrf())).andExpect(flash().attribute("evidenciaSalva", true));
        var original = evidencias.findByAnuncioIdAndAnuncioVendedorId(id, dono.getId()).orElseThrow().getConteudo();
        for(byte[] conteudo : new byte[][] {new byte[0], "<script>arquivo falso</script>".getBytes(), new byte[ValidadorImagemEvidencia.MAX_BYTES + 1]}) {
            mvc.perform(multipart(rota()).file(new MockMultipartFile("arquivo", "falsa.png", "image/png", conteudo))
                .with(user(dono)).with(csrf())).andExpect(flash().attributeExists("erroEvidencia"));
            assertThat(evidencias.findByAnuncioIdAndAnuncioVendedorId(id, dono.getId()).orElseThrow().getConteudo()).isEqualTo(original);
        }
        // O formato real prevalece sobre o nome/MIME fornecidos pelo cliente.
        mvc.perform(multipart(rota()).file(new MockMultipartFile("arquivo", "arquivo.txt", "text/plain", imagem("jpeg")))
            .with(user(dono)).with(csrf())).andExpect(flash().attribute("evidenciaSalva", true));
        mvc.perform(get(rota()).with(user(dono))).andExpect(content().contentType("image/jpeg"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evidencias_imei WHERE anuncio_id = ?", Long.class, id)).isEqualTo(1);
    }
    @Test void excluirRascunhoRemoveEvidenciaJunto() throws Exception {
        mvc.perform(multipart(rota()).file(new MockMultipartFile("arquivo", "captura.png", "image/png", imagem("png")))
            .with(user(dono)).with(csrf())).andExpect(flash().attribute("evidenciaSalva", true));
        mvc.perform(post("/minha-conta/anuncios/" + id + "/excluir").with(user(dono)).with(csrf()))
            .andExpect(redirectedUrl("/minha-conta/anuncios"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evidencias_imei WHERE anuncio_id = ?", Long.class, id)).isZero();
    }
}
