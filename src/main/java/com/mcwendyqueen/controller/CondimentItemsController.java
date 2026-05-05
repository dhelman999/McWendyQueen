package com.mcwendyqueen.controller;

import com.mcwendyqueen.model.ModelMapperUtils;
import com.mcwendyqueen.model.condiment.CondimentItem;
import com.mcwendyqueen.model.condiment.CondimentItemRequestDTO;
import com.mcwendyqueen.model.condiment.CondimentItemResponseDTO;
import com.mcwendyqueen.service.condiment.CondimentItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static com.mcwendyqueen.ApiConstants.API_BASE_PATH;
import static com.mcwendyqueen.ApiConstants.CONDIMENT_PATH;
import static com.mcwendyqueen.ApiConstants.NAME_PATH;
import static com.mcwendyqueen.ApiConstants.V1_PATH;

@Slf4j
@RestController
@Validated
@RequestMapping(API_BASE_PATH + V1_PATH)
public class CondimentItemsController {

    private final CondimentItemService condimentItemService;

    public CondimentItemsController(CondimentItemService condimentItemService) {
        this.condimentItemService = condimentItemService;
    }

    @GetMapping(CONDIMENT_PATH)
    @Operation(summary = "List all condiments", description = "Returns all condiments in the system.")
    @ApiResponse(responseCode = "200", description = "Condiments returned successfully")
    public ResponseEntity<List<CondimentItemResponseDTO>> getAllCondiments() {
        List<CondimentItem> allCondiments = condimentItemService.getAllCondiments();

        List<CondimentItemResponseDTO> response = allCondiments.stream()
                .map(ModelMapperUtils::GetCondimentItemResponseDTO)
                .toList();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(CONDIMENT_PATH + "/{condimentId}")
    @Operation(summary = "Get condiment by id", description = "Returns a single condiment by id.")
    @ApiResponse(responseCode = "200", description = "Condiment returned successfully")
    public ResponseEntity<CondimentItemResponseDTO> getCondimentById(@PathVariable @Positive(message = "condimentId must be > 0") Long condimentId) {
        Optional<CondimentItem> existingCondiment = condimentItemService.getCondimentItemById(condimentId);

        if (existingCondiment.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Condiment not found");
        }

        CondimentItemResponseDTO condimentResponse = ModelMapperUtils.GetCondimentItemResponseDTO(existingCondiment.get());

        return new ResponseEntity<>(condimentResponse, HttpStatus.OK);
    }

    @GetMapping(CONDIMENT_PATH + NAME_PATH + "/{condimentName}")
    @Operation(summary = "Get condiment by name", description = "Returns a single condiment by name.")
    @ApiResponse(responseCode = "200", description = "Condiment returned successfully")
    public ResponseEntity<CondimentItemResponseDTO> getCondimentByName(@PathVariable @NotBlank(message = "condimentName is required") String condimentName) {
        Optional<CondimentItem> existingCondiment = condimentItemService.getCondimentByName(condimentName);

        if (existingCondiment.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Condiment not found");
        }

        CondimentItemResponseDTO condimentResponse = ModelMapperUtils.GetCondimentItemResponseDTO(existingCondiment.get());

        return new ResponseEntity<>(condimentResponse, HttpStatus.OK);
    }

    @PostMapping(CONDIMENT_PATH)
    @Operation(summary = "Create a new condiment", description = "Creates and saves a new condiment.")
    @ApiResponse(responseCode = "201", description = "Created condiment returned successfully")
    public ResponseEntity<CondimentItemResponseDTO> createCondiment(@Valid @RequestBody CondimentItemRequestDTO newCondiment) {
        CondimentItem createdCondiment = condimentItemService.createCondimentItem(newCondiment);
        CondimentItemResponseDTO condimentResponse = ModelMapperUtils.GetCondimentItemResponseDTO(createdCondiment);

        return new ResponseEntity<>(condimentResponse, HttpStatus.CREATED);
    }

    @DeleteMapping(CONDIMENT_PATH)
    @Operation(summary = "Deletes a condiment", description = "Deletes and returns the condiment.")
    @ApiResponse(responseCode = "200", description = "Delete condiment returned successfully")
    public ResponseEntity<CondimentItemResponseDTO> deleteCondiment(@Valid @RequestBody CondimentItemRequestDTO condiment) {
        CondimentItem deletedCondiment = condimentItemService.deleteCondimentItem(condiment);

        if (deletedCondiment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Condiment not found");
        }

        CondimentItemResponseDTO condimentResponse = ModelMapperUtils.GetCondimentItemResponseDTO(deletedCondiment);

        return new ResponseEntity<>(condimentResponse, HttpStatus.OK);
    }

    @DeleteMapping(CONDIMENT_PATH + "/{condimentId}")
    @Operation(summary = "Deletes a condiment by id", description = "Deletes and returns the condiment.")
    @ApiResponse(responseCode = "200", description = "Delete condiment returned successfully")
    public ResponseEntity<CondimentItemResponseDTO> deleteCondimentById(@PathVariable @Positive(message = "condimentId must be > 0") Long condimentId) {
        Optional<CondimentItem> deletedCondiment = condimentItemService.deleteCondimentItem(condimentId);

        if (deletedCondiment.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Condiment not found");
        }

        CondimentItemResponseDTO condimentResponse = ModelMapperUtils.GetCondimentItemResponseDTO(deletedCondiment.get());

        return new ResponseEntity<>(condimentResponse, HttpStatus.OK);
    }

    @DeleteMapping(CONDIMENT_PATH + NAME_PATH + "/{condimentName}")
    @Operation(summary = "Deletes a condiment by name", description = "Deletes and returns the condiment.")
    @ApiResponse(responseCode = "200", description = "Delete condiment returned successfully")
    public ResponseEntity<CondimentItemResponseDTO> deleteCondimentByName(@PathVariable @NotBlank(message = "condimentName is required") String condimentName) {
        Optional<CondimentItem> deletedCondiment = condimentItemService.deleteCondimentItem(condimentName);

        if (deletedCondiment.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Condiment not found");
        }

        CondimentItemResponseDTO condimentResponse = ModelMapperUtils.GetCondimentItemResponseDTO(deletedCondiment.get());

        return new ResponseEntity<>(condimentResponse, HttpStatus.OK);
    }
}
