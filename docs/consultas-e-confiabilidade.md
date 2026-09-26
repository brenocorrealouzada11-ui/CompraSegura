# Consultas de IMEI e avaliação de confiabilidade

## Fluxo implementado

O formulário reúne dados, imagem de evidência e prévia da avaliação. A prévia usa POST autenticado com CSRF e retorna um fragmento HTML com o resultado, sem persistir anúncio, imagem ou consultas. O arquivo permanece no campo do navegador. Mudanças no formulário invalidam a prévia; respostas de requisições antigas não substituem os dados alterados.

Ao salvar, `PreparacaoAnuncioService` valida novamente o formulário e a imagem, consulta todos os IMEIs e recalcula a avaliação no servidor. A transação grava o rascunho, a evidência, as consultas e o resultado. Uma restrição não impede o salvamento. A publicação é uma etapa futura e independente.

## Responsabilidades

- `NormalizadorIMEIs`: valida formato e duplicações, preservando todos os identificadores.
- `ProvedorConsultaIMEI`: contrato com origem e identificação do provedor; permite substituir o adaptador simulado por um real.
- `ProvedorSimuladoIMEI`: quatro cenários explícitos, sem comunicação externa. Outros números retornam inconclusivo.
- `VerificacaoService`: consulta individualmente todos os IMEIs, trata falhas como indisponibilidade, constrói o contexto e registra resultados. Respostas de outro IMEI ou origem incompatível não são aceitas.
- `AvaliadorPorRegras`: política qualitativa isolada da interface, do provedor e da persistência.
- `RegistroAvaliacao` e `ConsultaIMEI`: histórico persistido após o salvamento, com motivos, pendências, origem, datas, versão da regra, contexto e hash da evidência usada.

## Política qualitativa v1

1. O IMEI é o fator principal. Cada IMEI esperado precisa ter exatamente uma consulta correspondente. Faltas, duplicações ou resultados de IMEIs estranhos geram pendências.
2. Uma restrição conhecida leva a **risco alto**, mesmo com falhas em outros resultados. Condição do aparelho ou histórico favorável não escondem a restrição. Uma divergência efetivamente conferida na evidência também leva a risco alto.
3. Sem risco alto identificado, pendências levam a **inconclusivo**. Simulação, consulta ausente/inconclusiva/indisponível, evidência não conferida e histórico indisponível são explicitados; nunca representam aprovação.
4. Somente em um futuro contexto completo, com consulta real e evidência conferida, defeitos declarados ou ocorrências do vendedor poderão levar a risco moderado; sem esses fatores, a classificação poderá ser baixa. Mesmo risco baixo não garante procedência ou funcionamento.
5. Reparos declarados não são automaticamente uma irregularidade. As observações detalham o reparo, e condições e reparos continuam sendo declarações do vendedor.
6. A imagem enviada recebe **aguardando conferência**. Não há OCR nem correspondência automática de IMEI/modelo nesta etapa. O sistema nunca marca uma imagem como coerente só porque ela foi enviada.
7. O histórico real do vendedor ainda não existe. O contexto usa **não disponível**, sem presumir reputação positiva ou negativa.
8. Toda simulação aparece no resultado individual e na avaliação agregada. Mesmo com todos os cenários sem restrição, o resultado permanece inconclusivo para uma decisão real.
9. Não há pesos ou nota de 0 a 100. Motivos e pendências explicam o resultado e a informação que falta.

## Cenários de demonstração

| IMEI fictício | Resultado simulado |
| --- | --- |
| `000000000000001` | Sem restrição |
| `000000000000002` | Com restrição |
| `000000000000003` | Inconclusivo |
| `000000000000004` | Indisponível |

Qualquer outro número retorna inconclusivo, sem alegar dados reais sobre o aparelho. A combinação dos finais 001 e 002 permite testar que uma restrição não é compensada por outro resultado sem restrição.

## Persistência e atualização

A migração V5 cria avaliações e consultas; a V6 adiciona o hash da evidência. Migrações aplicadas são preservadas sem alteração de checksum. Datas de avaliação/consulta são armazenadas em UTC. O contexto é uma fotografia textual das informações usadas; o hash SHA-256 identifica a imagem processada daquela versão, sem preservar cópias de imagens substituídas.

Ao substituir a evidência, o histórico fica preservado, mas a avaliação corrente é marcada como desatualizada. A atualização gera novo registro e deixa o anterior desatualizado. Gravação da evidência, reavaliação e exclusão usam bloqueio do mesmo anúncio para evitar resultados concorrentes marcados como atuais. Todas as rotas de dados persistidos exigem o proprietário.

Como regra operacional provisória, consultas e avaliações com mais de 24 horas exigem atualização. Datas futuras incompatíveis também não são aceitas como recentes na política. Esse prazo deverá ser revisto conforme o contrato do provedor real; não representa validade oficial de consulta externa.

## Próximos incrementos

- Implementar a conferência da coerência entre imagem, IMEIs e modelo.
- Definir e integrar um provedor real, incluindo timeout, limites, erros e validade dos resultados.
- Implementar histórico real do vendedor sem criar reputação fictícia.
- Ao implementar edição de anúncios, invalidar a avaliação quando mudarem IMEIs, modelo, condições, reparos ou observações relevantes.
- Adicionar pesquisa e publicação com critérios próprios, sem bloquear automaticamente por restrição de IMEI.
- Se necessário, expor o histórico de avaliações em uma interface própria; atualmente os detalhes mostram o último resultado.

A identificação automática de modelo por IMEI continua pendente do provedor. Pagamentos, entregas e registro de negociações ficam fora do MVP.
