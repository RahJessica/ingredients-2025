package td5springingredientapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import td5springingredientapp.entity.StockValue;
import td5springingredientapp.repository.IngredientRepository;
import td5springingredientapp.entity.Ingredient;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientRepository ingredientRepository;

    public IngredientController(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    @GetMapping
    public ResponseEntity<List<Ingredient>> getAllIngredients() {
        List<Ingredient> ingredients = ingredientRepository.findAllIngredients();
        return ResponseEntity.ok(ingredients);
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<?> getIngredientStock(
            @PathVariable("id") Integer id,
            @RequestParam(value = "at") String temporal,
            @RequestParam(value = "unit") String unit) {

        Ingredient ingredient = ingredientRepository.findById(id);

        if (ingredient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Ingredient.id=" + id + " is not found");
        }

        Instant atInstant;
        try {
            atInstant = Instant.parse(temporal);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid 'at' parameter. Use ISO-8601 format like 2026-03-30T15:00:00Z");
        }

        StockValue stockValue = ingredient.getStockValueAt(atInstant);

        if (stockValue == null) {
            return ResponseEntity.ok(new StockResponse(ingredient.getId(), 0.0, unit));
        }

        return ResponseEntity.ok(new StockResponse(ingredient.getId(), stockValue.getQuantity(), stockValue.getUnit().name()));
    }

    static class StockResponse {
        public Integer id;
        public Double quantity;
        public String unit;

        public StockResponse(Integer id, Double quantity, String unit) {
            this.id = id;
            this.quantity = quantity;
            this.unit = unit;
        }
    }
}