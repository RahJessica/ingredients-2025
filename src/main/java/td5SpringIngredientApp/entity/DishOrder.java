package td5springingredientapp.entity;

import java.util.Objects;

public class DishOrder {
    private Integer id;
    private td5springingredientapp.entity.Dish dish;
    private Integer quantity;

    public DishOrder(Integer id, td5springingredientapp.entity.Dish dish, Integer quantity) {
        this.id = id;
        this.dish = dish;
        this.quantity = quantity;
    }

    public DishOrder() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public td5springingredientapp.entity.Dish getDish() {
        return dish;
    }

    public void setDish(td5springingredientapp.entity.Dish dish) {
        this.dish = dish;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DishOrder dishOrder = (DishOrder) o;
        return Objects.equals(id, dishOrder.id) && Objects.equals(dish, dishOrder.dish) && Objects.equals(quantity, dishOrder.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dish, quantity);
    }

    @Override
    public String toString() {
        return "td5SpringIngredientApp.repository.entity.DishOrder{" +
                "id=" + id +
                ", dish=" + dish +
                ", quantity=" + quantity +
                '}';
    }
}
