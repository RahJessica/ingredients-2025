import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws SQLException {
        // Log before changes
        /* DataRetriever dataRetriever = new DataRetriever();
        Dish dish = dataRetriever.findDishById(2);

        Ingredient carottes = new Ingredient(8, "Carotte", CategoryEnum.VEGETABLE, 100.0 );
        Ingredient haricots = new Ingredient(7, "Haricots", CategoryEnum.VEGETABLE, 300.0);
        Ingredient oignons = new Ingredient(6, "Oignons", CategoryEnum.VEGETABLE, 400.0);
        Ingredient poivre = new Ingredient(9, "Poivre", CategoryEnum.VEGETABLE, 500.0);
        Ingredient pommeDeTerre = new Ingredient(10, "PommeDeTerre", CategoryEnum.VEGETABLE, 600.0);
        Ingredient farine = new Ingredient(11, "Farine", CategoryEnum.OTHER, 1000.0);
        Ingredient lait = new Ingredient(12, "Lait", CategoryEnum.OTHER, 1000.0);

        List<StockMovement> listStockMovementList = new ArrayList<>();
        StockValue stockValue1 = new StockValue(1.0, UnitEnum.L);
        StockMovement stockMovement1 = new StockMovement(11, stockValue1, MovementTypeEnum.OUT, Instant.parse("2026-01-26T15:42:18Z"));

        Ingredient sel =  new Ingredient(13, "Sel", CategoryEnum.OTHER, 1000.0, List.of(stockMovement1));

        // System.out.println(dataRetriever.findDishById(2));

        // Log after changes
        Ingredient savedHaricot = dataRetriever.saveIngredient(haricots);
        Ingredient savedOignons = dataRetriever.saveIngredient(oignons);
        Ingredient savedPommeDeTerre = dataRetriever.saveIngredient(pommeDeTerre);
        Ingredient savedFarine = dataRetriever.saveIngredient(farine);
        Ingredient savedLait = dataRetriever.saveIngredient(lait);

        Ingredient savedSalt = dataRetriever.saveIngredient(sel);
        // System.out.println(dataRetriever.saveIngredient(sel));

        dish.setIngredients(List.of(savedOignons, savedPommeDeTerre, savedFarine, savedLait, savedSalt));

      Dish newDish = dataRetriever.saveDish(dish);
      // System.out.println(newDish);
      // System.out.println(dataRetriever.findDishById(1));

       // System.out.println(dataRetriever.saveIngredient(haricots));

        // Ingredient creations
        //List<Ingredient> createdIngredients = dataRetriever.createIngredients(List.of(new Ingredient(null, "Fromage", CategoryEnum.DAIRY, 1200.0)));
        //System.out.println(createdIngredients);

        DataRetriever dr = new DataRetriever();

        // Test : method saveOrder()
        Dish dish2 = dr.findDishById(6);

        DishOrder dishOrder = new DishOrder();
        dishOrder.setDish(dish2);
        dishOrder.setQuantity(1);

        Order order = new Order();
        order.setReference("ORD0002");
        order.setCreationDateTime(Instant.now());
        order.setDishOrders(List.of(dishOrder));
        Order saved = dr.saveOrder(order);
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
