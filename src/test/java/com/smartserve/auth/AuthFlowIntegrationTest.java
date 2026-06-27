package com.smartserve.auth;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.enums.Role;
import com.smartserve.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin12345";
    private static final String WAITER_USERNAME = "auth_flow_waiter";
    private static final String WAITER_PASSWORD = "waiter12345";
    private static final String CREATED_USERNAME = "auth_flow_created_user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpUsers() {
        userRepository.findByUsername(CREATED_USERNAME).ifPresent(userRepository::delete);
        upsertUser(ADMIN_USERNAME, ADMIN_PASSWORD, "System Admin", Role.ADMIN);
        upsertUser(WAITER_USERNAME, WAITER_PASSWORD, "Auth Flow Waiter", Role.WAITER);
    }

    @Test
    void loginWithAdminCredentialsReturnsJwt() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(ADMIN_USERNAME, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(not(blankOrNullString())))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    @Test
    void getCurrentUserWorksWithJwt() throws Exception {
        String token = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void getCurrentUserFailsWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void adminCanCreateUser() throws Exception {
        String token = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(CREATED_USERNAME, "created12345", "Created User", "WAITER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(CREATED_USERNAME))
                .andExpect(jsonPath("$.data.fullName").value("Created User"))
                .andExpect(jsonPath("$.data.role").value("WAITER"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void nonAdminCannotCreateUser() throws Exception {
        String token = loginAndGetToken(WAITER_USERNAME, WAITER_PASSWORD);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(CREATED_USERNAME, "created12345", "Created User", "WAITER")))
                .andExpect(status().isForbidden());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("data").path("token").asText();
    }

    private void upsertUser(String username, String password, String fullName, Role role) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseGet(UserEntity::new);

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(true);

        userRepository.save(user);
    }

    private String loginJson(String username, String password) {
        return """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);
    }

    private String createUserJson(String username, String password, String fullName, String role) {
        return """
                {
                  "username": "%s",
                  "password": "%s",
                  "fullName": "%s",
                  "role": "%s"
                }
                """.formatted(username, password, fullName, role);
    }
}
