package com.smartserve.restaurant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartserve.TestcontainersConfiguration;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.entity.Restaurant;
import com.smartserve.restaurant.entity.RestaurantTable;
import com.smartserve.restaurant.enums.TableStatus;
import com.smartserve.restaurant.repository.BranchRepository;
import com.smartserve.restaurant.repository.RestaurantRepository;
import com.smartserve.restaurant.repository.RestaurantTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RestaurantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private RestaurantTableRepository tableRepository;

    @BeforeEach
    void cleanDatabase() {
        tableRepository.deleteAll();
        branchRepository.deleteAll();
        restaurantRepository.deleteAll();
    }

    /*
     * Restaurant tests
     */

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateRestaurant() throws Exception {
        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restaurantJson(
                                "SmartServe Foods",
                                "Anshu Yadav"
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Restaurant created"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name")
                        .value("SmartServe Foods"))
                .andExpect(jsonPath("$.data.ownerName")
                        .value("Anshu Yadav"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCannotCreateRestaurant() throws Exception {
        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restaurantJson(
                                "SmartServe Foods",
                                "Anshu Yadav"
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateRestaurantNameIsRejectedIgnoringCase()
            throws Exception {
        restaurantRepository.save(
                restaurant("SmartServe Foods")
        );

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restaurantJson(
                                "smartserve foods",
                                "Another Owner"
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Restaurant already exists"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidRestaurantRequestReturnsValidationErrors()
            throws Exception {
        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "ownerName": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name")
                        .exists())
                .andExpect(jsonPath("$.validationErrors.ownerName")
                        .exists());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanListRestaurants() throws Exception {
        restaurantRepository.save(
                restaurant("SmartServe Foods")
        );

        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name")
                        .value("SmartServe Foods"));
    }

    /*
     * Branch tests
     */

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateBranch() throws Exception {
        Restaurant restaurant = restaurantRepository.save(
                restaurant("SmartServe Foods")
        );

        mockMvc.perform(post(
                        "/api/restaurants/{restaurantId}/branches",
                        restaurant.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchJson("Delhi Central")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name")
                        .value("Delhi Central"))
                .andExpect(jsonPath("$.data.restaurantId")
                        .value(restaurant.getId()))
                .andExpect(jsonPath("$.data.restaurantName")
                        .value("SmartServe Foods"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void branchCreationFailsForMissingRestaurant()
            throws Exception {
        mockMvc.perform(post(
                        "/api/restaurants/{restaurantId}/branches",
                        999999L
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchJson("Delhi Central")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Restaurant not found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateBranchNameInSameRestaurantIsRejected()
            throws Exception {
        Restaurant restaurant = restaurantRepository.save(
                restaurant("SmartServe Foods")
        );

        branchRepository.save(
                branch(restaurant, "Delhi Central")
        );

        mockMvc.perform(post(
                        "/api/restaurants/{restaurantId}/branches",
                        restaurant.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchJson("delhi central")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(
                                "Branch already exists in this restaurant"
                        ));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sameBranchNameIsAllowedInDifferentRestaurants()
            throws Exception {
        Restaurant firstRestaurant =
                restaurantRepository.save(
                        restaurant("SmartServe Foods")
                );

        Restaurant secondRestaurant =
                restaurantRepository.save(
                        restaurant("Another Restaurant")
                );

        branchRepository.save(
                branch(firstRestaurant, "Downtown")
        );

        mockMvc.perform(post(
                        "/api/restaurants/{restaurantId}/branches",
                        secondRestaurant.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchJson("Downtown")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.restaurantId")
                        .value(secondRestaurant.getId()));
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void waiterCanGetBranch() throws Exception {
        Restaurant restaurant = restaurantRepository.save(
                restaurant("SmartServe Foods")
        );

        Branch branch = branchRepository.save(
                branch(restaurant, "Delhi Central")
        );

        mockMvc.perform(get(
                        "/api/branches/{branchId}",
                        branch.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(branch.getId()))
                .andExpect(jsonPath("$.data.name")
                        .value("Delhi Central"));
    }

    /*
     * Table tests
     */

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanCreateTableAndItDefaultsToAvailable()
            throws Exception {
        Branch branch = savedBranch();

        mockMvc.perform(post(
                        "/api/branches/{branchId}/tables",
                        branch.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tableJson("T1", 4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.branchId")
                        .value(branch.getId()))
                .andExpect(jsonPath("$.data.tableNumber")
                        .value("T1"))
                .andExpect(jsonPath("$.data.capacity")
                        .value(4))
                .andExpect(jsonPath("$.data.status")
                        .value("AVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void waiterCannotCreateTable() throws Exception {
        Branch branch = savedBranch();

        mockMvc.perform(post(
                        "/api/branches/{branchId}/tables",
                        branch.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tableJson("T1", 4)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void duplicateTableNumberInSameBranchIsRejected()
            throws Exception {
        Branch branch = savedBranch();

        tableRepository.save(
                table(branch, "T1", TableStatus.AVAILABLE)
        );

        mockMvc.perform(post(
                        "/api/branches/{branchId}/tables",
                        branch.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tableJson("t1", 4)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(
                                "Table number already exists in this branch"
                        ));
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void waiterCanListTables() throws Exception {
        Branch branch = savedBranch();

        tableRepository.save(
                table(branch, "T1", TableStatus.AVAILABLE)
        );

        tableRepository.save(
                table(branch, "T2", TableStatus.OCCUPIED)
        );

        mockMvc.perform(get(
                        "/api/branches/{branchId}/tables",
                        branch.getId()
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].tableNumber")
                        .value("T1"))
                .andExpect(jsonPath("$.data[1].tableNumber")
                        .value("T2"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanUpdateTableStatus() throws Exception {
        Branch branch = savedBranch();

        RestaurantTable table = tableRepository.save(
                table(branch, "T1", TableStatus.AVAILABLE)
        );

        mockMvc.perform(patch(
                        "/api/branches/{branchId}/tables/{tableId}/status",
                        branch.getId(),
                        table.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "OCCUPIED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Table status updated"))
                .andExpect(jsonPath("$.data.status")
                        .value("OCCUPIED"));
    }

    @Test
    @WithMockUser(roles = "WAITER")
    void waiterCannotUpdateTableStatus() throws Exception {
        Branch branch = savedBranch();

        RestaurantTable table = tableRepository.save(
                table(branch, "T1", TableStatus.AVAILABLE)
        );

        mockMvc.perform(patch(
                        "/api/branches/{branchId}/tables/{tableId}/status",
                        branch.getId(),
                        table.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "OCCUPIED"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void tableCannotBeUpdatedThroughAnotherBranch()
            throws Exception {
        Restaurant restaurant = restaurantRepository.save(
                restaurant("SmartServe Foods")
        );

        Branch actualBranch = branchRepository.save(
                branch(restaurant, "Delhi Central")
        );

        Branch otherBranch = branchRepository.save(
                branch(restaurant, "Delhi Airport")
        );

        RestaurantTable table = tableRepository.save(
                table(
                        actualBranch,
                        "T1",
                        TableStatus.AVAILABLE
                )
        );

        mockMvc.perform(patch(
                        "/api/branches/{branchId}/tables/{tableId}/status",
                        otherBranch.getId(),
                        table.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "OCCUPIED"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Table not found in this branch"));
    }

    /*
     * Test data helpers
     */

    private Branch savedBranch() {
        Restaurant restaurant = restaurantRepository.save(
                restaurant("SmartServe Foods")
        );

        return branchRepository.save(
                branch(restaurant, "Delhi Central")
        );
    }

    private Restaurant restaurant(String name) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(name);
        restaurant.setOwnerName("Test Owner");
        restaurant.setActive(true);
        return restaurant;
    }

    private Branch branch(
            Restaurant restaurant,
            String name
    ) {
        Branch branch = new Branch();
        branch.setRestaurant(restaurant);
        branch.setName(name);
        branch.setAddress("Test Address");
        branch.setPhone("9876543210");
        branch.setActive(true);
        return branch;
    }

    private RestaurantTable table(
            Branch branch,
            String tableNumber,
            TableStatus status
    ) {
        RestaurantTable table = new RestaurantTable();
        table.setBranch(branch);
        table.setTableNumber(tableNumber);
        table.setCapacity(4);
        table.setStatus(status);
        return table;
    }

    private String restaurantJson(
            String name,
            String ownerName
    ) {
        return """
                {
                  "name": "%s",
                  "ownerName": "%s"
                }
                """.formatted(name, ownerName);
    }

    private String branchJson(String name) {
        return """
                {
                  "name": "%s",
                  "address": "Connaught Place, New Delhi",
                  "phone": "+91 98765 43210"
                }
                """.formatted(name);
    }

    private String tableJson(
            String tableNumber,
            int capacity
    ) {
        return """
                {
                  "tableNumber": "%s",
                  "capacity": %d
                }
                """.formatted(tableNumber, capacity);
    }
}