package rca.ac.rw.template.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public String someString() {
        return "YourFixedStringValue";
    }
}
