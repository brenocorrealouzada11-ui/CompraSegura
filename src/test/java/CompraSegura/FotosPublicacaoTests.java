package CompraSegura;
import CompraSegura.anuncio.*;
import CompraSegura.foto.*;
import CompraSegura.usuario.*;
import java.math.BigDecimal;
import java.util.*;
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
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class FotosPublicacaoTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AnuncioRepository anuncios;
    @Autowired FotosAnuncioService cadastro;
    @Autowired FotoAnuncioRepository fotos;
    @Autowired FotoPerfilRepository perfis;
    @Autowired FotosService imagens;
    @Autowired AnuncioService service;
    @Autowired PublicacaoService publicacao;
    @Autowired EdicaoRascunhoService edicao;
    @Autowired jakarta.persistence.EntityManager em;
    private UsuarioAutenticado dono, outro;
    @BeforeEach void preparar() {
        dono = new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario("Dono das fotos", UUID.randomUUID()+"@example.invalid", "hash")));
        outro = new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario("Outra pessoa", UUID.randomUUID()+"@example.invalid", "hash")));
    }
    private AnuncioForm form() {
        var f = new AnuncioForm(); f.setTitulo("Aparelho com foto"); f.setDescricao(""); f.setPreco(new BigDecimal("100"));
        f.setTipo(TipoAparelho.SMARTPHONE); f.setMarca("Marca"); f.setModelo("Modelo"); f.setCondicao(EstadoConservacao.BOM);
        f.setReparos(HistoricoReparos.SEM_REPAROS); f.setImeis("000000000000001"); return f;
    }
    private Long criar() { return cadastro.criar(dono, form(), null, List.of(TesteFotos.arquivo("fotos"))); }
    private long versao(Long id) { return anuncios.findById(id).orElseThrow().getVersao(); }
    private MockMultipartHttpServletRequestBuilder envio(String url, AnuncioForm f) {
        return multipart(url).with(user(dono)).with(csrf()).param("titulo", f.getTitulo()).param("descricao", f.getDescricao())
            .param("preco", f.getPreco().toString()).param("tipo", f.getTipo().name()).param("marca", f.getMarca()).param("modelo", f.getModelo())
            .param("condicao", f.getCondicao().name()).param("reparos", f.getReparos().name()).param("imeis", f.getImeis());
    }
    @Test void cadastroExigeDeUmaASeisFotosERejeitaFalsaImagemSemSalvar() throws Exception {
        long antes = anuncios.count();
        mvc.perform(envio("/minha-conta/anuncios/novo", form())).andExpect(status().isOk())
            .andExpect(content().string(containsString("pelo menos uma foto")));
        var sete = envio("/minha-conta/anuncios/novo", form());
        for (int i=0; i<7; i++) sete.file(TesteFotos.arquivo("fotos"));
        mvc.perform(sete).andExpect(status().isOk()).andExpect(content().string(containsString("no máximo 6 fotos")));
        mvc.perform(envio("/minha-conta/anuncios/novo", form()).file(new MockMultipartFile("fotos", "falsa.png", "image/png", new byte[]{1,2,3})))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("anuncio"));
        assertThat(anuncios.count()).isEqualTo(antes);
        mvc.perform(envio("/minha-conta/anuncios/novo", form()).file(TesteFotos.arquivo("fotos")))
            .andExpect(status().is3xxRedirection());
        assertThat(anuncios.count()).isEqualTo(antes+1);
    }
    @Test void fotosPrivadasAtePublicarESomenteDoAnuncioCorreto() throws Exception {
        Long id=criar(), outra=criar(), foto=fotos.ids(id).get(0);
        String publica="/anuncios/"+id+"/fotos/"+foto;
        mvc.perform(get(publica)).andExpect(status().isNotFound());
        mvc.perform(get("/minha-conta/anuncios/"+id+"/fotos/"+foto).with(user(outro))).andExpect(status().isNotFound());
        mvc.perform(get("/minha-conta/anuncios/"+id+"/fotos/"+foto).with(user(dono))).andExpect(status().isOk())
            .andExpect(content().contentType("image/png")).andExpect(header().string("X-Content-Type-Options", "nosniff"));
        publicacao.alterar(id,dono,versao(id),true);
        mvc.perform(get(publica)).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/anuncios/"+outra+"/fotos/"+foto)).andExpect(status().isNotFound());
        mvc.perform(get("/anuncios/"+id)).andExpect(status().isOk()).andExpect(content().string(containsString(publica)));
        publicacao.alterar(id,dono,versao(id),false);
        mvc.perform(get(publica)).andExpect(status().isNotFound());
    }
    @Test void edicaoMantemFotosExigeUmaERemoveApenasFotosDoProprioAnuncio() throws Exception {
        Long id=criar(), outra=criar();
        Long foto=fotos.ids(id).get(0), alheia=fotos.ids(outra).get(0);
        var f=edicao.carregar(id,dono);
        String url="/minha-conta/anuncios/"+id+"/editar";
        mvc.perform(envio(url,f).param("versao",f.getVersao().toString()).param("removerFotos",foto.toString()))
            .andExpect(status().isOk()).andExpect(content().string(containsString("pelo menos uma foto")));
        assertThat(fotos.ids(id)).containsExactly(foto);
        mvc.perform(envio(url,f).param("versao",f.getVersao().toString()).param("removerFotos",alheia.toString()))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("anuncio"));
        mvc.perform(envio(url,f).param("versao",f.getVersao().toString()).param("removerFotos",foto.toString()).file(TesteFotos.arquivo("fotos")))
            .andExpect(status().is3xxRedirection());
        assertThat(fotos.ids(id)).hasSize(1).doesNotContain(foto);
        assertThat(fotos.existsById(alheia)).isTrue();
    }
    @Test void perfilOpcionalSalvaExibeAoPublicarERemoveSemMudarCredenciais() throws Exception {
        long credenciais=usuarios.findById(dono.getId()).orElseThrow().getVersaoCredenciais();
        mvc.perform(get("/minha-conta/foto").with(user(dono))).andExpect(status().isOk());
        mvc.perform(multipart("/minha-conta/foto").file(TesteFotos.arquivo("arquivo")).with(user(dono)))
            .andExpect(status().isForbidden());
        mvc.perform(multipart("/minha-conta/foto").file(TesteFotos.arquivo("arquivo")).with(user(dono)).with(csrf()))
            .andExpect(flash().attributeExists("fotoSalva"));
        mvc.perform(get("/vendedores/"+dono.getId()+"/foto")).andExpect(status().isNotFound());
        Long id=criar(); publicacao.alterar(id,dono,versao(id),true);
        mvc.perform(get("/vendedores/"+dono.getId()+"/foto")).andExpect(status().isOk());
        mvc.perform(get("/anuncios/"+id)).andExpect(status().isOk())
            .andExpect(content().string(containsString("/vendedores/"+dono.getId()+"/foto")));
        mvc.perform(post("/minha-conta/foto").param("remover","true").with(user(dono)).with(csrf()))
            .andExpect(flash().attribute("fotoSalva","Foto de perfil removida."));
        assertThat(perfis.existsByUsuarioId(dono.getId())).isFalse();
        assertThat(usuarios.findById(dono.getId()).orElseThrow().getVersaoCredenciais()).isEqualTo(credenciais);
        mvc.perform(get("/anuncios/"+id)).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("/vendedores/"+dono.getId()+"/foto"))));
    }
    @Test void terceiraExclusaoPersisteBloqueioAvisosENaoImpedeExcluirOuEditar() throws Exception {
        for(int i=1;i<=3;i++) {
            Long id=criar(); String url="/minha-conta/anuncios/"+id;
            mvc.perform(post(url+"/publicar").param("versao",""+versao(id)).with(user(dono)).with(csrf()))
                .andExpect(flash().attributeExists("avisoPublicacao"));
            mvc.perform(get(url+"/excluir").with(user(dono))).andExpect(status().isOk())
                .andExpect(content().string(containsString("24 horas")));
            mvc.perform(post(url+"/excluir").param("versao",""+versao(id)).with(user(dono)).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("avisoExclusao"));
            assertThat(fotos.ids(id)).isEmpty();
            assertThat(anuncios.existsById(id)).isFalse();
        }
        em.flush(); em.clear();
        var usuario=usuarios.findById(dono.getId()).orElseThrow();
        assertThat(usuario.getExclusoesRapidas()).isEqualTo(3);
        assertThat(usuario.getBloqueadoAte()).isNotNull();
        Long id=criar(); String url="/minha-conta/anuncios/"+id;
        mvc.perform(post(url+"/publicar").param("versao",""+versao(id)).with(user(dono)).with(csrf()))
            .andExpect(flash().attribute("erroPublicacao",containsString("bloqueadas até")));
        assertThat(anuncios.findById(id).orElseThrow().getStatus()).isEqualTo("RASCUNHO");
        mvc.perform(get(url+"/editar").with(user(dono))).andExpect(status().isOk());
        mvc.perform(post(url+"/excluir").param("versao",""+versao(id)).with(user(dono)).with(csrf())).andExpect(status().is3xxRedirection());
    }
    @Test void retirarAntesDeExcluirNaoEvitaContagemEAnuncioAntigoReiniciaSequencia() {
        Long id=criar(); publicacao.alterar(id,dono,versao(id),true); publicacao.alterar(id,dono,versao(id),false);
        service.excluir(id,dono,versao(id));
        assertThat(usuarios.findById(dono.getId()).orElseThrow().getExclusoesRapidas()).isEqualTo(1);
        Long antigo=criar(); publicacao.alterar(antigo,dono,versao(antigo),true);
        anuncios.findById(antigo).orElseThrow().publicar(java.time.LocalDateTime.now().minusMinutes(6)); anuncios.flush();
        service.excluir(antigo,dono,versao(antigo));
        assertThat(usuarios.findById(dono.getId()).orElseThrow().getExclusoesRapidas()).isZero();
    }
    @Test void exclusaoPublicadaExigeDonoCsrfEVersaoAtual() throws Exception {
        Long id=criar(); publicacao.alterar(id,dono,versao(id),true);
        String url="/minha-conta/anuncios/"+id+"/excluir";
        mvc.perform(post(url).param("versao",""+versao(id)).with(user(outro)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(url).param("versao",""+versao(id)).with(user(dono))).andExpect(status().isForbidden());
        mvc.perform(post(url).param("versao","999").with(user(dono)).with(csrf())).andExpect(status().isConflict());
        assertThat(anuncios.existsById(id)).isTrue();
        assertThat(usuarios.findById(dono.getId()).orElseThrow().getExclusoesRapidas()).isZero();
    }
}
