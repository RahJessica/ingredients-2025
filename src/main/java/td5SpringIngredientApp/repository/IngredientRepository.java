package td5springingredientapp.repository;

import org.springframework.stereotype.Repository;
import td5springingredientapp.entity.DishIngredient;
import td5springingredientapp.entity.Ingredient;
import td5springingredientapp.entity.StockMovement;
import td5springingredientapp.entity.StockValue;
import td5springingredientapp.entity.enums.CategoryEnum;
import td5springingredientapp.entity.enums.MovementTypeEnum;
import td5springingredientapp.entity.enums.UnitEnum;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class IngredientRepository {

    private final DataSource dataSource;

    public IngredientRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Ingredient> findAllIngredients() {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT id, name, price, category FROM ingredient";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Ingredient ingredient = new Ingredient();
                ingredient.setId(rs.getInt("id"));
                ingredient.setName(rs.getString("name"));
                ingredient.setPrice(rs.getDouble("price"));
                String cat = rs.getString("category");
                ingredient.setCategory(cat != null ? CategoryEnum.valueOf(cat) : null);

                List<StockMovement> movements = findStockMovementsByIngredientId(ingredient.getId());
                ingredient.setStockMovementList(movements);

                ingredients.add(ingredient);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return ingredients;
    }

    public Ingredient findById(Integer id) {
        String sql = "SELECT id, name, price, category FROM ingredient WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Ingredient ingredient = new Ingredient();
                    ingredient.setId(rs.getInt("id"));
                    ingredient.setName(rs.getString("name"));
                    ingredient.setPrice(rs.getDouble("price"));
                    String cat = rs.getString("category");
                    ingredient.setCategory(cat != null ? CategoryEnum.valueOf(cat) : null);

                    List<StockMovement> movements = findStockMovementsByIngredientId(ingredient.getId());
                    ingredient.setStockMovementList(movements);

                    return ingredient;
                } else {
                    return null;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) {
            return List.of();
        }

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            List<Ingredient> savedIngredients = insertIngredients(conn, newIngredients);

            conn.commit();
            return savedIngredients;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Ingredient> insertIngredients(Connection conn, List<Ingredient> ingredients) throws SQLException {

        List<Ingredient> savedIngredients = new ArrayList<>();

        String insertSql = """
            INSERT INTO ingredient (id, name, category, price, required_quantity)
            VALUES (?, ?, ?::ingredient_category, ?, ?)
            RETURNING id
            """;

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {

            for (Ingredient ingredient : ingredients) {

                prepareIngredientStatement(conn, ps, ingredient);

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    int generatedId = rs.getInt(1);
                    ingredient.setId(generatedId);
                    savedIngredients.add(ingredient);
                }
            }

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }

        return savedIngredients;
    }

    private void prepareIngredientStatement(Connection conn, PreparedStatement ps, Ingredient ingredient) throws SQLException {

        if (ingredient.getId() != null) {
            ps.setInt(1, ingredient.getId());
        } else {
            ps.setInt(1, getNextSerialValue(conn, "ingredient", "id"));
        }

        ps.setString(2, ingredient.getName());
        ps.setString(3, ingredient.getCategory().name());
        ps.setDouble(4, ingredient.getPrice());
        ps.setObject(5, null);
    }

    private int getNextSerialValue(Connection conn, String tableName, String columnName) throws SQLException {
        String seqSql = "SELECT pg_get_serial_sequence(?, ?)";
        String sequenceName;

        try (PreparedStatement ps = conn.prepareStatement(seqSql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                sequenceName = rs.getString(1);
            }
        }

        String nextValSql = "SELECT nextval(?)";
        try (PreparedStatement ps = conn.prepareStatement(nextValSql)) {
            ps.setString(1, sequenceName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public Ingredient save(Ingredient ingredient) {
        String upsertSql = """
            INSERT INTO ingredient (id, name, price, category)
            VALUES (?, ?, ?, ?::ingredient_category)
            ON CONFLICT (id) DO UPDATE
            SET name = EXCLUDED.name,
                price = EXCLUDED.price,
                category = EXCLUDED.category
            RETURNING id
            """;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
                if (ingredient.getId() != null) {
                    ps.setInt(1, ingredient.getId());
                } else {
                    ps.setNull(1, Types.INTEGER);
                }

                ps.setString(2, ingredient.getName());
                ps.setDouble(3, ingredient.getPrice());
                ps.setString(4, ingredient.getCategory().name());

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    ingredient.setId(rs.getInt(1));
                }
            }

            insertStockMovements(conn, ingredient.getStockMovementList(), ingredient.getId());

            conn.commit();
            return ingredient;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertStockMovements(Connection conn, List<StockMovement> stockMovements, int ingredientId) throws SQLException {
        if (stockMovements == null || stockMovements.isEmpty()) return;

        String sql = """
            INSERT INTO stock_movement (id, id_ingredient, quantity, type, unit, creation_datetime)
            VALUES (?, ?, ?, ?::movement_type, ?::unit, ?)
            ON CONFLICT (id) DO NOTHING
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (StockMovement sm : stockMovements) {

                if (sm.getId() != null && sm.getId() > 0) {
                    ps.setInt(1, sm.getId());
                } else {
                    ps.setNull(1, Types.INTEGER);
                }

                ps.setInt(2, ingredientId);
                ps.setDouble(3, sm.getValue().getQuantity());
                ps.setString(4, sm.getType().name());
                ps.setString(5, sm.getValue().getUnit().name());
                ps.setTimestamp(6, Timestamp.from(sm.getCreationDatetime()));

                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<StockMovement> findStockMovementsByIngredientId(int ingredientId) {
        List<StockMovement> movements = new ArrayList<>();

        String sql = "SELECT id, quantity, unit, type, creation_datetime FROM stock_movement WHERE id_ingredient = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ingredientId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockMovement sm = new StockMovement();
                    sm.setId(rs.getInt("id"));
                    sm.setType(MovementTypeEnum.valueOf(rs.getString("type")));
                    sm.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());

                    StockValue value = new StockValue(
                            rs.getDouble("quantity"),
                            UnitEnum.valueOf(rs.getString("unit"))
                    );

                    sm.setValue(value);

                    movements.add(sm);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return movements;
    }

    private void attachIngredients(Connection conn, List<DishIngredient> ingredients)
            throws SQLException {

        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }

        String attachSql = """
            insert into dish_ingredient (id, id_ingredient, id_dish, required_quantity, unit)
            values (?, ?, ?, ?, ?::unit)
            """;

        try (PreparedStatement ps = conn.prepareStatement(attachSql)) {

            for (DishIngredient dishIngredient : ingredients) {
                prepareDishIngredientStatement(conn, ps, dishIngredient);
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    private void prepareDishIngredientStatement(Connection conn, PreparedStatement ps, DishIngredient dishIngredient)
            throws SQLException {

        ps.setInt(1, getNextSerialValue(conn, "dish_ingredient", "id"));
        ps.setInt(2, dishIngredient.getIngredient().getId());
        ps.setInt(3, dishIngredient.getDish().getId());
        ps.setDouble(4, dishIngredient.getQuantity());
        ps.setObject(5, dishIngredient.getUnit());
    }

    public List<Ingredient> findIngredientByDishId(Integer idDish) {

        List<Ingredient> ingredients = new ArrayList<>();

        String sql = """
            select ingredient.id, ingredient.name, ingredient.price, ingredient.category, ingredient.required_quantity
            from ingredient 
            JOIN dish_ingredient di
            ON di.id_ingredient = ingredient.id
            WHERE di.id_dish = ?;
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idDish);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ingredients.add(mapIngredient(rs));
                }
            }

            return ingredients;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Ingredient mapIngredient(ResultSet rs) throws SQLException {

        Ingredient ingredient = new Ingredient();

        ingredient.setId(rs.getInt("id"));
        ingredient.setName(rs.getString("name"));
        ingredient.setPrice(rs.getDouble("price"));
        ingredient.setCategory(CategoryEnum.valueOf(rs.getString("category")));
        Object requiredQuantity = rs.getObject("required_quantity");

        return ingredient;
    }

    public List<StockMovement> findStockMovementsByIngredientId(Integer ingredientId) {
        List<StockMovement> movements = new ArrayList<>();
        String sql = "SELECT id, type, quantity, unit, creation_datetime " +
                "FROM stock_movement WHERE id_ingredient = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockMovement sm = new StockMovement();
                    sm.setId(rs.getInt("id"));
                    sm.setType(MovementTypeEnum.valueOf(rs.getString("type")));
                    StockValue value = new StockValue();
                    value.setQuantity(rs.getDouble("quantity"));
                    value.setUnit(UnitEnum.valueOf(rs.getString("unit")));
                    sm.setValue(value);
                    sm.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());
                    movements.add(sm);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return movements;
    }
}