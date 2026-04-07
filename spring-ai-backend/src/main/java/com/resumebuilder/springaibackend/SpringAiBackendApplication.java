package com.resumebuilder.springaibackend;

import com.resumebuilder.springaibackend.config.AppProperties;
import com.resumebuilder.springaibackend.config.MySqlDataSourceProperties;
import com.resumebuilder.springaibackend.config.PgVectorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
// author: jf
@EnableConfigurationProperties({AppProperties.class, MySqlDataSourceProperties.class, PgVectorProperties.class})
public class SpringAiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiBackendApplication.class, args);
    }
}
