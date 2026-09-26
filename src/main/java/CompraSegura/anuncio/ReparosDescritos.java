package CompraSegura.anuncio;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReparosDescritosValidator.class)
public @interface ReparosDescritos {
    String message() default "Descreva os reparos e alterações nas observações com pelo menos 20 caracteres.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
