package controller;

import entity.Dish;
import entity.DishIngredient;
import repository.DishRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishRepository dishRepository;

    public DishController(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @GetMapping
    public ResponseEntity<List<Dish>> getAllDishes() {
        List<Dish> dishes = List.of();
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

        try {
            Dish dish = dishRepository.findDishById(id);
            if (dish == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Dish.id=" + id + " is not found");
            }

            dish.setDishIngredients(ingredients);
            Dish updatedDish = dishRepository.save(dish);
            return ResponseEntity.ok(updatedDish);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Dish.id=" + id + " is not found");
        }
    }
}