package td5springingredientapp.entity;

import td5springingredientapp.entity.enums.UnitEnum;

public class DishIngredient {
    private td5springingredientapp.entity.Dish dish;
    private td5springingredientapp.entity.Ingredient ingredient;
    private Double quantity;
    private UnitEnum unit;

    public td5springingredientapp.entity.Ingredient getIngredient() {
        return ingredient;
    }

    public void setIngredient(td5springingredientapp.entity.Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public UnitEnum getUnit() {
        return unit;
    }

    public void setUnit(UnitEnum unit) {
        this.unit = unit;
    }

    public td5springingredientapp.entity.Dish getDish() {
        return dish;
    }

    public void setDish(Dish dish) {
        this.dish = dish;
    }

    @Override
    public String toString() {
        return "td5SpringIngredientApp.repository.entity.DishIngredient{" +
                "ingredient=" + ingredient +
                ", quantity=" + quantity +
                ", unit=" + unit +
                '}';
    }
}