package CompraSegura.config;
import java.time.Clock;
import org.springframework.context.annotation.*;
@Configuration
public class RelogioConfig {
    @Bean Clock relogio() { return Clock.systemDefaultZone(); }
}
