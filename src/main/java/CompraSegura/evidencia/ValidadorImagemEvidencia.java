package CompraSegura.evidencia;

import java.io.*;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ValidadorImagemEvidencia {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 20_000_000;

    public ImagemEvidencia validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) throw new IllegalArgumentException("Escolha uma imagem da tela do *#06#.");
        if (arquivo.getSize() > MAX_BYTES) throw new IllegalArgumentException("A imagem deve ter no máximo 5 MB.");
        try (var entrada = new MemoryCacheImageInputStream(arquivo.getInputStream())) {
            var readers = ImageIO.getImageReaders(entrada);
            if (!readers.hasNext()) throw new IllegalArgumentException("Envie uma imagem válida em PNG ou JPEG.");
            var reader = readers.next();
            try {
                String formato = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!formato.equals("png") && !formato.equals("jpeg") && !formato.equals("jpg")) {
                    throw new IllegalArgumentException("Use uma imagem PNG ou JPEG.");
                }
                reader.setInput(entrada, true, true);
                int largura = reader.getWidth(0), altura = reader.getHeight(0);
                if (largura <= 0 || altura <= 0 || largura > 8000 || altura > 8000 || (long) largura * altura > MAX_PIXELS) {
                    throw new IllegalArgumentException("A imagem é grande demais. Use até 8.000 pixels por lado e 20 megapixels.");
                }
                var imagem = reader.read(0);
                try (var saida = new ByteArrayOutputStream()) {
                    // Regrava somente os pixels; nao confia em extensao, MIME ou metadados enviados.
                    boolean png = formato.equals("png");
                    if (!ImageIO.write(imagem, png ? "png" : "jpeg", saida)) {
                        throw new IllegalArgumentException("Não foi possível processar a imagem. Use PNG ou JPEG.");
                    }
                    if (saida.size() > MAX_BYTES) throw new IllegalArgumentException("Reduza a imagem: após o processamento ela ultrapassa 5 MB.");
                    return new ImagemEvidencia(saida.toByteArray(), png ? "image/png" : "image/jpeg");
                } finally { imagem.flush(); }
            } finally { reader.dispose(); }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Não foi possível ler essa imagem. Escolha outro arquivo PNG ou JPEG.");
        }
    }
}
