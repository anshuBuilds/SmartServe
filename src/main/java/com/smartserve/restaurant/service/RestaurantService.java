package com.smartserve.restaurant.service;

import com.smartserve.common.exception.BadRequestException;
import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.restaurant.dto.BranchResponse;
import com.smartserve.restaurant.dto.CreateBranchRequest;
import com.smartserve.restaurant.dto.CreateRestaurantRequest;
import com.smartserve.restaurant.dto.CreateTableRequest;
import com.smartserve.restaurant.dto.RestaurantResponse;
import com.smartserve.restaurant.dto.TableResponse;
import com.smartserve.restaurant.dto.UpdateTableStatusRequest;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.entity.Restaurant;
import com.smartserve.restaurant.entity.RestaurantTable;
import com.smartserve.restaurant.enums.TableStatus;
import com.smartserve.restaurant.repository.BranchRepository;
import com.smartserve.restaurant.repository.RestaurantRepository;
import com.smartserve.restaurant.repository.RestaurantTableRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final RestaurantTableRepository tableRepository;

    /*
     * Restaurant operations
     */

    public RestaurantResponse createRestaurant(
            CreateRestaurantRequest request
    ) {
        String name = request.name().trim();
        String ownerName = request.ownerName().trim();

        if (restaurantRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException(
                    "Restaurant already exists"
            );
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setName(name);
        restaurant.setOwnerName(ownerName);
        restaurant.setActive(true);

        Restaurant savedRestaurant =
                restaurantRepository.save(restaurant);

        return toRestaurantResponse(savedRestaurant);
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getRestaurants() {
        return restaurantRepository.findAll()
                .stream()
                .map(this::toRestaurantResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant(Long restaurantId) {
        return toRestaurantResponse(
                findRestaurant(restaurantId)
        );
    }

    /*
     * Branch operations
     */

    public BranchResponse createBranch(
            Long restaurantId,
            CreateBranchRequest request
    ) {
        Restaurant restaurant = findRestaurant(restaurantId);
        String name = request.name().trim();

        boolean branchExists =
                branchRepository
                        .existsByRestaurantIdAndNameIgnoreCase(
                                restaurantId,
                                name
                        );

        if (branchExists) {
            throw new BadRequestException(
                    "Branch already exists in this restaurant"
            );
        }

        Branch branch = new Branch();
        branch.setRestaurant(restaurant);
        branch.setName(name);
        branch.setAddress(request.address().trim());
        branch.setPhone(request.phone().trim());
        branch.setActive(true);

        Branch savedBranch = branchRepository.save(branch);

        return toBranchResponse(savedBranch);
    }

    @Transactional(readOnly = true)
    public BranchResponse getBranch(Long branchId) {
        return toBranchResponse(findBranch(branchId));
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getBranches(Long restaurantId) {
        // Distinguishes "restaurant not found" from "no branches".
        findRestaurant(restaurantId);

        return branchRepository
                .findByRestaurantIdOrderByNameAsc(restaurantId)
                .stream()
                .map(this::toBranchResponse)
                .toList();
    }

    /*
     * Table operations
     */

    public TableResponse createTable(
            Long branchId,
            CreateTableRequest request
    ) {
        Branch branch = findBranch(branchId);
        String tableNumber = request.tableNumber().trim();

        boolean tableExists =
                tableRepository
                        .existsByBranchIdAndTableNumberIgnoreCase(
                                branchId,
                                tableNumber
                        );

        if (tableExists) {
            throw new BadRequestException(
                    "Table number already exists in this branch"
            );
        }

        RestaurantTable table = new RestaurantTable();
        table.setBranch(branch);
        table.setTableNumber(tableNumber);
        table.setCapacity(request.capacity());
        table.setStatus(TableStatus.AVAILABLE);

        RestaurantTable savedTable =
                tableRepository.save(table);

        return toTableResponse(savedTable);
    }

    public List<TableResponse> getTables(Long branchId) {
        // Distinguishes "branch not found" from "no tables".
        findBranch(branchId);

        return tableRepository
                .findByBranchIdOrderByTableNumberAsc(branchId)
                .stream().peek(this::ensureQrToken)
                .map(this::toTableResponse)
                .toList();
    }

    public TableResponse updateTableStatus(
            Long branchId,
            Long tableId,
            UpdateTableStatusRequest request
    ) {
        RestaurantTable table =
                tableRepository
                        .findByIdAndBranchId(tableId, branchId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Table not found in this branch"
                                )
                        );

        table.setStatus(request.status());

        /*
         * No explicit save is necessary here.
         * Hibernate detects the change when the transaction commits.
         */
        return toTableResponse(table);
    }

    public TableResponse rotateTableQrToken(Long branchId, Long tableId) {
        RestaurantTable table = tableRepository.findByIdAndBranchId(tableId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found in this branch"));
        table.setQrToken(newQrToken());
        return toTableResponse(table);
    }

    private void ensureQrToken(RestaurantTable table) {
        if (table.getQrToken() == null || table.getQrToken().isBlank()) table.setQrToken(newQrToken());
    }
    private String newQrToken() { return UUID.randomUUID().toString().replace("-", ""); }

    /*
     * Entity lookup helpers
     */

    private Restaurant findRestaurant(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Restaurant not found"
                        )
                );
    }

    private Branch findBranch(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Branch not found"
                        )
                );
    }

    /*
     * Entity-to-response mapping
     */

    private RestaurantResponse toRestaurantResponse(
            Restaurant restaurant
    ) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getOwnerName(),
                restaurant.getActive(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt()
        );
    }

    private BranchResponse toBranchResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getRestaurant().getId(),
                branch.getRestaurant().getName(),
                branch.getName(),
                branch.getAddress(),
                branch.getPhone(),
                branch.getActive(),
                branch.getCreatedAt(),
                branch.getUpdatedAt()
        );
    }

    private TableResponse toTableResponse(
            RestaurantTable table
    ) {
        return new TableResponse(
                table.getId(),
                table.getBranch().getId(),
                table.getTableNumber(),
                table.getCapacity(),
                table.getStatus(),
                table.getQrToken(),
                table.getCreatedAt(),
                table.getUpdatedAt()
        );
    }
}
