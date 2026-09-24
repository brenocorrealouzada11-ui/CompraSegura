package CompraSegura;

import CompraSegura.anuncio.*;
import CompraSegura.usuario.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AnuncioTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AnuncioRepository anuncios;
    @Autowired AnuncioService service;
    @Autowired PasswordEncoder encoder;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    private Long vendedorId;
    private Long outroId;
    private static final String SENHA = "senha ficticia de anuncios";

    @BeforeEach void preparar() {
        anuncios.deleteAll();
        usuarios.deleteAll();
        vendedorId = usuarios.saveAndFlush(new Usuario("Vendedor Teste", "vendedor@example.com", encoder.encode(SENHA))).getId();
        outroId = usuarios.saveAndFlush(new Usuario("Outra Conta", "outro@example.com", encoder.encode(SENHA))).getId();
    }
    @AfterEach void limpar() { anuncios.deleteAll(); }
    private MockHttpSession entrar(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("email", email).param("senha", SENHA))
            .andExpect(redirectedUrl("/minha-conta")).andReturn().getRequest().getSession(false);
    }
    private MockHttpServletRequestBuilder valido(MockHttpSession sessao) {
        return post("/minha-conta/anuncios/novo").session(sessao).with(csrf())
            .param("titulo", "  Aparelho de teste  ").param("descricao", "Descricao completa do aparelho")
            .param("preco", "1599.90").param("tipo", "SMARTPHONE").param("marca", "Marca Teste")
            .param("modelo", "Modelo Teste").param("condicao", "COM_MARCAS").param("reparos", "SEM_REPAROS")
            .param("imeis", "123456789012345\n987654321098765");
    }
    private Long criar(MockHttpSession sessao) throws Exception {
        String url = mvc.perform(valido(sessao)).andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        return Long.valueOf(url.substring(url.lastIndexOf('/') + 1));
    }
    @Test void paginasProtegidasEFormularioDisponivel() throws Exception {
        for (String path : new String[]{"/minha-conta/anuncios", "/minha-conta/anuncios/novo", "/minha-conta/anuncios/1"}) {
            mvc.perform(get(path)).andExpect(redirectedUrl("/login"));
        }
        var sessao = entrar("vendedor@example.com");
        mvc.perform(get("/minha-conta/anuncios/novo").session(sessao)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Salvar rascunho")))
            .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/minha-conta").session(sessao)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Segurança da conta")))
            .andExpect(content().string(containsString("Alterar senha")));
    }
    @Test void criaRascunhoComTodosImeisEIgnoraProprietarioEStatusInjetados() throws Exception {
        var sessao = entrar("vendedor@example.com");
        mvc.perform(valido(sessao).param("vendedorId", outroId.toString()).param("status", "PUBLICADO"))
            .andExpect(status().is3xxRedirection());
        assertThat(anuncios.countByVendedorId(vendedorId)).isEqualTo(1);
        assertThat(anuncios.countByVendedorId(outroId)).isZero();
        var salvo = anuncios.findAll().get(0);
        assertThat(salvo.getStatus()).isEqualTo("RASCUNHO");
        var detalhe = service.buscar(salvo.getId(), vendedorId);
        assertThat(detalhe.imeis()).containsExactly("123456789012345", "987654321098765");
        assertThat(detalhe.preco()).isEqualByComparingTo("1599.90");
        assertThat(detalhe.titulo()).isEqualTo("Aparelho de teste");
        mvc.perform(get("/minha-conta/anuncios/" + salvo.getId()).session(sessao)).andExpect(status().isOk())
            .andExpect(content().string(containsString("123456789012345")))
            .andExpect(content().string(containsString("Consulta de IMEI ainda não realizada")));
        mvc.perform(get("/minha-conta/anuncios").session(sessao)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Aparelho de teste")));
    }
    @Test void rejeitaImeisInvalidosRepetidosEVaziosSemSalvar() throws Exception {
        var sessao = entrar("vendedor@example.com");
        for (String valor : new String[]{"123", "12345678901234x", "123456789012345\n123456789012345", " ", ",;"}) {
            var request = valido(sessao);
            // Substitui o valor existente para testar cada entrada isoladamente.
            request.with(req -> { req.setParameter("imeis", valor); return req; });
            mvc.perform(request).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("anuncio", "imeis"));
        }
        assertThat(anuncios.count()).isZero();
    }
    @Test void validaPrecoTipoECamposObrigatorios() throws Exception {
        var sessao = entrar("vendedor@example.com");
        for (String preco : new String[]{"0", "-1", "1.999", "10000000000.00", "abc"}) {
            mvc.perform(valido(sessao).with(req -> { req.setParameter("preco", preco); return req; }))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("anuncio", "preco"));
        }
        mvc.perform(valido(sessao).with(req -> { req.setParameter("tipo", "INVALIDO"); req.setParameter("marca", " "); return req; }))
            .andExpect(model().attributeHasFieldErrors("anuncio", "tipo", "marca"));
        assertThat(anuncios.count()).isZero();
    }
    @Test void isolaDetalhesEListaPorDono() throws Exception {
        var dono = entrar("vendedor@example.com");
        var outraConta = entrar("outro@example.com");
        Long id = criar(dono);
        mvc.perform(get("/minha-conta/anuncios/" + id).session(outraConta)).andExpect(status().isNotFound());
        mvc.perform(get("/minha-conta/anuncios").session(outraConta)).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Aparelho de teste"))))
            .andExpect(content().string(not(containsString("123456789012345"))));
        mvc.perform(get("/minha-conta/anuncios/999999").session(dono)).andExpect(status().isNotFound());
    }
    @Test void criacaoExigeCsrf() throws Exception {
        mvc.perform(post("/minha-conta/anuncios/novo").session(entrar("vendedor@example.com")))
            .andExpect(status().isForbidden());
        assertThat(anuncios.count()).isZero();
    }
    @Test void aceitaImeisFormatadosEObservacoesVazias() throws Exception {
        var sessao = entrar("vendedor@example.com");
        mvc.perform(valido(sessao).with(req -> {
            req.setParameter("descricao", "");
            req.setParameter("imeis", "12345678-901234-5\n98765432-109876-5");
            return req;
        })).andExpect(status().is3xxRedirection());
        var detalhe = service.buscar(anuncios.findAll().get(0).getId(), vendedorId);
        assertThat(detalhe.imeis()).containsExactly("123456789012345", "987654321098765");
        assertThat(detalhe.descricao()).isEmpty();
        assertThat(detalhe.condicao()).isEqualTo("Com marcas de uso");
    }
    @Test void excluiSomenteRascunhoDoDonoComCsrf() throws Exception {
        var dono = entrar("vendedor@example.com");
        var outro = entrar("outro@example.com");
        Long id = criar(dono);
        Long aparelho = jdbc.queryForObject("SELECT aparelho_id FROM anuncios WHERE id = ?", Long.class, id);
        String url = "/minha-conta/anuncios/" + id + "/excluir";
        mvc.perform(get(url).session(dono)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Sim, excluir rascunho")));
        assertThat(anuncios.existsById(id)).isTrue();
        mvc.perform(get(url).session(outro)).andExpect(status().isNotFound());
        mvc.perform(post(url).session(outro).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(url).session(dono)).andExpect(status().isForbidden());
        assertThat(anuncios.existsById(id)).isTrue();
        mvc.perform(post(url).session(dono).with(csrf())).andExpect(redirectedUrl("/minha-conta/anuncios"));
        assertThat(anuncios.existsById(id)).isFalse();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM aparelhos WHERE id = ?", Long.class, aparelho)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM aparelho_imeis WHERE aparelho_id = ?", Long.class, aparelho)).isZero();
        assertThat(usuarios.existsById(vendedorId)).isTrue();
        mvc.perform(post(url).session(dono).with(csrf())).andExpect(status().isNotFound());
    }
    @Test void naoExcluiAnuncioPublicado() throws Exception {
        var sessao = entrar("vendedor@example.com");
        Long id = criar(sessao);
        jdbc.update("UPDATE anuncios SET status = 'PUBLICADO' WHERE id = ?", id);
        mvc.perform(post("/minha-conta/anuncios/" + id + "/excluir").session(sessao).with(csrf()))
            .andExpect(status().isConflict());
        assertThat(anuncios.existsById(id)).isTrue();
    }
    @Test void textoDoAnuncioEhEscapado() throws Exception {
        var sessao = entrar("vendedor@example.com");
        mvc.perform(valido(sessao).with(req -> { req.setParameter("titulo", "<script>alert(1)</script>"); return req; }))
            .andExpect(status().is3xxRedirection());
        Long id = anuncios.findAll().get(0).getId();
        mvc.perform(get("/minha-conta/anuncios/" + id).session(sessao)).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
            .andExpect(content().string(containsString("&lt;script&gt;")));
    }
}
