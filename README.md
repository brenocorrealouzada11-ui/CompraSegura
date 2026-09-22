# CompraSegura

Plataforma web acadêmica para apoiar a compra e venda de smartphones e tablets usados com conexão celular e IMEI. A proposta é reunir anúncios, evidências do aparelho e indicadores de confiabilidade para ajudar o comprador a avaliar os riscos antes de negociar.

O **IMEI é o principal fator da avaliação de risco**, complementado pelas informações do aparelho e pelo histórico do vendedor. A plataforma busca dar transparência à decisão de compra; a avaliação não representa garantia de procedência ou de funcionamento.

> **Em desenvolvimento:** a página inicial e o cadastro de usuário com persistência em MySQL estão implementados. Login, gerenciamento de perfil, anúncios e consulta de IMEI são as próximas etapas.

## Objetivo e escopo

O CompraSegura foi pensado para pessoas que desejam comprar ou vender aparelhos usados e precisam de informações mais claras sobre sua condição e identificação.

O MVP contempla smartphones e tablets com conexão celular e IMEI. Aparelhos sem IMEI, pagamentos, entregas, recursos pagos e ferramentas específicas para lojas ficam fora desta etapa. Registro formal de negociações, comprovantes e relatórios também não integram o escopo atual da modelagem.

## Funcionalidades previstas

- Cadastro de usuário, login e gerenciamento de perfil.
- Cadastro, edição e remoção de anúncios pelo vendedor.
- Identificação do tipo e modelo do aparelho e declaração de suas condições e alterações.
- Registro de todos os IMEIs do aparelho e envio da evidência obtida com `*#06#`.
- Validação dos dados e consulta individual dos IMEIs.
- Avaliação de confiabilidade com apresentação transparente dos resultados.
- Pesquisa e visualização de anúncios, situação dos IMEIs e histórico do vendedor.

Comprador e vendedor são papéis que um mesmo usuário pode assumir.

## Regras centrais do projeto

1. **IMEI como principal indicador:** a situação dos IMEIs deve orientar a avaliação de risco do aparelho.
2. **Todos os IMEIs devem ser informados:** aparelhos com mais de um IMEI precisam ter todos os identificadores registrados e consultados.
3. **Evidência de identificação:** o cadastro deve incluir a evidência do `*#06#`, permitindo conferir a coerência entre os IMEIs, o modelo e os dados apresentados.
4. **Transparência sobre irregularidades:** uma irregularidade na consulta do IMEI não bloqueia automaticamente o anúncio. Ela deve aparecer claramente para o comprador e influenciar a avaliação de confiabilidade.
5. **Validação cadastral é uma etapa distinta:** dados inválidos, incompletos ou incompatíveis devem ser tratados pelo fluxo de validação, sem confundir esses problemas com o resultado da consulta externa.
6. **Condições e alterações declaradas:** o vendedor deve informar as condições e alterações do aparelho para apoiar a análise do comprador.
7. **Histórico como informação complementar:** o histórico do vendedor deve ser considerado junto aos dados do aparelho, sem substituir a consulta dos IMEIs.
8. **Limites da consulta visíveis:** falhas de consulta e resultados inconclusivos não devem ser apresentados como confirmação de regularidade.

A integração real depende da definição e do acesso a um serviço externo de consulta de IMEI. Durante o desenvolvimento, está prevista a utilização de respostas simuladas, identificadas como simulação. O provedor e a fórmula de cálculo da confiabilidade ainda não estão implementados neste repositório.

## Fluxo principal planejado

1. O vendedor acessa sua conta e inicia um anúncio.
2. Informa os dados do aparelho, tipo, modelo, condições e alterações.
3. Registra todos os IMEIs e envia a evidência do `*#06#`.
4. O sistema valida os dados e consulta cada IMEI.
5. O sistema avalia a confiabilidade e apresenta a situação encontrada.
6. O comprador pesquisa anúncios e consulta os dados do aparelho, os resultados da avaliação e o histórico do vendedor.

## Modelagem UML

A organização definida para a modelagem em PlantUML reúne **um diagrama de casos de uso, seis diagramas de sequência e um diagrama de classes**:

| Diagrama | Responsabilidade |
| --- | --- |
| Casos de uso — visão geral | Usuário, comprador, vendedor, serviço externo e funcionalidades do sistema |
| Sequência — cadastrar usuário | Validação dos dados, verificação de e-mail e cadastro |
| Sequência — fazer login | Identificação do usuário e autenticação |
| Sequência — gerenciar perfil | Edição dos dados e alteração de senha |
| Sequência — cadastrar e validar anúncio | Cadastro do aparelho, evidência, consulta dos IMEIs e avaliação |
| Sequência — pesquisar e visualizar anúncio | Pesquisa, detalhes, confiabilidade e histórico do vendedor |
| Sequência — gerenciar anúncio | Edição, remoção e verificação de permissão |
| Classes — visão geral | Entidades do domínio e seus relacionamentos |

Na modelagem acordada, `Cadastrar anúncio` inclui `Validar aparelho`, que inclui `Consultar IMEIs`. A visualização do anúncio reúne a confiabilidade do aparelho e o histórico do vendedor.

### Entidades previstas

