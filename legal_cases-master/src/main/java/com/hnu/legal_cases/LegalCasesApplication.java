package com.hnu.legal_cases;

import com.hnu.legal_cases.config.AdminProperties;
import com.hnu.legal_cases.config.CrawlerProperties;
import com.hnu.legal_cases.config.PaymentProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.hnu.legal_cases.dao")
@EnableConfigurationProperties({PaymentProperties.class, AdminProperties.class, CrawlerProperties.class})
public class LegalCasesApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegalCasesApplication.class, args);
    }

}
