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
class PerfilTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    private static final String EMAIL = "perfil@example.com";
    private static final String SENHA = "minha frase segura antiga";
    private static final String NOVA = "minha nova frase de acesso";
    private Long id;
    private Long outroId;

    @BeforeEach
    void preparar() {
        usuarios.deleteAll();
        id = usuarios.saveAndFlush(new Usuario("Nome Original", EMAIL, encoder.encode(SENHA))).getId();
        outroId = usuarios.saveAndFlush(new Usuario("Outra Pessoa", "outro@example.com", encoder.encode(SENHA))).getId();
    }

    private MockHttpSession entrar() throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("email", EMAIL).param("senha", SENHA))
            .andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }

    @Test
    void paginasProtegidasEFormulariosCarregam() throws Exception {
        for (String url : new String[] {"/minha-conta/editar", "/minha-conta/senha"}) {
            mvc.perform(get(url)).andExpect(redirectedUrl("/login"));
            mvc.perform(get(url).session(entrar())).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString(SENHA))));
        }
    }

    @Test
    void alteraApenasNomeSemSenhaEIgnoraIdInjetado() throws Exception {
        var sessao = entrar();
        mvc.perform(post("/minha-conta/editar").session(sessao).with(csrf())
                .param("nome", "  Nome Novo  ").param("email", EMAIL).param("id", outroId.toString())
                .param("versaoCredenciais", "999").param("senhaHash", "adulterado"))
            .andExpect(redirectedUrl("/minha-conta"));
        assertThat(usuarios.findById(id).orElseThrow().getNome()).isEqualTo("Nome Novo");
        assertThat(usuarios.findById(outroId).orElseThrow().getNome()).isEqualTo("Outra Pessoa");
        assertThat(encoder.matches(SENHA, usuarios.findById(id).orElseThrow().getSenhaHash())).isTrue();
        mvc.perform(get("/minha-conta").session(sessao)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Nome Novo")));
    }

    @Test
    void emailExigeSenhaAtualENaoSalvaParcialmente() throws Exception {
        var sessao = entrar();
        for (String senha : new String[] {"", "senha incorreta"}) {
            mvc.perform(post("/minha-conta/editar").session(sessao).with(csrf())
                    .param("nome", "Mudanca rejeitada").param("email", "novo@example.com").param("senhaAtual", senha))
                .andExpect(model().attributeHasFieldErrors("perfil", "senhaAtual"));
        }
        assertThat(usuarios.findById(id).orElseThrow().getEmail()).isEqualTo(EMAIL);
        assertThat(usuarios.findById(id).orElseThrow().getNome()).isEqualTo("Nome Original");
    }

    @Test
    void rejeitaEmailDuplicadoEDadosInvalidos() throws Exception {
        var sessao = entrar();
        mvc.perform(post("/minha-conta/editar").session(sessao).with(csrf())
                .param("nome", "Mudanca rejeitada").param("email", "OUTRO@example.com").param("senhaAtual", SENHA))
            .andExpect(model().attributeHasFieldErrors("perfil", "email"))
            .andExpect(content().string(not(containsString(SENHA))));
        mvc.perform(post("/minha-conta/editar").session(sessao).with(csrf()).param("nome", " ").param("email", "invalido"))
            .andExpect(model().attributeHasFieldErrors("perfil", "nome", "email"));
        assertThat(usuarios.findById(id).orElseThrow().getNome()).isEqualTo("Nome Original");
    }

    @Test
    void trocaEmailMantemIdEncerraSessoesEUsaNovoLogin() throws Exception {
        var sessao = entrar();
        var outraSessao = entrar();
        mvc.perform(post("/minha-conta/editar").session(sessao).with(csrf())
                .param("nome", "Nome Novo").param("email", " NOVO@EXAMPLE.COM ").param("senhaAtual", SENHA))
            .andExpect(redirectedUrl("/login?emailAlterado"));
        assertThat(sessao.isInvalid()).isTrue();
        assertThat(usuarios.findByEmail("novo@example.com").orElseThrow().getId()).isEqualTo(id);
        // Mesmo se o email antigo for reutilizado, a sessao nao pode acessar a nova conta.
        usuarios.saveAndFlush(new Usuario("Conta diferente", EMAIL, encoder.encode(SENHA)));
        mvc.perform(get("/minha-conta").session(outraSessao)).andExpect(redirectedUrl("/login?atualizada"));
        assertThat(outraSessao.isInvalid()).isTrue();
        mvc.perform(post("/login").with(csrf()).param("email", "novo@example.com").param("senha", SENHA))
            .andExpect(authenticated().withUsername("novo@example.com"));
    }

    @Test
    void senhaIncorretaConfirmacaoDiferenteESenhaFracaNaoAlteramHash() throws Exception {
        var sessao = entrar();
        String original = usuarios.findById(id).orElseThrow().getSenhaHash();
        mvc.perform(post("/minha-conta/senha").session(sessao).with(csrf())
                .param("senhaAtual", "incorreta").param("novaSenha", NOVA).param("confirmacaoSenha", NOVA))
            .andExpect(model().attributeHasFieldErrors("senha", "senhaAtual"));
        mvc.perform(post("/minha-conta/senha").session(sessao).with(csrf())
                .param("senhaAtual", SENHA).param("novaSenha", NOVA).param("confirmacaoSenha", "outra frase diferente"))
            .andExpect(model().attributeHasFieldErrors("senha", "confirmacaoSenha"))
            .andExpect(content().string(not(containsString(SENHA))))
            .andExpect(content().string(not(containsString(NOVA))));
        mvc.perform(post("/minha-conta/senha").session(sessao).with(csrf())
                .param("senhaAtual", SENHA).param("novaSenha", "curta").param("confirmacaoSenha", "curta"))
            .andExpect(model().attributeHasFieldErrors("senha", "novaSenha"));
        mvc.perform(post("/minha-conta/senha").session(sessao).with(csrf())
                .param("senhaAtual", SENHA).param("novaSenha", SENHA).param("confirmacaoSenha", SENHA))
            .andExpect(model().attributeHasFieldErrors("senha", "novaSenha"));
        assertThat(usuarios.findById(id).orElseThrow().getSenhaHash()).isEqualTo(original);
    }

    @Test
    void trocaSenhaEncerraSessoesERejeitaSenhaAntiga() throws Exception {
        var sessao = entrar();
        var outraSessao = entrar();
        mvc.perform(post("/minha-conta/senha").session(sessao).with(csrf())
                .param("senhaAtual", SENHA).param("novaSenha", NOVA).param("confirmacaoSenha", NOVA))
            .andExpect(redirectedUrl("/login?senhaAlterada"));
        assertThat(sessao.isInvalid()).isTrue();
        String hash = usuarios.findById(id).orElseThrow().getSenhaHash();
        assertThat(hash).isNotEqualTo(NOVA);
        assertThat(encoder.matches(NOVA, hash)).isTrue();
        mvc.perform(get("/minha-conta").session(outraSessao)).andExpect(redirectedUrl("/login?atualizada"));
        mvc.perform(post("/login").with(csrf()).param("email", EMAIL).param("senha", SENHA))
            .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?erro"));
        mvc.perform(post("/login").with(csrf()).param("email", EMAIL).param("senha", NOVA))
            .andExpect(authenticated());
    }

    @Test
    void alteracoesExigemCsrf() throws Exception {
        var sessao = entrar();
        for (String url : new String[] {"/minha-conta/editar", "/minha-conta/senha"}) {
            mvc.perform(post(url).session(sessao)).andExpect(status().isForbidden());
        }
        assertThat(usuarios.findById(id).orElseThrow().getVersaoCredenciais()).isZero();
    }
}
