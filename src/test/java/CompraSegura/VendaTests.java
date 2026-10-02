package CompraSegura;

import CompraSegura.anuncio.*;
import CompraSegura.catalogo.*;
import CompraSegura.chat.*;
import CompraSegura.foto.*;
import CompraSegura.usuario.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class VendaTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AnuncioRepository anuncios;
    @Autowired FotosAnuncioService cadastro;
    @Autowired FotoAnuncioRepository fotos;
    @Autowired FotosService imagens;
    @Autowired PublicacaoService publicacao;
    @Autowired VendaService vendas;
    @Autowired CatalogoService catalogo;
    @Autowired ChatService chat;
    @Autowired jakarta.persistence.EntityManager em;
    private UsuarioAutenticado dono, comprador;
    private Long id;
    private String url;
    @BeforeEach void preparar() {
        dono = usuario("Vendedor teste venda"); comprador = usuario("Comprador teste venda");
        id = criar("Aparelho venda teste", true); url = "/minha-conta/anuncios/" + id;
    }
    private UsuarioAutenticado usuario(String nome) { return new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario(nome, UUID.randomUUID()+"@example.invalid", "hash-privado"))); }
    private Long criar(String titulo, boolean publicar) {
        var f = new AnuncioForm(); f.setTitulo(titulo); f.setPreco(new BigDecimal("120")); f.setDescricao("");
        f.setTipo(TipoAparelho.SMARTPHONE); f.setMarca("Marca"); f.setModelo("Modelo"); f.setCondicao(EstadoConservacao.BOM);
        f.setReparos(HistoricoReparos.SEM_REPAROS); f.setImeis("000000000000001");
        Long novo = cadastro.criar(dono, f, null, List.of(TesteFotos.arquivo("fotos")));
        if (publicar) publicacao.alterar(novo, dono, anuncios.findById(novo).orElseThrow().getVersao(), true);
        return novo;
    }
    private long versao() { return anuncios.findById(id).orElseThrow().getVersao(); }
    private void vender() { vendas.concluir(id, dono, versao()); }

    @Test void confirmacaoExigeDonoSessaoCsrfEVersaoAtual() throws Exception {
        mvc.perform(get(url+"/vender").with(user(dono))).andExpect(status().isOk()).andExpect(content().string(containsString("Confirmar venda")));
        assertThat(anuncios.findById(id).orElseThrow().getStatus()).isEqualTo("PUBLICADO");
        mvc.perform(get(url+"/vender").with(user(comprador))).andExpect(status().isNotFound());
        mvc.perform(post(url+"/vender").param("versao", ""+versao()).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post(url+"/vender").param("versao", ""+versao()).with(user(dono))).andExpect(status().isForbidden());
        mvc.perform(post(url+"/vender").param("versao", ""+versao()).with(user(comprador)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(url+"/vender").with(user(dono)).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(post(url+"/vender").param("versao", "9999").with(user(dono)).with(csrf())).andExpect(flash().attributeExists("erroPublicacao"));
        assertThat(anuncios.findById(id).orElseThrow().getVendidoEm()).isNull();
        mvc.perform(post(url+"/vender").param("versao", ""+versao()).with(user(dono)).with(csrf())).andExpect(redirectedUrl(url)).andExpect(flash().attributeExists("sucessoPublicacao"));
        assertThat(anuncios.findById(id).orElseThrow().getStatus()).isEqualTo("VENDIDO");
    }
    @Test void vendidoSaiDaPesquisaMasMantemHistoricoFotosEPrivacidade() throws Exception {
        Long rascunho = criar("Rascunho segredo", false);
        imagens.perfil(dono, TesteFotos.arquivo("arquivo"), false);
        vender(); em.flush(); em.clear();
        assertThat(anuncios.findById(id).orElseThrow().getVendidoEm()).isNotNull();
        assertThat(catalogo.pesquisar(new FiltroPesquisa()).getTotalElements()).isZero();
        var historico = catalogo.vendedor(dono.getId(), 0);
        assertThat(historico.disponiveis()).isZero(); assertThat(historico.vendidos()).isEqualTo(1);
        assertThat(historico.anuncios().getContent()).extracting(AnuncioPublico::id).containsExactly(id);
        mvc.perform(get("/vendedores/"+dono.getId())).andExpect(status().isOk()).andExpect(content().string(containsString("Vendido"))).andExpect(content().string(not(containsString("Rascunho segredo"))));
        mvc.perform(get("/anuncios/"+id).with(user(comprador))).andExpect(status().isOk())
            .andExpect(content().string(containsString("Venda informada pelo vendedor")))
            .andExpect(content().string(not(containsString("Conversar com o vendedor"))))
            .andExpect(content().string(not(containsString("000000000000001"))))
            .andExpect(content().string(not(containsString(dono.getUsername()))));
        mvc.perform(get("/anuncios/"+rascunho)).andExpect(status().isNotFound());
        mvc.perform(get("/anuncios/"+id+"/fotos/"+fotos.ids(id).get(0))).andExpect(status().isOk());
        mvc.perform(get("/vendedores/"+dono.getId()+"/foto")).andExpect(status().isOk());
    }
    @Test void conclusaoRepetidaPreservaDataVersaoEContagem() {
        long anterior = versao(); vender();
        var data = anuncios.findById(id).orElseThrow().getVendidoEm(); long atual = versao();
        vendas.concluir(id, dono, anterior);
        assertThat(versao()).isEqualTo(atual);
        assertThat(anuncios.findById(id).orElseThrow().getVendidoEm()).isEqualTo(data);
        assertThat(catalogo.vendedor(dono.getId(),0).vendidos()).isEqualTo(1);
    }
    @Test void rascunhoNaoPodeSerVendido() throws Exception {
        publicacao.alterar(id,dono,versao(),false);
        mvc.perform(get(url+"/vender").with(user(dono))).andExpect(redirectedUrl(url));
        mvc.perform(post(url+"/vender").param("versao",""+versao()).with(user(dono)).with(csrf())).andExpect(status().isConflict());
        assertThat(anuncios.findById(id).orElseThrow().getVendidoEm()).isNull();
    }
    @Test void vendidoNaoPodeSerEditadoRepublicadoRetiradoExcluidoOuReavaliado() throws Exception {
        vender();
        mvc.perform(get(url).with(user(dono))).andExpect(status().isOk()).andExpect(content().string(containsString("Venda registrada")))
            .andExpect(content().string(not(containsString("Editar rascunho")))).andExpect(content().string(not(containsString("Excluir anúncio"))))
            .andExpect(content().string(not(containsString("Publicar anúncio")))).andExpect(content().string(not(containsString("Atualizar avaliação simulada"))));
        mvc.perform(get("/minha-conta/anuncios").with(user(dono))).andExpect(status().isOk()).andExpect(content().string(containsString("Vendido")));
        mvc.perform(get(url+"/editar").with(user(dono))).andExpect(status().isConflict());
        mvc.perform(get(url+"/excluir").with(user(dono))).andExpect(redirectedUrl(url));
        mvc.perform(get(url+"/publicar").with(user(dono))).andExpect(redirectedUrl(url));
        for (String acao : List.of("/editar", "/publicar", "/retirar", "/excluir", "/avaliar")) {
            mvc.perform(post(url+acao).param("versao",""+versao()).with(user(dono)).with(csrf())).andExpect(status().isConflict());
        }
        assertThat(anuncios.existsById(id)).isTrue();
    }
    @Test void conversasExistentesContinuamMasNovasSaoRecusadas() throws Exception {
        Long conversa = chat.iniciar(id, comprador);
        var form = new MensagemForm(); form.setTexto("Compra combinada"); chat.enviar(conversa,comprador,form);
        vender();
        var resposta = new MensagemForm(); resposta.setTexto("Venda concluída, obrigado!"); chat.enviar(conversa,dono,resposta);
        assertThat(chat.abrir(conversa,comprador,null).lote().mensagens()).hasSize(2);
        mvc.perform(post("/minha-conta/mensagens/anuncio/"+id).with(user(comprador)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/minha-conta/mensagens/"+conversa).with(user(comprador))).andExpect(status().isOk());
    }
    @Test void vendaNaoResetaNemAumentaContagemDeExclusoesOuBloqueio() {
        var usuario = usuarios.findById(dono.getId()).orElseThrow();
        var agora = LocalDateTime.now().withNano(0);
        for (int i=0;i<3;i++) usuario.registrarExclusao(agora,agora);
        usuarios.flush(); var bloqueio = usuario.getBloqueadoAte();
        vender();
        assertThat(usuario.getExclusoesRapidas()).isEqualTo(3); assertThat(usuario.getBloqueadoAte()).isEqualTo(bloqueio);
    }
}
