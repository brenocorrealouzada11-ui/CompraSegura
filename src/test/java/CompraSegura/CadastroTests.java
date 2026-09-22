package CompraSegura;

import CompraSegura.usuario.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CadastroTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    private static final String SENHA = "uma frase de teste segura";

    @BeforeEach
    void limpar() { usuarios.deleteAll(); }

    @Test
    void formularioPublicoTemCsrfESenhaVazia() throws Exception {
        mvc.perform(get("/cadastro")).andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(containsString("Criar minha conta")));
    }

    @Test
    void cadastraNormalizaEmailEProtegeSenha() throws Exception {
        mvc.perform(post("/cadastro").with(csrf()).param("nome", "  Breno Teste  ")
                .param("email", "  BRENO@example.com ").param("senha", SENHA))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cadastro/sucesso"))
            .andExpect(flash().attribute("cadastroConcluido", true));
        Usuario usuario = usuarios.findByEmail("breno@example.com").orElseThrow();
        assertThat(usuario.getNome()).isEqualTo("Breno Teste");
        assertThat(usuario.getSenhaHash()).isNotEqualTo(SENHA);
        assertThat(encoder.matches(SENHA, usuario.getSenhaHash())).isTrue();
    }

    @Test
    void rejeitaDuplicadoIgnorandoMaiusculasENaoDevolveSenha() throws Exception {
        mvc.perform(post("/cadastro").with(csrf()).param("nome", "Teste")
                .param("email", "breno@example.com").param("senha", SENHA)).andExpect(status().is3xxRedirection());
        mvc.perform(post("/cadastro").with(csrf()).param("nome", "Outro")
                .param("email", "BRENO@example.com").param("senha", SENHA))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("cadastro", "email"))
            .andExpect(content().string(not(containsString(SENHA))));
        assertThat(usuarios.count()).isEqualTo(1);
    }

    @Test
    void rejeitaDadosInvalidosSemGravar() throws Exception {
        mvc.perform(post("/cadastro").with(csrf()).param("nome", " ")
                .param("email", "invalido").param("senha", "curta"))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("cadastro", "nome", "email", "senha"))
            .andExpect(content().string(not(containsString("value=\"curta\""))));
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void recusaPostSemCsrf() throws Exception {
        mvc.perform(post("/cadastro").param("nome", "Teste")
                .param("email", "teste@example.com").param("senha", SENHA)).andExpect(status().isForbidden());
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void sucessoExigeCadastroConcluido() throws Exception {
        mvc.perform(get("/cadastro/sucesso")).andExpect(redirectedUrl("/cadastro"));
        mvc.perform(get("/cadastro/sucesso").flashAttr("cadastroConcluido", true))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Sua conta foi criada!")));
    }
}
