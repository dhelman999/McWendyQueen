package com.mcwendyqueen.controller;

import com.mcwendyqueen.model.condiment.CondimentItem;
import com.mcwendyqueen.model.condiment.CondimentItemRequestDTO;
import com.mcwendyqueen.model.condiment.CondimentItemResponseDTO;
import com.mcwendyqueen.model.ModelMapperUtils;
import com.mcwendyqueen.service.condiment.CondimentItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.mcwendyqueen.ApiConstants.API_BASE_PATH;
import static com.mcwendyqueen.ApiConstants.CONDIMENT_PATH;
import static com.mcwendyqueen.ApiConstants.V1_PATH;

@Slf4j
@RestController
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

    @PostMapping(CONDIMENT_PATH)
    @Operation(summary = "Create a new condiment", description = "Creates and saves a new condiment.")
    @ApiResponse(responseCode = "200", description = "Created condiment returned successfully")
    public ResponseEntity<CondimentItemResponseDTO> createCondiment(CondimentItemRequestDTO newCondiment) {
        CondimentItem createdCondiment = condimentItemService.createCondimentItem(newCondiment);
        CondimentItemResponseDTO condimentResponse = ModelMapperUtils.GetCondimentItemResponseDTO(createdCondiment);

        return new ResponseEntity<>(condimentResponse, HttpStatus.OK);
    }

    @DeleteMapping(CONDIMENT_PATH)
    @Operation(summary = "Deletes a condiment", description = "Deletes and returns the condiment.")
    @ApiResponse(responseCode = "200", description = "Delete condiment returned successfully")
    public ResponseEntity<CondimentItemResponseDTO> deleteCondiment(CondimentItemRequestDTO condiment) {
        CondimentItem deletedCondiment = condimentItemService.deleteCondimentItem(condiment);
        CondimentItemResponseDTO condimentResponse = ModelMapperUtils.GetCondimentItemResponseDTO(deletedCondiment);

        return new ResponseEntity<>(condimentResponse, HttpStatus.OK);
    }
}
