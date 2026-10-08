package CompraSegura;

import CompraSegura.anuncio.*;
import CompraSegura.chat.*;
import CompraSegura.favorito.*;
import CompraSegura.foto.*;
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
class FavoritosNegociacoesTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AnuncioRepository anuncios;
    @Autowired FotosAnuncioService cadastro;
    @Autowired PublicacaoService publicacao;
    @Autowired VendaService vendas;
    @Autowired AnuncioService gerenciar;
    @Autowired FavoritoService favoritos;
    @Autowired FavoritoRepository registros;
    @Autowired ChatService chat;
    @Autowired NegociacaoService negociacoes;
    @Autowired jakarta.persistence.EntityManager em;
    private UsuarioAutenticado vendedor, comprador, terceiro;
    private Long anuncio;
    @BeforeEach void preparar() {
        vendedor=usuario("Vendedor Favoritos"); comprador=usuario("Comprador Favoritos"); terceiro=usuario("Terceiro");
        anuncio=criar(vendedor,"Celular <script>alert(1)</script>",true);
    }
    private UsuarioAutenticado usuario(String nome) { return new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario(nome,UUID.randomUUID()+"@example.invalid","hash-teste"))); }
    private Long criar(UsuarioAutenticado dono,String titulo,boolean publicar) {
        var f=new AnuncioForm(); f.setTitulo(titulo); f.setPreco(new BigDecimal("100")); f.setTipo(TipoAparelho.SMARTPHONE);
        f.setMarca("Marca"); f.setModelo("Modelo"); f.setCondicao(EstadoConservacao.BOM); f.setReparos(HistoricoReparos.SEM_REPAROS); f.setImeis("000000000000001");
        Long id=cadastro.criar(dono,f,null,List.of(TesteFotos.arquivo("fotos")));
        if(publicar) publicacao.alterar(id,dono,versao(id),true);
        return id;
    }
    private long versao(Long id) { return anuncios.findById(id).orElseThrow().getVersao(); }
    private void mensagem(Long id,UsuarioAutenticado autor,String texto) { var f=new MensagemForm(); f.setTexto(texto); chat.enviar(id,autor,f); }
    private org.springframework.data.domain.Page<NegociacaoService.Item> lista(UsuarioAutenticado pessoa,NegociacaoService.Papel papel,NegociacaoService.Situacao situacao) { return negociacoes.listar(pessoa,papel,situacao,0); }

    @Test void favoritosExigemSessaoCsrfPublicacaoESaoUnicosPorUsuario() throws Exception {
        String acao="/minha-conta/favoritos/anuncio/"+anuncio;
        mvc.perform(get("/minha-conta/favoritos")).andExpect(status().is3xxRedirection());
        mvc.perform(post(acao).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post(acao).with(user(comprador))).andExpect(status().isForbidden());
        mvc.perform(post(acao).with(user(vendedor)).with(csrf())).andExpect(status().isConflict());
        Long privado=criar(vendedor,"Segredo",false);
        mvc.perform(post("/minha-conta/favoritos/anuncio/"+privado).with(user(comprador)).with(csrf())).andExpect(status().isNotFound());
        for(int i=0;i<2;i++) mvc.perform(post(acao).with(user(comprador)).with(csrf())).andExpect(redirectedUrl("/anuncios/"+anuncio));
        assertThat(registros.count()).isEqualTo(1);
        favoritos.adicionar(anuncio,terceiro); assertThat(registros.count()).isEqualTo(2);
        mvc.perform(get("/anuncios/"+anuncio).with(user(comprador))).andExpect(content().string(containsString("Nos seus favoritos")));
    }
    @Test void favoritosSaoPrivadosERemocaoExigeProprietario() throws Exception {
        favoritos.adicionar(anuncio,comprador);
        Long id=favoritos.listar(comprador,0).getContent().get(0).id();
        assertThat(favoritos.listar(terceiro,0).getContent()).isEmpty();
        mvc.perform(get("/minha-conta/favoritos").with(user(comprador))).andExpect(status().isOk()).andExpect(content().string(containsString("&lt;script&gt;"))).andExpect(content().string(not(containsString("<script>"))));
        mvc.perform(post("/minha-conta/favoritos/"+id+"/remover").with(user(terceiro)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/minha-conta/favoritos/"+id+"/remover").with(user(comprador))).andExpect(status().isForbidden());
        mvc.perform(post("/minha-conta/favoritos/"+id+"/remover").with(user(comprador)).with(csrf())).andExpect(redirectedUrl("/minha-conta/favoritos"));
        assertThat(registros.count()).isZero();
    }
    @Test void favoritoRetiradoNaoExpoeEdicoesPrivadasESobreviveAExclusao() throws Exception {
        favoritos.adicionar(anuncio,comprador);
        publicacao.alterar(anuncio,vendedor,versao(anuncio),false);
        anuncios.findById(anuncio).orElseThrow().atualizar("Titulo privado novo","Segredo privado",new BigDecimal("999")); anuncios.flush();
        var item=favoritos.listar(comprador,0).getContent().get(0);
        assertThat(item.situacao()).isEqualTo("Indisponível"); assertThat(item.anuncioId()).isNull(); assertThat(item.preco()).isNull(); assertThat(item.foto()).isNull();
        mvc.perform(get("/minha-conta/favoritos").with(user(comprador))).andExpect(status().isOk()).andExpect(content().string(not(containsString("Titulo privado novo"))));
        em.flush(); em.clear(); gerenciar.excluir(anuncio,vendedor,versao(anuncio)); em.flush(); em.clear();
        assertThat(favoritos.listar(comprador,0).getTotalElements()).isEqualTo(1);
        favoritos.remover(item.id(),comprador); assertThat(registros.count()).isZero();
    }
    @Test void vendidoPermaneceNosFavoritosMasNaoAceitaNovoFavorito() throws Exception {
        favoritos.adicionar(anuncio,comprador); vendas.concluir(anuncio,vendedor,versao(anuncio));
        var item=favoritos.listar(comprador,0).getContent().get(0);
        assertThat(item.situacao()).isEqualTo("Vendido"); assertThat(item.anuncioId()).isEqualTo(anuncio);
        mvc.perform(get("/minha-conta/favoritos").with(user(comprador))).andExpect(status().isOk()).andExpect(content().string(containsString("Vendido")));
        mvc.perform(post("/minha-conta/favoritos/anuncio/"+anuncio).with(user(terceiro)).with(csrf())).andExpect(status().isNotFound());
    }
    @Test void negociacoesLimitamParticipantesFiltramPapelENaoMarcamLeitura() throws Exception {
        Long conversa=chat.iniciar(anuncio,comprador); Long outra=chat.iniciar(anuncio,terceiro);
        mensagem(conversa,vendedor,"Mensagem exclusiva comprador"); mensagem(outra,terceiro,"Mensagem exclusiva terceiro");
        assertThat(lista(comprador,NegociacaoService.Papel.COMPRANDO,NegociacaoService.Situacao.TODAS).getContent()).extracting(NegociacaoService.Item::conversaId).containsExactly(conversa);
        assertThat(lista(comprador,NegociacaoService.Papel.VENDENDO,NegociacaoService.Situacao.TODAS).getContent()).isEmpty();
        assertThat(lista(vendedor,NegociacaoService.Papel.VENDENDO,NegociacaoService.Situacao.TODAS).getTotalElements()).isEqualTo(2);
        mvc.perform(get("/minha-conta/negociacoes")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/minha-conta/negociacoes").with(user(comprador))).andExpect(status().isOk()).andExpect(content().string(containsString("Mensagem exclusiva comprador")))
            .andExpect(content().string(not(containsString("Mensagem exclusiva terceiro")))).andExpect(content().string(not(containsString(vendedor.getUsername()))));
        assertThat(chat.naoLidas(comprador)).isEqualTo(1);
        mvc.perform(get("/minha-conta/negociacoes").param("papel","INVALIDO").with(user(comprador))).andExpect(status().isBadRequest());
    }
    @Test void negociacoesAcompanhamVendaRetiradaEExclusaoSemVazarRascunho() throws Exception {
        chat.iniciar(anuncio,comprador);
        Long outro=criar(vendedor,"Outro aparelho publico",true); chat.iniciar(outro,comprador);
        vendas.concluir(anuncio,vendedor,versao(anuncio));
        assertThat(lista(comprador,NegociacaoService.Papel.TODOS,NegociacaoService.Situacao.ANUNCIO_VENDIDO).getContent()).extracting(NegociacaoService.Item::anuncioId).containsExactly(anuncio);
        publicacao.alterar(outro,vendedor,versao(outro),false);
        anuncios.findById(outro).orElseThrow().atualizar("Segredo novo titulo","Segredo",BigDecimal.TEN); anuncios.flush();
        mvc.perform(get("/minha-conta/negociacoes").param("situacao","INDISPONIVEL").with(user(comprador))).andExpect(status().isOk())
            .andExpect(content().string(containsString("Outro aparelho publico"))).andExpect(content().string(not(containsString("Segredo novo titulo"))));
        em.flush(); em.clear(); gerenciar.excluir(outro,vendedor,versao(outro)); em.flush(); em.clear();
        var indisponiveis=lista(comprador,NegociacaoService.Papel.TODOS,NegociacaoService.Situacao.INDISPONIVEL);
        assertThat(indisponiveis.getTotalElements()).isEqualTo(1); assertThat(indisponiveis.getContent().get(0).anuncioId()).isNull();
        assertThat(lista(comprador,NegociacaoService.Papel.TODOS,NegociacaoService.Situacao.EM_ANDAMENTO).getContent()).isEmpty();
    }
    @Test void paginacaoLimitaFavoritosENegociacoesEPreservaFiltros() throws Exception {
        for(int i=0;i<13;i++) { Long id=criar(vendedor,"Celular pagina "+i,true); favoritos.adicionar(id,comprador); chat.iniciar(id,comprador); }
        assertThat(favoritos.listar(comprador,0).getContent()).hasSize(12); assertThat(favoritos.listar(comprador,1).getContent()).hasSize(1);
        mvc.perform(get("/minha-conta/favoritos").with(user(comprador))).andExpect(status().isOk()).andExpect(content().string(containsString("Página 1 de 2")));
        mvc.perform(get("/minha-conta/negociacoes").param("papel","COMPRANDO").param("situacao","EM_ANDAMENTO").with(user(comprador))).andExpect(status().isOk())
            .andExpect(content().string(containsString("Página 1 de 2"))).andExpect(content().string(containsString("papel=COMPRANDO"))).andExpect(content().string(containsString("situacao=EM_ANDAMENTO")));
    }
}
