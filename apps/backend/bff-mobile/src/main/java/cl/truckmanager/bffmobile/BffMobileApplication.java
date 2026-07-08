package cl.truckmanager.bffmobile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BffMobileApplication {
    public static void main(String[] args) {
        SpringApplication.run(BffMobileApplication.class, args);
    }
}
