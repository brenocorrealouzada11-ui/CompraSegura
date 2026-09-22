package CompraSegura;

import CompraSegura.usuario.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    private static final String EMAIL = "login@example.com";
    private static final String SENHA = "uma frase segura para teste";

    @BeforeEach
    void preparar() {
        usuarios.deleteAll();
        usuarios.saveAndFlush(new Usuario("Pessoa Teste", EMAIL, encoder.encode(SENHA)));
    }

    @Test
    void paginaPublicaEContaProtegida() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/minha-conta")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void autenticaNormalizaEmailMantemSessaoEExibeSomentePropriosDados() throws Exception {
        usuarios.saveAndFlush(new Usuario("Outra Pessoa", "outro@example.com", encoder.encode(SENHA)));
        var resultado = mvc.perform(post("/login").with(csrf()).param("email", " LOGIN@EXAMPLE.COM ").param("senha", SENHA))
            .andExpect(authenticated().withUsername(EMAIL)).andExpect(redirectedUrl("/minha-conta")).andReturn();
        var sessao = (MockHttpSession) resultado.getRequest().getSession(false);
        mvc.perform(get("/minha-conta").session(sessao).param("email", "outro@example.com"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Pessoa Teste")))
            .andExpect(content().string(not(containsString("Outra Pessoa"))))
            .andExpect(content().string(not(containsString("pbkdf2"))))
            .andExpect(header().string("Cache-Control", containsString("no-store")));
        mvc.perform(get("/login").session(sessao)).andExpect(redirectedUrl("/minha-conta"));
    }

    @Test
    void rejeitaSenhaIncorretaEEmailInexistenteComMesmoErro() throws Exception {
        for (String email : new String[] {EMAIL, "ausente@example.com"}) {
            mvc.perform(post("/login").with(csrf()).param("email", email).param("senha", "incorreta"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?erro"));
        }
        mvc.perform(get("/login?erro")).andExpect(content().string(containsString("E-mail ou senha incorretos.")));
    }

    @Test
    void logoutInvalidaSessaoERequerCsrf() throws Exception {
        var resultado = mvc.perform(post("/login").with(csrf()).param("email", EMAIL).param("senha", SENHA)).andReturn();
        var sessao = (MockHttpSession) resultado.getRequest().getSession(false);
        mvc.perform(post("/sair").session(sessao)).andExpect(status().isForbidden());
        mvc.perform(get("/minha-conta").session(sessao)).andExpect(status().isOk());
        mvc.perform(post("/sair").session(sessao).with(csrf()))
            .andExpect(redirectedUrl("/login?saiu")).andExpect(unauthenticated());
        assertThat(sessao.isInvalid()).isTrue();
        mvc.perform(get("/minha-conta")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginSemCsrfNaoAutentica() throws Exception {
        mvc.perform(post("/login").param("email", EMAIL).param("senha", SENHA))
            .andExpect(status().isForbidden()).andExpect(unauthenticated());
    }
}
