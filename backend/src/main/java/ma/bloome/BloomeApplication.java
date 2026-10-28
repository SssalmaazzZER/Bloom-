package ma.bloome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BloomeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BloomeApplication.class, args);
    }
}
