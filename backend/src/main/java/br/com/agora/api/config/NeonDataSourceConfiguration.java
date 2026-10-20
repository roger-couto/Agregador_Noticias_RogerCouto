package br.com.agora.api.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Adapto a URI PostgreSQL que o Neon fornece ao formato JDBC usado pelo back-end Java. */
@Configuration
public class NeonDataSourceConfiguration {

    @Bean
    @Primary
    public DataSource dataSource(Environment environment, DataSourceProperties properties) {
        // Uma URL JDBC explícita continua tendo prioridade (por exemplo, no Docker Compose).
        String jdbcUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        if (jdbcUrl != null && !jdbcUrl.isBlank()) {
            return construirDataSource(
                    jdbcUrl,
                    environment.getProperty("SPRING_DATASOURCE_USERNAME", properties.getUsername()),
                    environment.getProperty("SPRING_DATASOURCE_PASSWORD", properties.getPassword())
            );
        }

        // DATABASE_URL é a conexão agrupada da Neon, indicada para as consultas normais da aplicação.
        String neonUrl = environment.getProperty("DATABASE_URL");
        if (neonUrl != null && !neonUrl.isBlank()) {
            return construirDataSource(neonUrl, null, null);
        }

        // Sem Neon, preservo a configuração PostgreSQL local que o projeto já utilizava.
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    private DataSource construirDataSource(String url, String username, String password) {
        url = limparAspas(url);
        HikariDataSource dataSource = new HikariDataSource();
        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            aplicarUriNeon(dataSource, url);
        } else {
            dataSource.setJdbcUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
        }
        dataSource.setMaximumPoolSize(5);
        return dataSource;
    }

    private void aplicarUriNeon(HikariDataSource dataSource, String url) {
        URI uri = URI.create(url);
        if (uri.getHost() == null || uri.getPath() == null || uri.getPath().length() < 2) {
            throw new IllegalArgumentException("A variável DATABASE_URL da Neon não tem o formato esperado.");
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() >= 0) {
            jdbcUrl.append(':').append(uri.getPort());
        }
        jdbcUrl.append(uri.getRawPath());
        if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
            jdbcUrl.append('?').append(uri.getRawQuery());
        }
        dataSource.setJdbcUrl(jdbcUrl.toString());

        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            String[] credentials = userInfo.split(":", 2);
            dataSource.setUsername(decodificar(credentials[0]));
            if (credentials.length == 2) {
                dataSource.setPassword(decodificar(credentials[1]));
            }
        }
    }

    private String decodificar(String valor) {
        // URLDecoder trata '+' como espaço; preservo-o se fizer parte de uma credencial URI.
        return URLDecoder.decode(valor.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private String limparAspas(String valor) {
        String resultado = valor.trim();
        if (resultado.length() >= 2) {
            char primeira = resultado.charAt(0);
            char ultima = resultado.charAt(resultado.length() - 1);
            if ((primeira == '"' && ultima == '"') || (primeira == '\'' && ultima == '\'')) {
                return resultado.substring(1, resultado.length() - 1);
            }
        }
        return resultado;
    }
}
