import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws SQLException {
        // Log before changes
        DataRetriever dataRetriever = new DataRetriever();
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


        // System.out.println(dataRetriever.findOrderByReference("ORD0001"));
        
        DataRetriever dr = new DataRetriever();

        // Créer un plat existant (déjà en base)
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
    }
}
