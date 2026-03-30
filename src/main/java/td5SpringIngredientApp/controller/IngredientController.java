package td5springingredientapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import td5springingredientapp.repository.IngredientRepository;
import td5springingredientapp.entity.Ingredient;

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
        List<Ingredient> ingredients = ingredientRepository.findAllIngreidents();
        return ResponseEntity.ok(ingredients);
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<?> getIngredientStock(
            @PathVariable("id") Integer id,
            @RequestParam(value = "at", required = false) String temporal,
            @RequestParam(value = "unit", required = false) String unit) {

        if (temporal == null || unit == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Either mandatory query parameter at or unit is not provided");
        }

        List<Ingredient> ingredients = ingredientRepository.createIngredients(List.of()); // remplace par findById
        Ingredient ingredient = ingredients.stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);

        if (ingredient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Ingredient.id=" + id + " is not found");
        }
        return ResponseEntity.ok(new StockResponse(ingredient.getId(), 100.0, unit));
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