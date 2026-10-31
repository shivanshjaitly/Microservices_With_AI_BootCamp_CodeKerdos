package in.codekerdos.order.config;

import in.codekerdos.order.entity.AppUser;
import in.codekerdos.order.enums.Role;
import in.codekerdos.order.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner seedUsers(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmail("customer@codekerdos.in")) {
                AppUser customer = new AppUser();
                customer.setEmail("customer@codekerdos.in");
                customer.setPassword(passwordEncoder.encode("cust123"));
                customer.setFullName("Demo Customer");
                customer.setRole(Role.CUSTOMER);
                userRepository.save(customer);
            }
            if (!userRepository.existsByEmail("admin@codekerdos.in")) {
                AppUser admin = new AppUser();
                admin.setEmail("admin@codekerdos.in");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setFullName("Demo Admin");
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
            }
        };
    }
}
