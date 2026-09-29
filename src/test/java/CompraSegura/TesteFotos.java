package CompraSegura;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.springframework.mock.web.MockMultipartFile;
final class TesteFotos {
    static MockMultipartFile arquivo(String nome) {
        try (var saida = new ByteArrayOutputStream()) {
            var imagem = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
            try { ImageIO.write(imagem, "png", saida); }
            finally { imagem.flush(); }
            return new MockMultipartFile(nome, "foto.png", "image/png", saida.toByteArray());
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }
    static CompraSegura.evidencia.ImagemEvidencia imagem() {
        try { return new CompraSegura.evidencia.ImagemEvidencia(arquivo("fotos").getBytes(), "image/png"); }
        catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }
}
