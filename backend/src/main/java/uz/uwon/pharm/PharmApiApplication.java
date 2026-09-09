package uz.uwon.pharm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PharmApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PharmApiApplication.class, args);
    }
}
