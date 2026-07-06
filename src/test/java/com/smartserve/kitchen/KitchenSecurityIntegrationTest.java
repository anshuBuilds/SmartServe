package com.smartserve.kitchen;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartserve.TestcontainersConfiguration;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.entity.Restaurant;
import com.smartserve.restaurant.repository.BranchRepository;
import com.smartserve.restaurant.repository.RestaurantRepository;
import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.enums.Role;
import com.smartserve.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class KitchenSecurityIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired RestaurantRepository restaurantRepository;
    @Autowired BranchRepository branchRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Branch branch;

    @BeforeEach
    void setUp() {
        Restaurant restaurant = new Restaurant();
        restaurant.setName("Kitchen Security Restaurant " + System.nanoTime());
        restaurant.setOwnerName("Owner");
        restaurant.setActive(true);
        restaurant = restaurantRepository.save(restaurant);

        branch = new Branch();
        branch.setRestaurant(restaurant);
        branch.setName("Main Kitchen");
        branch.setAddress("Test Address");
        branch.setPhone("9999999999");
        branch.setActive(true);
        branch = branchRepository.save(branch);
    }

    @Test
    void kitchenUserReadsOnlyAssignedQueueWithoutSupplyingBranch() throws Exception {
        createUser("kitchen_assigned", Role.KITCHEN, branch);
        String token = login("kitchen_assigned");

        mockMvc.perform(get("/api/kitchen/tickets").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.pending").value(0));
    }

    @Test
    void unassignedKitchenUserGetsConfigurationForbidden() throws Exception {
        createUser("kitchen_unassigned", Role.KITCHEN, null);
        String token = login("kitchen_unassigned");

        mockMvc.perform(get("/api/kitchen/tickets").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Kitchen user is not assigned to a branch"));
    }

    @Test
    void waiterCannotUseKitchenEndpoints() throws Exception {
        createUser("kitchen_waiter", Role.WAITER, branch);
        String token = login("kitchen_waiter");

        mockMvc.perform(get("/api/kitchen/tickets").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerMustChooseAnExistingBranch() throws Exception {
        createUser("kitchen_manager", Role.MANAGER, null);
        String token = login("kitchen_manager");

        mockMvc.perform(get("/api/kitchen/tickets").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/kitchen/tickets").param("branchId", branch.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void createUser(String username, Role role, Branch assignedBranch) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("password1"));
        user.setFullName(username);
        user.setRole(role);
        user.setActive(true);
        user.setBranch(assignedBranch);
        userRepository.save(user);
    }

    private String login(String username) throws Exception {
        String json = "{\"username\":\"" + username + "\",\"password\":\"password1\"}";
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(body);
        return response.path("data").path("token").asText();
    }
}
