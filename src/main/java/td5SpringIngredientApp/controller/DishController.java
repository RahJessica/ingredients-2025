package td5springingredientapp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import td5springingredientapp.entity.Dish;
import td5springingredientapp.entity.DishIngredient;
import td5springingredientapp.entity.Ingredient;
import td5springingredientapp.repository.DishRepository;

import java.util.List;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishRepository dishRepository;

    public DishController(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @GetMapping
    public ResponseEntity<List<Dish>> getAllDishes(
            @RequestParam(required = false) Double priceUnder,
            @RequestParam(required = false) Double priceOver,
            @RequestParam(required = false) String name) {

        List<Dish> dishes;

        if (priceUnder == null && priceOver == null && (name == null || name.isEmpty())) {
            dishes = dishRepository.findAllDishes();
        } else {
            dishes = dishRepository.findDishesFiltered(priceUnder, priceOver, name);
        }

        return ResponseEntity.ok(dishes);
    }

    @PutMapping("/{id}/ingredients")
    public ResponseEntity<?> updateDishIngredients(
            @PathVariable("id") Integer id,
            @RequestBody(required = false) List<DishIngredient> ingredients) {

        if (ingredients == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Request body is mandatory");
        }

        Dish dish;
        try {
            dish = dishRepository.findDishById(id);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Dish.id=" + id + " is not found");
        }

        for (DishIngredient di : ingredients) {
            di.setDish(dish);
            if (di.getIngredient() != null) {
                di.setIngredient(new Ingredient(di.getIngredient().getId()));
            }
        }

        dish.setDishIngredients(ingredients);
        Dish updatedDish = dishRepository.save(dish);
        return ResponseEntity.ok(updatedDish);
    }

    @PostMapping
    public ResponseEntity<?> createDishes(@RequestBody List<Dish> newDishes) {
        if (newDishes == null || newDishes.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Request body must contain at least one dish");
        }

        try {
            List<Dish> createdDishes = dishRepository.createDishes(newDishes);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDishes);

        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("Dish.name=")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal error: " + e.getMessage());
        }
    }
}