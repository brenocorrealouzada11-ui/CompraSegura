package CompraSegura.config;

import java.util.Map;
import java.util.Locale;
import CompraSegura.usuario.UsuarioRepository;
import CompraSegura.usuario.UsuarioAutenticado;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
public class SegurancaConfig {
    @Bean
    UserDetailsService userDetailsService(UsuarioRepository usuarios) {
        return email -> usuarios.findByEmail(email.strip().toLowerCase(Locale.ROOT))
                .map(UsuarioAutenticado::new)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        String algoritmo = "pbkdf2@SpringSecurity_v5_8";
        return new DelegatingPasswordEncoder(algoritmo,
                Map.of(algoritmo, Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioRepository usuarios) throws Exception {
        // O Spring Security verifica o hash e administra a sessao, mantendo CSRF ativo.
        return http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/cadastro", "/cadastro/sucesso", "/login", "/error").permitAll()
                    .requestMatchers("/minha-conta", "/minha-conta/**").authenticated()
                    .anyRequest().denyAll())
                .formLogin(form -> form.loginPage("/login")
                        .usernameParameter("email").passwordParameter("senha")
                        .defaultSuccessUrl("/minha-conta", true)
                        .failureUrl("/login?erro").permitAll())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(new SessaoValidaFilter(usuarios), AuthorizationFilter.class)
                .logout(logout -> logout.logoutUrl("/sair")
                        .logoutSuccessUrl("/login?saiu")
                        .invalidateHttpSession(true).clearAuthentication(true)
                        .deleteCookies("JSESSIONID").permitAll())
                .build();
    }
}
