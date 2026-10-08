package CompraSegura;

import CompraSegura.anuncio.*;
import CompraSegura.catalogo.*;
import CompraSegura.chat.*;
import CompraSegura.foto.*;
import CompraSegura.reputacao.*;
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
class PerfilVendedorTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AnuncioRepository anuncios;
    @Autowired FotosAnuncioService cadastro;
    @Autowired FotosService fotos;
    @Autowired PublicacaoService publicacao;
    @Autowired AnuncioService gerenciar;
    @Autowired ChatService chat;
    @Autowired PesquisaVendedorService pesquisa;
    @Autowired AvaliacaoPerfilService avaliacoes;
    @Autowired AvaliacaoVendedorRepository registros;
    @Autowired jakarta.persistence.EntityManager em;
    private UsuarioAutenticado vendedor, comprador, outro;
    private Long anuncio;
    private String perfil, acao;
    @BeforeEach void preparar() {
        vendedor=usuario("NomeExclusivo Vendedor"); comprador=usuario("Comprador <script>alert(1)</script>"); outro=usuario("Pessoa sem conversa");
        anuncio=criar(vendedor,"Celular Palavrachave",true);
        perfil="/vendedores/"+vendedor.getId(); acao="/minha-conta/vendedores/"+vendedor.getId()+"/avaliacao";
    }
    private UsuarioAutenticado usuario(String nome) { return new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario(nome,UUID.randomUUID()+"@example.invalid","hash-secreto"))); }
    private Long criar(UsuarioAutenticado dono,String titulo,boolean publicar) {
        var f=new AnuncioForm(); f.setTitulo(titulo); f.setPreco(new BigDecimal("100")); f.setTipo(TipoAparelho.SMARTPHONE);
        f.setMarca("MarcaSecreta"); f.setModelo("ModeloSecreto"); f.setCondicao(EstadoConservacao.BOM); f.setReparos(HistoricoReparos.SEM_REPAROS); f.setImeis("000000000000001");
        Long id=cadastro.criar(dono,f,null,List.of(TesteFotos.arquivo("fotos")));
        if(publicar) publicacao.alterar(id,dono,anuncios.findById(id).orElseThrow().getVersao(),true);
        return id;
    }
    private void mensagem(Long id,UsuarioAutenticado autor,String texto) { var f=new MensagemForm(); f.setTexto(texto); chat.enviar(id,autor,f); }
    private void conversar(UsuarioAutenticado autor) { Long id=chat.iniciar(anuncio,autor); mensagem(id,autor,"Mensagem privada comprador"); mensagem(id,vendedor,"Resposta privada vendedor"); }
    private AvaliacaoPerfilForm form(int nota,String texto) { var f=new AvaliacaoPerfilForm(); f.setNota(nota); f.setComentario(texto); return f; }
    private List<Long> buscar(String nome) { var f=new FiltroVendedor(); f.setNome(nome); return pesquisa.pesquisar(f).getContent().stream().map(PesquisaVendedorService.Vendedor::id).toList(); }

    @Test void buscaExclusivaPorNomeSemEmailsTitulosMarcasOuContasPrivadas() throws Exception {
        criar(vendedor,"Segundo anuncio mesmo vendedor",true); criar(outro,"Rascunho privado",false);
        assertThat(buscar("nomeEXCLUSIVO")).containsExactly(vendedor.getId());
        for(String nome:List.of("Palavrachave","MarcaSecreta","ModeloSecreto",vendedor.getUsername(),"Pessoa sem conversa")) assertThat(buscar(nome)).isEmpty();
        mvc.perform(get("/anuncios")).andExpect(status().isOk()).andExpect(content().string(containsString("Vendedores")));
        mvc.perform(get("/vendedores").param("nome","NomeExclusivo")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Ver perfil e avaliações"))).andExpect(content().string(not(containsString(vendedor.getUsername()))));
    }
    @Test void buscaEscapaCuringasMantemNomeNaPaginacaoEValidaEntrada() throws Exception {
        var especial=usuario("Loja 10%_! especial"); criar(especial,"Aparelho especial",true);
        assertThat(buscar("%_!")).containsExactly(especial.getId());
        for(int i=0;i<13;i++) criar(usuario("Paginado "+i),"Aparelho pagina",true);
        mvc.perform(get("/vendedores").param("nome","Paginado")).andExpect(status().isOk()).andExpect(content().string(containsString("Página 1 de 2"))).andExpect(content().string(containsString("nome=Paginado")));
        var f=new FiltroVendedor(); f.setNome("Paginado"); f.setPagina(1); assertThat(pesquisa.pesquisar(f).getContent()).hasSize(1);
        for(String[] invalido:new String[][]{{"nome","x".repeat(101)},{"pagina","-1"},{"pagina","abc"}})
            mvc.perform(get("/vendedores").param(invalido[0],invalido[1])).andExpect(status().isOk()).andExpect(model().attributeHasErrors("filtroVendedor"));
    }
    @Test void soConversaComMensagensDosDoisLadosHabilitaAvaliacao() throws Exception {
        mvc.perform(post(acao).param("nota","5").with(user(comprador)).with(csrf())).andExpect(status().isForbidden());
        Long id=chat.iniciar(anuncio,comprador);
        mvc.perform(post(acao).param("nota","5").with(user(comprador)).with(csrf())).andExpect(status().isForbidden());
        mensagem(id,comprador,"Olá");
        mvc.perform(post(acao).param("nota","5").with(user(comprador)).with(csrf())).andExpect(status().isForbidden());
        mensagem(id,vendedor,"Oi");
        mvc.perform(get(perfil).with(user(comprador))).andExpect(status().isOk()).andExpect(content().string(containsString("Publicar avaliação")));
        mvc.perform(post(acao).param("nota","5").with(user(comprador)).with(csrf())).andExpect(redirectedUrl(perfil+"#avaliacoes"));
        assertThat(registros.count()).isEqualTo(1);
    }
    @Test void sessaoCsrfAutoriaENotaSaoValidados() throws Exception {
        conversar(comprador);
        mvc.perform(post(acao).param("nota","5").with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post(acao).param("nota","5").with(user(comprador))).andExpect(status().isForbidden());
        mvc.perform(post(acao).param("nota","5").with(user(vendedor)).with(csrf())).andExpect(status().isForbidden());
        for(String nota:List.of("0","6","abc","")) mvc.perform(post(acao).param("nota",nota).with(user(comprador)).with(csrf())).andExpect(flash().attributeExists("errosAvaliacaoPerfil"));
        mvc.perform(post(acao).param("nota","5").param("comentario","x".repeat(1001)).with(user(comprador)).with(csrf())).andExpect(flash().attributeExists("errosAvaliacaoPerfil"));
        assertThat(registros.count()).isZero();
        mvc.perform(post(acao).param("nota","4").param("autorId",""+outro.getId()).param("vendedorId",""+comprador.getId()).with(user(comprador)).with(csrf())).andExpect(flash().attributeExists("sucessoAvaliacaoPerfil"));
        assertThat(registros.findByVendedorIdAndAutorId(vendedor.getId(),comprador.getId())).isPresent();
        assertThat(registros.findByVendedorIdAndAutorId(vendedor.getId(),outro.getId())).isEmpty();
    }
    @Test void umaAvaliacaoPorPessoaEdicaoRecalculaMediaERemocaoRespeitaAutor() throws Exception {
        conversar(comprador); conversar(outro);
        avaliacoes.salvar(vendedor.getId(),comprador,form(5,"Bom atendimento"));
        avaliacoes.salvar(vendedor.getId(),comprador,form(5,"Bom atendimento"));
        avaliacoes.salvar(vendedor.getId(),outro,form(3,"Regular"));
        assertThat(registros.count()).isEqualTo(2);
        assertThat(avaliacoes.perfil(vendedor.getId(),null,0).resumo().media()).isEqualTo("4,0");
        avaliacoes.salvar(vendedor.getId(),comprador,form(1,"Atualizado"));
        assertThat(avaliacoes.perfil(vendedor.getId(),null,0).resumo().media()).isEqualTo("2,0");
        mvc.perform(get(perfil).with(user(comprador))).andExpect(content().string(containsString("Editar sua avaliação")));
        mvc.perform(post(acao+"/remover").with(user(vendedor)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(acao+"/remover").with(user(comprador))).andExpect(status().isForbidden());
        mvc.perform(post(acao+"/remover").with(user(comprador)).with(csrf())).andExpect(redirectedUrl(perfil+"#avaliacoes"));
        assertThat(registros.count()).isEqualTo(1);
        assertThat(avaliacoes.perfil(vendedor.getId(),null,0).resumo().media()).isEqualTo("3,0");
    }
    @Test void comentariosEscapadosSemEmailOuConversasPrivadas() throws Exception {
        conversar(comprador); avaliacoes.salvar(vendedor.getId(),comprador,form(5,"<script>alert('comentario')</script>"));
        mvc.perform(get(perfil)).andExpect(status().isOk()).andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>")))).andExpect(content().string(not(containsString(comprador.getUsername()))))
            .andExpect(content().string(not(containsString("Mensagem privada comprador")))).andExpect(content().string(not(containsString("Resposta privada vendedor"))));
    }
    @Test void perfilEAvaliacaoPersistemAposExcluirUltimoAnuncio() throws Exception {
        conversar(comprador); fotos.perfil(vendedor,TesteFotos.arquivo("arquivo"),false);
        avaliacoes.salvar(vendedor.getId(),comprador,form(5,"Atencioso"));
        em.flush(); em.clear();
        gerenciar.excluir(anuncio,vendedor,anuncios.findById(anuncio).orElseThrow().getVersao()); em.flush(); em.clear();
        assertThat(buscar("NomeExclusivo")).containsExactly(vendedor.getId());
        mvc.perform(get(perfil)).andExpect(status().isOk()).andExpect(content().string(containsString("Atencioso")));
        mvc.perform(get(perfil+"/foto")).andExpect(status().isOk());
        mvc.perform(post(acao+"/remover").with(user(comprador)).with(csrf())).andExpect(redirectedUrl("/vendedores"));
        assertThat(buscar("NomeExclusivo")).isEmpty();
    }
    @Test void comentariosSaoPaginadosSemMudarMediaTotal() throws Exception {
        for(int i=0;i<11;i++) { var autor=usuario("Avaliador "+i); conversar(autor); avaliacoes.salvar(vendedor.getId(),autor,form(4,"Opiniao "+i)); }
        var primeira=avaliacoes.perfil(vendedor.getId(),null,0);
        assertThat(primeira.itens().getContent()).hasSize(10); assertThat(primeira.resumo().total()).isEqualTo(11);
        assertThat(avaliacoes.perfil(vendedor.getId(),null,1).itens().getContent()).hasSize(1);
        mvc.perform(get(perfil)).andExpect(status().isOk()).andExpect(content().string(containsString("paginaAvaliacoes=1")));
        mvc.perform(get(perfil).param("paginaAvaliacoes","1")).andExpect(status().isOk()).andExpect(content().string(containsString("Página 2 de 2")));
    }
}