| Entidade | Papel no domínio |
| --- | --- |
| `Usuario` | Representa quem utiliza o sistema e pode publicar anúncios |
| `Anuncio` | Reúne a oferta de um aparelho e seu responsável |
| `Aparelho` | Representa o dispositivo anunciado |
| `TipoAparelho` | Classifica o tipo do dispositivo |
| `ModeloAparelho` | Identifica o modelo do dispositivo |
| `IMEI` | Representa cada identificador associado ao aparelho |
| `EvidenciaIMEI` | Registra a evidência de identificação apresentada |
| `ConsultaIMEI` | Registra as consultas realizadas para um IMEI |
| `AvaliacaoConfiabilidade` | Reúne o resultado da avaliação do anúncio |

Um usuário pode publicar vários anúncios; cada anúncio pertence a um usuário e apresenta um aparelho. O aparelho possui tipo, modelo, um ou mais IMEIs e evidência de identificação. Cada IMEI pode ter várias consultas, e o anúncio recebe uma avaliação de confiabilidade.

`Usuario` já está implementada com nome, e-mail e hash da senha. As demais entidades representam o planejamento do domínio. Os arquivos-fonte `.puml`/`.plantuml` ainda serão reconstruídos; esta seção resume a modelagem registrada no histórico do projeto.

## Tecnologias

| Tecnologia | Situação no projeto |
| --- | --- |
| Java 17 | Versão configurada no `pom.xml` |
| Spring Boot 4.1.1 | Base configurada para a aplicação |
| Spring Web MVC | Dependência presente para a camada web |
| Thymeleaf | Formulário de cadastro e confirmação renderizados no servidor |
| HTML e CSS | Utilizados na página inicial |
| Maven e Maven Wrapper | Gerenciamento de dependências e execução |
| Spring Boot DevTools | Dependência de apoio ao desenvolvimento |
| JavaScript | Previsto para interações do frontend |
| MySQL 8.4 | Persistência dos usuários na porta local 3308 |
| Flyway | Migrações versionadas do banco de dados |
| Spring Security | Hash de senhas com PBKDF2 e proteção CSRF do formulário |
| Spring Data JPA | Dependencia adicionada para persistencia |
| H2 | Banco em memoria exclusivo dos testes |
| Serviço externo de IMEI | Integração planejada, ainda não implementada |

A arquitetura planejada separa a interface, os controllers, os serviços de negócio e a persistência. A consulta externa de IMEI deverá ficar isolada para permitir desenvolver com respostas simuladas e integrar um provedor posteriormente.

## Executar com MySQL local

O servidor deve estar ativo em `127.0.0.1:3308`, com o banco `comprasegura` e o usuario `comprasegura_app` previamente criados.

Defina a variavel de ambiente `DB_PASSWORD` no ambiente que inicia a aplicacao. Nao coloque a senha no `application.properties`, no `pom.xml` ou no Git. O Spring Boot le variaveis de ambiente diretamente; um arquivo `.env` sozinho nao e carregado automaticamente.

No Windows, pesquise **Editar as variaveis de ambiente da sua conta**, crie `DB_PASSWORD` em **Variaveis de usuario** e informe a senha localmente. Reinicie o NetBeans e os terminais para que recebam a variavel. Depois execute a classe `CompraseguraApplication` pela IDE ou, na pasta do `pom.xml`:

```powershell
.\mvnw.cmd spring-boot:run
```

O terminal precisa de um JDK configurado em `JAVA_HOME`. A pagina inicial fica em [localhost:8080](http://localhost:8080).

Valores padrao e configuracoes opcionais:

| Variavel | Valor padrao |
| --- | --- |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3308/comprasegura` |
| `DB_USERNAME` | `comprasegura_app` |
| `DB_PASSWORD` | Obrigatoria, sem valor padrao |

O Flyway aplica as migrações em `src/main/resources/db/migration` na inicialização. A primeira cria `usuarios`; `flyway_schema_history` registra as versões aplicadas. O Hibernate usa `ddl-auto=validate` para conferir a correspondência entre entidades e tabelas, sem recriá-las a cada execução. Não edite migrações já aplicadas: crie uma nova versão para alterações futuras.

## Cadastro de usuário

Acesse [Criar conta](http://localhost:8080/cadastro) ou use o link na página inicial. Informe nome (2 a 100 caracteres), e-mail e senha (12 a 128 caracteres). O servidor valida os campos, normaliza o e-mail e impede duplicidade também por restrição no banco. A senha recebe hash PBKDF2 com salt aleatório; o texto original não é salvo.

Após o envio, a página confirma a criação da conta. O cadastro ainda não autentica o usuário: login e perfil serão implementados na próxima etapa. Esta conta é um usuário do site, diferente do usuário MySQL `comprasegura_app`.

Para conferir os cadastros no Workbench:

```sql
SELECT id, nome, email FROM comprasegura.usuarios;
```

## Testes

```powershell
.\mvnw.cmd test
```

Os testes usam o perfil `test`, H2 em memória no modo MySQL e a mesma migração SQL, sem modificar o banco local. Cobrem inicialização, formulário público, proteção CSRF, cadastro válido, hash da senha, e-mail duplicado, campos inválidos e confirmação. H2 não substitui a verificação da aplicação com MySQL real.
