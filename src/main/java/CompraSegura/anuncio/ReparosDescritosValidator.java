package CompraSegura.anuncio;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ReparosDescritosValidator implements ConstraintValidator<ReparosDescritos, AnuncioForm> {
    @Override
    public boolean isValid(AnuncioForm form, ConstraintValidatorContext context) {
        if (form == null || form.getReparos() != HistoricoReparos.COM_REPAROS) return true;
        String descricao = form.getDescricao();
        if (descricao != null && descricao.strip().length() >= 20) return true;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
            .addPropertyNode("descricao").addConstraintViolation();
        return false;
    }
}
