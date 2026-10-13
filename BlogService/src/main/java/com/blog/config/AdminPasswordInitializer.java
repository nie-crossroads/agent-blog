package com.blog.config;

import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminPasswordInitializer {
    private static final Logger log = LoggerFactory.getLogger(AdminPasswordInitializer.class);

    @Bean
    public ApplicationRunner resetAdminPasswordIfNeeded(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            @Value("${blog.admin.reset-password}") String resetPassword,
            @Value("${blog.admin.username}") String username) {
        return args -> {
            User admin = userMapper.findByUsername(username);
            String encoded = passwordEncoder.encode(resetPassword);
            if (admin == null) {
                admin = new User();
                admin.setUsername(username);
                admin.setPassword(encoded);
                admin.setRole(32);
                admin.setDisable(false);
                userMapper.insert(admin);
                log.info("admin user created by blog.admin.reset-password");
                return;
            }
            userMapper.updatePassword(admin.getId(), encoded);
            log.info("admin password has been reset by blog.admin.reset-password");
        };
    }
}
