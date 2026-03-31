package td5springingredientapp.repository;

import org.springframework.stereotype.Repository;
import td5springingredientapp.entity.Dish;
import td5springingredientapp.entity.DishIngredient;
import td5springingredientapp.entity.enums.DishTypeEnum;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class DishRepository {

    private final DataSource dataSource;

    public DishRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Dish> findAllDishes() {
        List<Dish> dishes = new java.util.ArrayList<>();

        String sql = """
        SELECT id, name, dish_type, price
        FROM dish
    """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Dish dish = new Dish();
                dish.setId(rs.getInt("id"));
                dish.setName(rs.getString("name"));
                dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type").toUpperCase()));
                dish.setPrice(rs.getObject("price") == null ? null : rs.getDouble("price"));

                dishes.add(dish);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return dishes;
    }

    public Dish findDishById(Integer id) {
        String sql = """
                select dish.id, dish.name, dish.dish_type, dish.price
                from dish
                where dish.id = ?;
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Dish dish = new Dish();
                    dish.setId(rs.getInt("id"));
                    dish.setName(rs.getString("name"));
                    dish.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                    dish.setPrice(rs.getObject("price") == null ? null : rs.getDouble("price"));
                    return dish;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        throw new RuntimeException("Dish not found with id " + id);
    }

    public Dish save(Dish dish) {
        String upsertSql = """
                INSERT INTO dish (id, name, dish_type, price)
                VALUES (?, ?, ?::dish_type, ?)
                ON CONFLICT (id) DO UPDATE
                SET name = EXCLUDED.name,
                    dish_type = EXCLUDED.dish_type,
                    price = EXCLUDED.price
                RETURNING id
                """;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            Integer dishId;

            try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
                if (dish.getId() != null) {
                    ps.setInt(1, dish.getId());
                } else {
                    ps.setNull(1, Types.INTEGER);
                }
                ps.setString(2, dish.getName());
                ps.setString(3, dish.getDishType().name());
                if (dish.getPrice() != null) {
                    ps.setDouble(4, dish.getPrice());
                }
                else {
                    ps.setNull(4, Types.DOUBLE);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    dishId = rs.getInt(1);
                }
            }

            detachIngredients(conn, dish.getDishIngredients());
            attachIngredients(conn, dish.getDishIngredients());

            conn.commit();
            return findDishById(dishId);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Dish> createDishes(List<Dish> dishes) {
        String checkNameSql = "SELECT COUNT(dish.id) FROM dish WHERE name = ?";
        String insertSql = """
            INSERT INTO dish (id, name, dish_type, price)
            VALUES (?, ?, ?::dish_type, ?)
            RETURNING id
            """;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            List<Dish> createdDishes = new java.util.ArrayList<>();

            for (Dish dish : dishes) {
                try (PreparedStatement checkPs = conn.prepareStatement(checkNameSql)) {
                    checkPs.setString(1, dish.getName());
                    try (ResultSet rs = checkPs.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) > 0) {
                            throw new RuntimeException("Dish.name=" + dish.getName() + " already exists");
                        }
                    }
                }

                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setInt(1, getNextSerialValue(conn, "dish", "id"));
                    insertPs.setString(2, dish.getName());
                    insertPs.setString(3, dish.getDishType().name());
                    if (dish.getPrice() != null) {
                        insertPs.setDouble(4, dish.getPrice());
                    } else {
                        insertPs.setNull(4, Types.DOUBLE);
                    }

                    try (ResultSet rs = insertPs.executeQuery()) {
                        rs.next();
                        int generatedId = rs.getInt(1);
                        dish.setId(generatedId);
                        createdDishes.add(dish);
                    }
                }
            }

            conn.commit();
            return createdDishes;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void attachIngredients(Connection conn, List<DishIngredient> ingredients) throws SQLException {
        if (ingredients == null || ingredients.isEmpty()) return;

        String attachSql = """
                INSERT INTO dish_ingredient (id, id_ingredient, id_dish, quantity_required, unit)
                VALUES (?, ?, ?, ?, ?::unit_type)
                """;

        try (PreparedStatement ps = conn.prepareStatement(attachSql)) {
            for (DishIngredient di : ingredients) {
                ps.setInt(1, getNextSerialValue(conn, "dish_ingredient", "id"));
                ps.setInt(2, di.getIngredient().getId());
                ps.setInt(3, di.getDish().getId());
                ps.setDouble(4, di.getQuantity());
                ps.setString(5, di.getUnit().name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void detachIngredients(Connection conn, List<DishIngredient> ingredients) throws SQLException {
        if (ingredients == null || ingredients.isEmpty()) return;

        Map<Integer, List<DishIngredient>> grouped = ingredients.stream()
                .collect(Collectors.groupingBy(di -> di.getDish().getId()));
        for (Integer dishId : grouped.keySet()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM dish_ingredient WHERE id_dish = ?")) {
                ps.setInt(1, dishId);
                ps.executeUpdate();
            }
        }
    }

    private int getNextSerialValue(Connection conn, String table, String column) throws SQLException {
        String seqSql = "SELECT pg_get_serial_sequence(?, ?)";
        String sequenceName;
        try (PreparedStatement ps = conn.prepareStatement(seqSql)) {
            ps.setString(1, table);
            ps.setString(2, column);
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
}