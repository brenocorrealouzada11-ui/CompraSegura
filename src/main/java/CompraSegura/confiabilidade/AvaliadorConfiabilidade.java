package CompraSegura.confiabilidade;

/** Politica de risco isolada da consulta externa, persistencia e publicacao. */
public interface AvaliadorConfiabilidade {
    AvaliacaoConfiabilidade avaliar(ContextoAvaliacao contexto);
}
