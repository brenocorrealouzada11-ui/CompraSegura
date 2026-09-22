package CompraSegura.config;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class SegurancaConfig {
    @Bean
    UserDetailsService userDetailsService() {
        // Evita gerar uma conta temporaria enquanto o login nao esta implementado.
        return new InMemoryUserDetailsManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        String algoritmo = "pbkdf2@SpringSecurity_v5_8";
        return new DelegatingPasswordEncoder(algoritmo,
                Map.of(algoritmo, Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Cadastro publico com CSRF ativo. Login sera implementado na proxima etapa.
        return http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/index.html", "/css/**", "/cadastro", "/cadastro/sucesso", "/error").permitAll()
                    .anyRequest().denyAll())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .build();
    }
}
