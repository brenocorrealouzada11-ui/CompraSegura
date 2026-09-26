package CompraSegura.anuncio;

import java.util.*;

public final class NormalizadorIMEIs {
    private NormalizadorIMEIs() { }
    public static List<String> normalizar(String texto) {
        if (texto == null || texto.length() > 1000) throw new IllegalArgumentException("Informe todos os IMEIs em até 1.000 caracteres.");
        String normalizados = texto.replaceAll("(?<![0-9])([0-9]{8})[- ]([0-9]{6})[- ]([0-9])(?![0-9])", "$1$2$3");
        var numeros = Arrays.stream(normalizados.split("[,;\\s]+")).filter(s -> !s.isBlank()).toList();
        if (numeros.isEmpty() || numeros.stream().anyMatch(n -> !n.matches("[0-9]{15}"))) {
            throw new IllegalArgumentException("Cada IMEI deve conter 15 dígitos. Use números ou o formato 12345678-901234-5.");
        }
        if (new HashSet<>(numeros).size() != numeros.size()) throw new IllegalArgumentException("Há IMEIs repetidos. Informe cada identificador apenas uma vez.");
        return numeros;
    }
}
