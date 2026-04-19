package com.taejin.authservice.config;

import com.taejin.authservice.entity.Role;
import com.taejin.authservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 기동 시 기본 Role(ROLE_USER, ROLE_ADMIN)을 보장한다.
 * RBAC에서 참조하는 역할이 항상 존재하도록 seeding.
 *
 * ApplicationReadyEvent 리스너로 구현한 이유:
 * - @SpringBootTest(MockMvc)에서도 컨텍스트 ready 시점에 확실히 실행된다.
 * - EntityManager/트랜잭션 매니저가 완전히 초기화된 이후 실행되어 안전.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleSeeder {

    private final RoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDefaultRoles() {
        ensure("ROLE_USER");
        ensure("ROLE_ADMIN");
    }

    private void ensure(String name) {
        roleRepository.findByName(name).orElseGet(() -> {
            log.info("Seeding role {}", name);
            return roleRepository.save(Role.of(name));
        });
    }
}
