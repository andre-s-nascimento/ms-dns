package net.ddns.adambravo79.nextdns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
// O Spring Boot por padrão já escaneia todos os subpacotes (domain, repository, etc)
public class NextDnsApplication {
    public static void main(String[] args) {
        SpringApplication.run(NextDnsApplication.class, args);
    }
}