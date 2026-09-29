package CompraSegura;

import CompraSegura.anuncio.*;
import CompraSegura.chat.*;
import CompraSegura.foto.FotosAnuncioService;
import CompraSegura.usuario.*;
import java.math.BigDecimal;
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
class ChatTests {
    @Autowired MockMvc mvc;
    @Autowired ChatService chat;
    @Autowired ConversaRepository conversas;
    @Autowired MensagemRepository mensagens;
    @Autowired FotosAnuncioService cadastro;
    @Autowired AnuncioRepository anuncios;
    @Autowired AnuncioService gerenciar;
    @Autowired PublicacaoService publicacao;
    @Autowired UsuarioRepository usuarios;
    @Autowired jakarta.persistence.EntityManager em;
    private UsuarioAutenticado vendedor, comprador, terceiro;
    private Long anuncio;
    @BeforeEach void preparar() {
        vendedor = usuario("Vendedor do chat"); comprador = usuario("Comprador do chat"); terceiro = usuario("Terceira pessoa");
        var f = new AnuncioForm(); f.setTitulo("Aparelho para conversar"); f.setDescricao(""); f.setPreco(new BigDecimal("120"));
        f.setTipo(TipoAparelho.SMARTPHONE); f.setMarca("Marca"); f.setModelo("Modelo"); f.setCondicao(EstadoConservacao.BOM);
        f.setReparos(HistoricoReparos.SEM_REPAROS); f.setImeis("000000000000001");
        anuncio = cadastro.criar(vendedor, f, null, List.of(TesteFotos.arquivo("fotos")));
        publicacao.alterar(anuncio, vendedor, versao(), true);
    }
    private UsuarioAutenticado usuario(String nome) { return new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario(nome, UUID.randomUUID()+"@example.invalid", "hash-privado"))); }
    private long versao() { return anuncios.findById(anuncio).orElseThrow().getVersao(); }
    private MensagemForm mensagem(String texto) { var f = new MensagemForm(); f.setTexto(texto); return f; }
    @Test void iniciarExigeSessaoCsrfAnuncioPublicadoENaoPermiteAutoconversa() throws Exception {
        String url = "/minha-conta/mensagens/anuncio/"+anuncio;
        mvc.perform(post(url).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post(url).with(user(comprador))).andExpect(status().isForbidden());
        mvc.perform(post(url).with(user(vendedor)).with(csrf())).andExpect(status().isConflict());
        mvc.perform(get("/anuncios/"+anuncio).with(user(vendedor))).andExpect(content().string(not(containsString("Conversar com o vendedor"))));
        mvc.perform(get("/anuncios/"+anuncio).with(user(comprador))).andExpect(content().string(containsString("Conversar com o vendedor")));
        publicacao.alterar(anuncio,vendedor,versao(),false);
        mvc.perform(post(url).with(user(comprador)).with(csrf())).andExpect(status().isNotFound());
        assertThat(conversas.count()).isZero();
    }
    @Test void abrirMesmoAnuncioReutilizaConversaECadaCompradorTemConversaSeparada() throws Exception {
        Long primeira=chat.iniciar(anuncio,comprador);
        assertThat(chat.iniciar(anuncio,comprador)).isEqualTo(primeira);
        Long outra=chat.iniciar(anuncio,terceiro);
        assertThat(outra).isNotEqualTo(primeira);
        assertThat(chat.listar(comprador,0).getTotalElements()).isEqualTo(1);
        assertThat(chat.listar(vendedor,0).getTotalElements()).isEqualTo(2);
        mvc.perform(post("/minha-conta/mensagens/anuncio/"+anuncio).with(user(comprador)).with(csrf()))
            .andExpect(redirectedUrl("/minha-conta/mensagens/"+primeira));
        mvc.perform(get("/minha-conta/mensagens").with(user(vendedor))).andExpect(status().isOk()).andExpect(content().string(containsString("Comprador do chat")));
    }
    @Test void somenteParticipantesPodemLerEnviarOuMarcarLida() throws Exception {
        Long id=chat.iniciar(anuncio,comprador);
        var msg=chat.enviar(id,comprador,mensagem("Mensagem privada"));
        for(String sufixo:List.of("","/novas","/historico?antes="+msg.id())) {
            mvc.perform(get("/minha-conta/mensagens/"+id+sufixo).with(user(terceiro))).andExpect(status().isNotFound());
        }
        mvc.perform(post("/minha-conta/mensagens/"+id+"/enviar").param("texto"," ").with(user(terceiro)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/minha-conta/mensagens/"+id+"/lida").param("ate",msg.id().toString()).with(user(terceiro)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/minha-conta/mensagens/"+id+"/novas")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/minha-conta/mensagens/lista").with(user(terceiro))).andExpect(content().string(not(containsString("Mensagem privada"))));
        assertThat(chat.naoLidas(terceiro)).isZero();
    }
    @Test void naoLidasContamSomenteRecebidasEGetNaoMarcaComoLida() throws Exception {
        Long id=chat.iniciar(anuncio,comprador);
        var primeira=chat.enviar(id,comprador,mensagem("Primeira"));
        var segunda=chat.enviar(id,comprador,mensagem("Segunda"));
        assertThat(chat.naoLidas(vendedor)).isEqualTo(2); assertThat(chat.naoLidas(comprador)).isZero();
        mvc.perform(get("/minha-conta/mensagens/"+id).with(user(vendedor))).andExpect(status().isOk());
        mvc.perform(get("/minha-conta/mensagens/"+id+"/novas").with(user(vendedor))).andExpect(jsonPath("$.mensagens.length()").value(2));
        assertThat(chat.naoLidas(vendedor)).isEqualTo(2);
        mvc.perform(post("/minha-conta/mensagens/"+id+"/lida").param("ate",primeira.id().toString()).with(user(vendedor))).andExpect(status().isForbidden());
        mvc.perform(post("/minha-conta/mensagens/"+id+"/lida").param("ate",primeira.id().toString()).with(user(vendedor)).with(csrf())).andExpect(status().isNoContent());
        assertThat(chat.naoLidas(vendedor)).isEqualTo(1);
        chat.marcarLida(id,vendedor,segunda.id()); chat.marcarLida(id,vendedor,primeira.id());
        assertThat(chat.naoLidas(vendedor)).isZero();
        chat.enviar(id,vendedor,mensagem("Resposta"));
        assertThat(chat.naoLidas(comprador)).isEqualTo(1);
    }
    @Test void cursorDeLeituraNaoAceitaIdDeOutraConversaNemFuturo() throws Exception {
        Long id=chat.iniciar(anuncio,comprador), outro=chat.iniciar(anuncio,terceiro);
        var m=chat.enviar(outro,terceiro,mensagem("Outra conversa"));
        for(String cursor:List.of(m.id().toString(),"999999999","-1")) {
            mvc.perform(post("/minha-conta/mensagens/"+id+"/lida").param("ate",cursor).with(user(vendedor)).with(csrf())).andExpect(status().isBadRequest());
        }
        assertThat(chat.naoLidas(vendedor)).isEqualTo(1);
    }
    @Test void rejeitaMensagemVaziaLongaEOuSemCsrf() throws Exception {
        Long id=chat.iniciar(anuncio,comprador);
        String url="/minha-conta/mensagens/"+id+"/enviar";
        mvc.perform(post(url).param("texto","Oi").with(user(comprador))).andExpect(status().isForbidden());
        for(String texto:List.of(" \n\t", "x".repeat(2001))) {
            mvc.perform(post(url).param("texto",texto).param("chave",UUID.randomUUID().toString()).with(user(comprador)).with(csrf())).andExpect(status().isUnprocessableEntity());
        }
        assertThat(mensagens.count()).isZero();
    }
    @Test void repeticaoDeEnvioNaoDuplicaEMetadadosInjetadosNaoMudamRemetente() throws Exception {
        Long id=chat.iniciar(anuncio,comprador); String chave=UUID.randomUUID().toString();
        String url="/minha-conta/mensagens/"+id+"/enviar";
        for(int i=0;i<2;i++) mvc.perform(post(url).param("texto","  Olá vendedor!  ").param("chave",chave)
                .param("remetenteId",vendedor.getId().toString()).param("conversaId","999999").with(user(comprador)).with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.mensagem.texto").value("Olá vendedor!"));
        assertThat(mensagens.count()).isEqualTo(1);
        assertThat(chat.naoLidas(vendedor)).isEqualTo(1);
        mvc.perform(post(url).param("texto","Texto diferente").param("chave",chave).with(user(comprador)).with(csrf())).andExpect(status().isConflict());
    }
    @Test void textoEscapadoEDadosPrivadosNaoSaoSerializados() throws Exception {
        Long id=chat.iniciar(anuncio,comprador);
        chat.enviar(id,comprador,mensagem("<script>alert(1)</script>\nOlá!"));
        mvc.perform(get("/minha-conta/mensagens/"+id).with(user(vendedor))).andExpect(status().isOk())
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
            .andExpect(content().string(not(containsString(comprador.getUsername()))));
        mvc.perform(get("/minha-conta/mensagens/"+id+"/novas").with(user(vendedor))).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.mensagens[0].propria").value(false)).andExpect(content().string(not(containsString("hash-privado"))))
            .andExpect(content().string(not(containsString(comprador.getUsername()))));
    }
    @Test void historicoPaginadoEConsultaIncrementalNaoPerdemMensagens() {
        Long id=chat.iniciar(anuncio,comprador);
        for(int i=0;i<53;i++) chat.enviar(id,comprador,mensagem("Mensagem "+i));
        var atual=chat.abrir(id,vendedor,null).lote();
        assertThat(atual.mensagens()).hasSize(50); assertThat(atual.temMais()).isTrue();
        assertThat(atual.mensagens().get(0).texto()).isEqualTo("Mensagem 3");
        var antigas=chat.anteriores(id,vendedor,atual.getPrimeiraId());
        assertThat(antigas.mensagens()).hasSize(3); assertThat(antigas.temMais()).isFalse();
        var novas=chat.novas(id,vendedor,0);
        assertThat(novas.mensagens()).hasSize(50); assertThat(novas.temMais()).isTrue();
        assertThat(chat.novas(id,vendedor,novas.getUltimaId()).mensagens()).hasSize(3);
    }
    @Test void conversaPermaneceAposRetiradaOuExclusaoSemExporDadosDoRascunho() throws Exception {
        Long id=chat.iniciar(anuncio,comprador);
        chat.enviar(id,comprador,mensagem("Quero negociar"));
        publicacao.alterar(anuncio,vendedor,versao(),false);
        anuncios.findById(anuncio).orElseThrow().atualizar("Titulo privado alterado", "Texto privado", new BigDecimal("500")); anuncios.flush();
        mvc.perform(get("/minha-conta/mensagens/"+id).with(user(comprador))).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Titulo privado alterado"))));
        // A exclusão ocorre em outra requisição, sem entidades da leitura anterior no contexto.
        em.flush(); em.clear();
        gerenciar.excluir(anuncio,vendedor,versao()); em.flush(); em.clear();
        var conversa=chat.abrir(id,comprador,null);
        assertThat(conversa.anuncioId()).isNull(); assertThat(conversa.lote().mensagens()).hasSize(1);
        chat.enviar(id,vendedor,mensagem("O anúncio foi removido."));
        assertThat(chat.novas(id,comprador,0).mensagens()).hasSize(2);
        mvc.perform(get("/minha-conta/mensagens/"+id).with(user(comprador))).andExpect(status().isOk()).andExpect(content().string(containsString("não está disponível na pesquisa")));
    }
    @Test void formularioFuncionaSemJavascript() throws Exception {
        Long id=chat.iniciar(anuncio,comprador);
        String url="/minha-conta/mensagens/"+id;
        mvc.perform(post(url).param("texto","Oi, ainda está disponível?").param("chave",UUID.randomUUID().toString()).with(user(comprador)).with(csrf()))
            .andExpect(redirectedUrl(url));
        mvc.perform(post(url).param("texto"," ").with(user(comprador)).with(csrf())).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("mensagem","texto"));
        mvc.perform(post(url+"/marcar-lida").param("ate",chat.novas(id,vendedor,0).getUltimaId()+"").with(user(vendedor)).with(csrf())).andExpect(redirectedUrl(url));
        assertThat(chat.naoLidas(vendedor)).isZero();
    }
}
