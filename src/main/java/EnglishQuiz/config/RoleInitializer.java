package EnglishQuiz.config;

import EnglishQuiz.model.Role;
import EnglishQuiz.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;

    public RoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        ensureRoleExists("ADMIN");
        ensureRoleExists("USER");
    }

    private void ensureRoleExists(String roleName) {
        if (roleRepository.findByNameIgnoreCase(roleName).isPresent()) {
            return;
        }
        Role role = new Role();
        role.setName(roleName);
        roleRepository.save(role);
    }
}
