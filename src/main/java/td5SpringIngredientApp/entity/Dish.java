package td5springingredientapp.entity;

import td5springingredientapp.entity.enums.DishTypeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Dish {
    private Integer id;
    private Double price;
    private String name;
    private DishTypeEnum dishType;
    private List<DishIngredient> dishIngredients = new ArrayList<>();

    public Dish() {
    }

    public Dish(Integer id, Double price, String name, DishTypeEnum dishType, List<DishIngredient> dishIngredients) {
        this.id = id;
        this.price = price;
        this.name = name;
        this.dishType = dishType;
        this.dishIngredients = dishIngredients != null ? dishIngredients : new ArrayList<>();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public DishTypeEnum getDishType() { return dishType; }
    public void setDishType(DishTypeEnum dishType) { this.dishType = dishType; }
    public List<DishIngredient> getDishIngredients() { return dishIngredients; }
    public void setDishIngredients(List<DishIngredient> dishIngredients) {
        this.dishIngredients = dishIngredients != null ? dishIngredients : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Dish dish = (Dish) o;
        return Objects.equals(id, dish.id) &&
                Objects.equals(price, dish.price) &&
                Objects.equals(name, dish.name) &&
                dishType == dish.dishType &&
                Objects.equals(dishIngredients, dish.dishIngredients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, price, name, dishType, dishIngredients);
    }

    @Override
    public String toString() {
        return "Dish{" +
                "id=" + id +
                ", price=" + price +
                ", name='" + name + '\'' +
                ", dishType=" + dishType +
                ", dishIngredients=" + dishIngredients +
                '}';
    }

    public Double getDishCost() {
        if (dishIngredients == null || dishIngredients.isEmpty()) return 0.0;

        double totalPrice = 0;
        for (DishIngredient di : dishIngredients) {
            Double quantity = di.getQuantity();
            if (quantity == null) continue; // on ignore les ingrédients sans quantité
            Double ingredientPrice = di.getIngredient() != null ? di.getIngredient().getPrice() : 0.0;
            totalPrice += ingredientPrice * quantity;
        }
        return totalPrice;
    }
}