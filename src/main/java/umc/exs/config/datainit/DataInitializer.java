package umc.exs.config.datainit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final TestSeedService testSeedService;
    private final StressSeedService stressSeedService;

    @Value("${app.seed.mode:test}")
    private String mode;

    @Override
    public void run(String... args) {

        log.info(" Seed mode ativo: {}", mode);

        switch (mode.toLowerCase()) {
            case "stress" -> stressSeedService.run();
            default -> testSeedService.run();
        }
    }
}