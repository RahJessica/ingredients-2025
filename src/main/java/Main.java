import entity.*;
import entity.enums.CategoryEnum;
import entity.enums.MovementTypeEnum;
import entity.enums.OrderTypeEnum;
import entity.enums.UnitEnum;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws SQLException {
        // Log before changes
        /* entity.DataRetriever dataRetriever = new entity.DataRetriever();
        entity.Dish dish = dataRetriever.findDishById(2);

        entity.Ingredient carottes = new entity.Ingredient(8, "Carotte", entity.enums.CategoryEnum.VEGETABLE, 100.0 );
        entity.Ingredient haricots = new entity.Ingredient(7, "Haricots", entity.enums.CategoryEnum.VEGETABLE, 300.0);
        entity.Ingredient oignons = new entity.Ingredient(6, "Oignons", entity.enums.CategoryEnum.VEGETABLE, 400.0);
        entity.Ingredient poivre = new entity.Ingredient(9, "Poivre", entity.enums.CategoryEnum.VEGETABLE, 500.0);
        entity.Ingredient pommeDeTerre = new entity.Ingredient(10, "PommeDeTerre", entity.enums.CategoryEnum.VEGETABLE, 600.0);
        entity.Ingredient farine = new entity.Ingredient(11, "Farine", entity.enums.CategoryEnum.OTHER, 1000.0);
        entity.Ingredient lait = new entity.Ingredient(12, "Lait", entity.enums.CategoryEnum.OTHER, 1000.0);

        List<entity.StockMovement> listStockMovementList = new ArrayList<>();
        entity.StockValue stockValue1 = new entity.StockValue(1.0, entity.enums.UnitEnum.L);
        entity.StockMovement stockMovement1 = new entity.StockMovement(11, stockValue1, entity.enums.MovementTypeEnum.OUT, Instant.parse("2026-01-26T15:42:18Z"));

        entity.Ingredient sel =  new entity.Ingredient(13, "Sel", entity.enums.CategoryEnum.OTHER, 1000.0, List.of(stockMovement1));

        // System.out.println(dataRetriever.findDishById(2));

        // Log after changes
        entity.Ingredient savedHaricot = dataRetriever.saveIngredient(haricots);
        entity.Ingredient savedOignons = dataRetriever.saveIngredient(oignons);
        entity.Ingredient savedPommeDeTerre = dataRetriever.saveIngredient(pommeDeTerre);
        entity.Ingredient savedFarine = dataRetriever.saveIngredient(farine);
        entity.Ingredient savedLait = dataRetriever.saveIngredient(lait);

        entity.Ingredient savedSalt = dataRetriever.saveIngredient(sel);
        // System.out.println(dataRetriever.saveIngredient(sel));

        dish.setIngredients(List.of(savedOignons, savedPommeDeTerre, savedFarine, savedLait, savedSalt));

      entity.Dish newDish = dataRetriever.saveDish(dish);
      // System.out.println(newDish);
      // System.out.println(dataRetriever.findDishById(1));

       // System.out.println(dataRetriever.saveIngredient(haricots));

        // entity.Ingredient creations
        //List<entity.Ingredient> createdIngredients = dataRetriever.createIngredients(List.of(new entity.Ingredient(null, "Fromage", entity.enums.CategoryEnum.DAIRY, 1200.0)));
        //System.out.println(createdIngredients);

        entity.DataRetriever dr = new entity.DataRetriever();

        // Test : method saveOrder()
        entity.Dish dish2 = dr.findDishById(6);

        entity.DishOrder dishOrder = new entity.DishOrder();
        dishOrder.setDish(dish2);
        dishOrder.setQuantity(1);

        entity.Order order = new entity.Order();
        order.setReference("ORD0002");
        order.setCreationDateTime(Instant.now());
        order.setDishOrders(List.of(dishOrder));
        entity.Order saved = dr.saveOrder(order);
        System.out.println("Commande enregistrée avec ID = " + saved.getId());

*/

        // Test : DELIVERED status cannot be modified
        DataRetriever dataRetriever = new DataRetriever();
        Order order1 = dataRetriever.findOrderByReference("ORD102");
        order1.setType(OrderTypeEnum.TAKE_AWAY);
        // System.out.println(dataRetriever.saveOrder(order1));
        // System.out.println(dataRetriever.saveOrder(order1));

        // Test : method findOrderByReference()
        // System.out.println(dataRetriever.findOrderByReference("ORD0001"));


        DataRetriever dr = new DataRetriever();
        Instant now = Instant.now();
        StockMovement movement1 = new StockMovement(
                1,
                new StockValue(10.0, UnitEnum.KG),
                MovementTypeEnum.IN,
                now.minusSeconds(3600)
        );

        StockMovement movement2 = new StockMovement(
                2,
                new StockValue(5.0, UnitEnum.KG),
                MovementTypeEnum.IN,
                now.minusSeconds(1800)
        );

        Ingredient ingredient = new Ingredient(
                1,
                "Farine",
                CategoryEnum.OTHER,
                2000.0,
                Arrays.asList(movement1, movement2)
        );

        Dish dish = dataRetriever.findDishById(6);
        // System.out.println(dish.getDishCost());

        Ingredient testIngredient = new Ingredient(
                null,
                "TestFarine",
                CategoryEnum.OTHER,
                100.0,
                Arrays.asList(
                        new StockMovement(0, new StockValue(10.0, UnitEnum.KG), MovementTypeEnum.IN, now.minusSeconds(3600)),
                        new StockMovement(0, new StockValue(3.0, UnitEnum.KG), MovementTypeEnum.OUT, now.minusSeconds(1800))
                )
        );

        StockValue javaStock = testIngredient.getStockValueAt(now);

        System.out.println("Stock testIngredient = " + javaStock.getQuantity() + " " + javaStock.getUnit());

        StockValue dbStock = dr.getStockValueAt(now, 2);

        System.out.println("Stock calculé côté DB = " + dbStock.getQuantity() + " " + dbStock.getUnit());

    }
}
