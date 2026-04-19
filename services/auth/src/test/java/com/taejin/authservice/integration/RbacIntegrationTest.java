package com.taejin.authservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taejin.authservice.dto.LoginRequest;
import com.taejin.authservice.dto.SignupRequest;
import com.taejin.authservice.entity.Role;
import com.taejin.authservice.entity.User;
import com.taejin.authservice.repository.RoleRepository;
import com.taejin.authservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC 권한 분리 검증:
 *  - ROLE_USER 가 /api/admin/users 호출 -> 403
 *  - ROLE_ADMIN 이 /api/admin/users 호출 -> 200
 *  - 미인증 호출 -> 401
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;

    @Test
    @DisplayName("미인증 요청은 /api/admin/users 에 접근 시 401")
    void anonymousForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ROLE_USER 는 /api/admin/users 에 접근 시 403")
    void userRoleForbidden() throws Exception {
        String email = "regular@example.com";
        String password = "Password1234";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(email, password, "Regular"))))
                .andExpect(status().isOk());

        String token = loginAndGetAccessToken(email, password);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ROLE_ADMIN 은 /api/admin/users 에 접근 시 200")
    void adminRoleAllowed() throws Exception {
        String email = "admin@example.com";
        String password = "Password1234";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(email, password, "Admin"))))
                .andExpect(status().isOk());

        promoteToAdmin(email);

        String token = loginAndGetAccessToken(email, password);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Transactional
    void promoteToAdmin(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Role admin = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.of("ROLE_ADMIN")));
        user.addRole(admin);
        userRepository.save(user);
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
