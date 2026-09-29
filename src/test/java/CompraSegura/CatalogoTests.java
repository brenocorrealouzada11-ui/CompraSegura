package CompraSegura;

import CompraSegura.anuncio.*;
import CompraSegura.catalogo.*;
import CompraSegura.usuario.*;
import java.math.BigDecimal;
import java.util.UUID;
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
class CatalogoTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AnuncioRepository anuncios;
    @Autowired CompraSegura.foto.FotoAnuncioRepository fotos;
    @Autowired PreparacaoAnuncioService preparacao;
    @Autowired PublicacaoService publicacao;
    @Autowired CatalogoService catalogo;
    @Autowired EdicaoRascunhoService edicao;
    private UsuarioAutenticado dono, outro;

    @BeforeEach void preparar() {
        dono = new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario("Vendedor Publico", UUID.randomUUID()+"@example.invalid", "hash-secreto-teste")));
        outro = new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario("Outro Vendedor", UUID.randomUUID()+"@example.invalid", "hash-teste")));
    }
    private Long criar(String titulo, String preco, TipoAparelho tipo, UsuarioAutenticado principal, boolean publicar) {
        var form = new AnuncioForm();
        form.setTitulo(titulo); form.setPreco(new BigDecimal(preco)); form.setTipo(tipo);
        form.setMarca("MarcaBusca"); form.setModelo("ModeloBusca"); form.setCondicao(EstadoConservacao.BOM);
        form.setReparos(HistoricoReparos.SEM_REPAROS); form.setImeis("000000000000001\n000000000000002");
        Long id = preparacao.salvar(principal, form, null);
        fotos.saveAndFlush(new CompraSegura.foto.FotoAnuncio(anuncios.findById(id).orElseThrow(), TesteFotos.imagem()));
        if (publicar) publicacao.alterar(id, principal, versao(id), true);
        return id;
    }
    private long versao(Long id) { return anuncios.findById(id).orElseThrow().getVersao(); }
    @Test void rascunhosNaoAparecemEmNenhumaRotaPublicaNemParaODono() throws Exception {
        Long id = criar("Segredo do vendedor", "100", TipoAparelho.SMARTPHONE, dono, false);
        mvc.perform(get("/anuncios")).andExpect(status().isOk()).andExpect(content().string(not(containsString("Segredo do vendedor"))));
        mvc.perform(get("/anuncios/"+id)).andExpect(status().isNotFound());
        mvc.perform(get("/anuncios/"+id).with(user(dono))).andExpect(status().isNotFound());
        mvc.perform(get("/vendedores/"+dono.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/minha-conta/anuncios/"+id+"/publicar").with(user(dono))).andExpect(status().isOk());
        assertThat(anuncios.findById(id).orElseThrow().getStatus()).isEqualTo("RASCUNHO");
    }
    @Test void publicacaoExigeDonoCsrfVersaoAtualEPodeSerRetirada() throws Exception {
        Long id = criar("Aparelho para publicar", "100", TipoAparelho.SMARTPHONE, dono, false);
        String acao = "/minha-conta/anuncios/"+id;
        mvc.perform(post(acao+"/publicar").param("versao", "0").with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post(acao+"/publicar").param("versao", "0").with(user(dono))).andExpect(status().isForbidden());
        mvc.perform(post(acao+"/publicar").param("versao", "0").with(user(outro)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(acao+"/publicar").param("versao", "99").with(user(dono)).with(csrf())).andExpect(flash().attributeExists("erroPublicacao"));
        assertThat(anuncios.findById(id).orElseThrow().getStatus()).isEqualTo("RASCUNHO");
        mvc.perform(post(acao+"/publicar").param("versao", ""+versao(id)).with(user(dono)).with(csrf())).andExpect(flash().attributeExists("sucessoPublicacao"));
        mvc.perform(get("/anuncios/"+id)).andExpect(status().isOk());
        mvc.perform(get(acao).with(user(dono))).andExpect(status().isOk()).andExpect(content().string(containsString("Retirar da pesquisa")))
            .andExpect(content().string(not(containsString("Editar rascunho"))));
        mvc.perform(get(acao+"/editar").with(user(dono))).andExpect(status().isConflict());
        mvc.perform(post(acao+"/retirar").param("versao", ""+versao(id)).with(user(outro)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post(acao+"/retirar").param("versao", ""+versao(id)).with(user(dono)).with(csrf())).andExpect(flash().attributeExists("sucessoPublicacao"));
        mvc.perform(get("/anuncios/"+id)).andExpect(status().isNotFound());
        mvc.perform(get(acao+"/editar").with(user(dono))).andExpect(status().isOk());
    }
    @Test void detalhesPublicosEscondemCredenciaisImagemEImeisCompletos() throws Exception {
        Long id = criar("Aparelho <script>alert(1)</script>", "100", TipoAparelho.SMARTPHONE, dono, true);
        mvc.perform(get("/anuncios/"+id)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Risco alto identificado")))
            .andExpect(content().string(containsString("SIMULAÇÃO")))
            .andExpect(content().string(containsString("•••••••••••0002")))
            .andExpect(content().string(not(containsString("000000000000002"))))
            .andExpect(content().string(not(containsString(dono.getUsername()))))
            .andExpect(content().string(not(containsString("hash-secreto-teste"))))
            .andExpect(content().string(not(containsString("<script>"))))
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("O risco considera as informações disponíveis"))));
        mvc.perform(get("/minha-conta/anuncios/"+id+"/evidencia")).andExpect(status().is3xxRedirection());
    }
    @Test void pesquisaCombinaTextoTipoConservacaoPrecoEOrdenacao() throws Exception {
        Long barato = criar("Celular prata", "120", TipoAparelho.SMARTPHONE, dono, true);
        Long caro = criar("Celular preto", "450", TipoAparelho.SMARTPHONE, dono, true);
        criar("Tablet prata", "200", TipoAparelho.TABLET, dono, true);
        criar("Celular escondido", "150", TipoAparelho.SMARTPHONE, dono, false);
        var filtro = new FiltroPesquisa(); filtro.setQ("MARCABUSCA"); filtro.setTipo(TipoAparelho.SMARTPHONE);
        filtro.setCondicao(EstadoConservacao.BOM); filtro.setMinimo(new BigDecimal("100")); filtro.setMaximo(new BigDecimal("500"));
        filtro.setOrdem(FiltroPesquisa.Ordem.MENOR_PRECO);
        assertThat(catalogo.pesquisar(filtro).getContent()).extracting(AnuncioPublico::id).containsExactly(barato, caro);
        filtro.setOrdem(FiltroPesquisa.Ordem.MAIOR_PRECO);
        assertThat(catalogo.pesquisar(filtro).getContent()).extracting(AnuncioPublico::id).containsExactly(caro, barato);
        filtro.setQ("modelobusca"); filtro.setMaximo(new BigDecimal("200"));
        assertThat(catalogo.pesquisar(filtro).getContent()).extracting(AnuncioPublico::id).containsExactly(barato);
        mvc.perform(get("/anuncios").param("q", "prata").param("tipo", "TABLET")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Tablet prata"))).andExpect(content().string(not(containsString("Celular prata"))));
    }
    @Test void pesquisaTrataCuringasComoTextoEFiltrosInvalidosNaoQuebramPagina() throws Exception {
        criar("Desconto 10% especial", "100", TipoAparelho.SMARTPHONE, dono, true);
        criar("Outro aparelho", "100", TipoAparelho.SMARTPHONE, dono, true);
        var filtro = new FiltroPesquisa(); filtro.setQ("%");
        assertThat(catalogo.pesquisar(filtro).getTotalElements()).isEqualTo(1);
        for (String[] invalido : new String[][] {{"tipo","INVALIDO"}, {"ordem","invalida"}, {"minimo","abc"}, {"pagina","-1"}}) {
            mvc.perform(get("/anuncios").param(invalido[0], invalido[1])).andExpect(status().isOk()).andExpect(model().attributeHasErrors("filtro"));
        }
        mvc.perform(get("/anuncios").param("minimo", "300").param("maximo", "100"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("O preço mínimo não pode ser maior")));
    }
    @Test void paginacaoMantemFiltrosEHistoricoMostraSomenteAnunciosPublicosDoVendedor() throws Exception {
        for (int i=0; i<13; i++) criar("Celular pagina "+i, "100", TipoAparelho.SMARTPHONE, dono, true);
        criar("Rascunho confidencial", "100", TipoAparelho.SMARTPHONE, dono, false);
        criar("Anuncio outro vendedor", "100", TipoAparelho.SMARTPHONE, outro, true);
        mvc.perform(get("/anuncios").param("q", "pagina").param("ordem", "MENOR_PRECO"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Página 1 de 2")))
            .andExpect(content().string(containsString("q=pagina"))).andExpect(content().string(containsString("ordem=MENOR_PRECO")));
        var filtro = new FiltroPesquisa(); filtro.setQ("pagina"); filtro.setPagina(1);
        assertThat(catalogo.pesquisar(filtro).getContent()).hasSize(1);
        mvc.perform(get("/vendedores/"+dono.getId())).andExpect(status().isOk())
            .andExpect(content().string(containsString("Vendedor Publico")))
            .andExpect(content().string(not(containsString("Rascunho confidencial"))))
            .andExpect(content().string(not(containsString("Anuncio outro vendedor"))))
            .andExpect(content().string(not(containsString(dono.getUsername()))));
        assertThat(catalogo.vendedor(dono.getId(), 0).anuncios().getTotalElements()).isEqualTo(13);
    }
    @Test void sessaoContinuaAtivaAoExplorarETextoSolicitadoFoiRemovido() throws Exception {
        Long id = criar("Celular sem restricao", "100", TipoAparelho.SMARTPHONE, dono, false);
        var form = edicao.carregar(id, dono); form.setImeis("000000000000001");
        edicao.salvar(id, dono, form, null);
        mvc.perform(get("/anuncios").with(user(dono))).andExpect(status().isOk()).andExpect(content().string(containsString("Minha conta")));
        mvc.perform(post("/minha-conta/anuncios/"+id+"/avaliar").with(user(dono)).with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(get("/minha-conta/anuncios/"+id).with(user(dono))).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Avaliação inconclusiva"))))
            .andExpect(content().string(not(containsString("O risco considera as informações disponíveis"))));
    }
}
