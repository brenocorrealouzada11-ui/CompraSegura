package CompraSegura;

import CompraSegura.anuncio.*;
import CompraSegura.catalogo.*;
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
class ComparacaoSegurancaTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired AnuncioRepository anuncios;
    @Autowired FotosAnuncioService cadastro;
    @Autowired FotoAnuncioRepository fotos;
    @Autowired PublicacaoService publicacao;
    @Autowired VendaService vendas;
    @Autowired AnuncioService gerenciar;
    @Autowired ComparacaoService comparacao;
    @Autowired CompraSegura.favorito.FavoritoService favoritos;
    @Autowired CompraSegura.confiabilidade.AvaliacaoRepository avaliacoes;
    private UsuarioAutenticado dono, comprador;
    private Long a,b,c;
    private AnuncioForm form(String titulo,String preco) {
        var f=new AnuncioForm(); f.setTitulo(titulo); f.setPreco(new BigDecimal(preco)); f.setTipo(TipoAparelho.SMARTPHONE);
        f.setMarca("Marca Comparada"); f.setModelo("Modelo Comparado"); f.setCondicao(EstadoConservacao.BOM); f.setReparos(HistoricoReparos.COM_REPAROS);
        f.setDescricao("A tela foi substituída durante um reparo anterior."); f.setImeis("000000000000001\n000000000000002"); return f;
    }
    private Long criar(String titulo,String preco) {
        Long id=cadastro.criar(dono,form(titulo,preco),TesteFotos.arquivo("arquivo"),List.of(TesteFotos.arquivo("fotos")));
        publicacao.alterar(id,dono,versao(id),true); return id;
    }
    private long versao(Long id) { return anuncios.findById(id).orElseThrow().getVersao(); }
    @BeforeEach void preparar() {
        dono=new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario("Vendedor Comparacao",UUID.randomUUID()+"@example.invalid","hash-secreto")));
        comprador=new UsuarioAutenticado(usuarios.saveAndFlush(new Usuario("Comprador",UUID.randomUUID()+"@example.invalid","hash-secreto")));
        a=criar("Celular <script>alert(1)</script>","100"); b=criar("Segundo aparelho","200"); c=criar("Terceiro aparelho","300");
    }
    @Test void comparaDoisOuTresEmOrdemComDadosPublicosEResultadosMascarados() throws Exception {
        assertThat(comparacao.comparar(List.of(b.toString(),a.toString())).stream().map(d->d.anuncio().id())).containsExactly(b,a);
        mvc.perform(get("/comparar").param("ids",a.toString(),b.toString(),c.toString())).andExpect(status().isOk())
            .andExpect(content().string(containsString("Conservação"))).andExpect(content().string(containsString("Reparos e alterações")))
            .andExpect(content().string(containsString("Segundo aparelho"))).andExpect(content().string(containsString("Terceiro aparelho")))
            .andExpect(content().string(containsString("SIMULAÇÃO"))).andExpect(content().string(containsString("Restrição indicada no resultado recebido")))
            .andExpect(content().string(containsString("•••••••••••0002"))).andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(not(containsString("<script>")))).andExpect(content().string(not(containsString("000000000000001"))))
            .andExpect(content().string(not(containsString("000000000000002")))).andExpect(content().string(not(containsString(dono.getUsername()))))
            .andExpect(content().string(not(containsString("hash-secreto")))).andExpect(content().string(not(containsString("/evidencia"))));
    }
    @Test void selecaoVaziaMostraOrientacaoESelecaoInvalidaRecebeMensagem() throws Exception {
        mvc.perform(get("/comparar")).andExpect(status().isOk()).andExpect(content().string(containsString("Escolha dois ou três aparelhos")));
        for(String[] ids:List.of(new String[]{a.toString()},new String[]{a.toString(),b.toString(),c.toString(),"999"},
                new String[]{a.toString(),a.toString()},new String[]{"0",b.toString()},new String[]{"-1",b.toString()},
                new String[]{"abc",b.toString()},new String[]{"9999999999999999999",b.toString()}))
            mvc.perform(get("/comparar").param("ids",ids)).andExpect(status().isBadRequest()).andExpect(model().attributeExists("erroComparacao"));
    }
    @Test void rascunhosEExcluidosNaoEntramNemParaODono() throws Exception {
        publicacao.alterar(a,dono,versao(a),false);
        for(var request:List.of(get("/comparar").param("ids",a.toString(),b.toString()),get("/comparar").param("ids",a.toString(),b.toString()).with(user(dono))))
            mvc.perform(request).andExpect(status().isNotFound()).andExpect(content().string(not(containsString("&lt;script&gt;"))));
        gerenciar.excluir(c,dono,versao(c));
        mvc.perform(get("/comparar").param("ids",c.toString(),b.toString())).andExpect(status().isNotFound()).andExpect(model().attributeExists("erroComparacao"));
    }
    @Test void vendidoESemAvaliacaoEDesatualizadoSaoIdentificados() throws Exception {
        vendas.concluir(a,dono,versao(a));
        avaliacoes.findByAnuncioIdAndAtualTrue(b).forEach(CompraSegura.confiabilidade.RegistroAvaliacao::desatualizar); avaliacoes.flush();
        Long sem=gerenciar.criar(dono,form("Sem avaliacao","50"));
        fotos.saveAndFlush(new FotoAnuncio(anuncios.findById(sem).orElseThrow(),TesteFotos.imagem())); publicacao.alterar(sem,dono,versao(sem),true);
        mvc.perform(get("/comparar").param("ids",a.toString(),b.toString(),sem.toString())).andExpect(status().isOk())
            .andExpect(content().string(containsString("Vendido · informado pelo vendedor"))).andExpect(content().string(containsString("Avaliação desatualizada")))
            .andExpect(content().string(containsString("Sem avaliação registrada."))).andExpect(content().string(containsString("Sem consultas registradas.")));
    }
    @Test void seletoresEmExplorarEFavoritosFuncionamComoFormularioSemJavascript() throws Exception {
        mvc.perform(get("/anuncios")).andExpect(status().isOk()).andExpect(content().string(containsString("form=\"form-comparacao\"")))
            .andExpect(content().string(containsString("Comparar aparelho"))).andExpect(content().string(containsString("name=\"ids\"")));
        favoritos.adicionar(a,comprador); favoritos.adicionar(b,comprador);
        mvc.perform(get("/minha-conta/favoritos").with(user(comprador))).andExpect(status().isOk()).andExpect(content().string(containsString("Comparar selecionados")));
        mvc.perform(get("/comparar").param("ids",a.toString(),b.toString())).andExpect(status().isOk());
        mvc.perform(get("/vendedores/"+dono.getId())).andExpect(status().isOk()).andExpect(content().string(not(containsString("form=\"form-comparacao\""))));
    }
    @Test void centralPublicaExplicaLimitesETemFontesELinksDeNavegacao() throws Exception {
        mvc.perform(get("/seguranca")).andExpect(status().isOk()).andExpect(content().string(containsString("Central de segurança")))
            .andExpect(content().string(containsString("*#06#"))).andExpect(content().string(containsString("As consultas deste MVP são simuladas")))
            .andExpect(content().string(containsString("não processa pagamentos"))).andExpect(content().string(containsString("https://www.gov.br/anatel/")))
            .andExpect(content().string(containsString("https://cartilha.cert.br/")));
        mvc.perform(get("/seguranca").with(user(comprador))).andExpect(status().isOk()).andExpect(content().string(containsString("Minha conta")));
        mvc.perform(get("/anuncios/"+a)).andExpect(content().string(containsString("/seguranca#resultados")));
    }
}
