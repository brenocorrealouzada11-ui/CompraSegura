package CompraSegura.consulta;

/** Ponto de extensao: um adaptador simulado e, posteriormente, um adaptador real. */
public interface ProvedorConsultaIMEI {
    ResultadoConsultaIMEI consultar(String imei);
    OrigemConsulta origem();
    String nome();
}
