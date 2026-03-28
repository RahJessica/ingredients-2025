package repository;

import entity.Dish;
import entity.DishIngredient;
import entity.enums.DishTypeEnum;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DishRepository {

    private final DataSource dataSource;

    public DishRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Dish findDishById(Integer id) {
        String sql = """
                select dish.id as dish_id, dish.name as dish_name, dish_type, dish.price as dish_price
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
                    dish.setPrice(rs.getObject("selling_price") == null ? null : rs.getDouble("selling_price"));
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
                INSERT INTO dish (id, selling_price, name, dish_type)
                VALUES (?, ?, ?, ?::dish_type)
                ON CONFLICT (id) DO UPDATE
                SET name = EXCLUDED.name,
                    dish_type = EXCLUDED.dish_type,
                    selling_price = EXCLUDED.selling_price
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
                if (dish.getPrice() != null) {
                    ps.setDouble(2, dish.getPrice());
                } else {
                    ps.setNull(2, Types.DOUBLE);
                }
                ps.setString(3, dish.getName());
                ps.setString(4, dish.getDishType().name());

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

    private void attachIngredients(Connection conn, List<DishIngredient> ingredients) throws SQLException {
        if (ingredients == null || ingredients.isEmpty()) return;

        String attachSql = """
                INSERT INTO dish_ingredient (id, id_ingredient, id_dish, required_quantity, unit)
                VALUES (?, ?, ?, ?, ?::unit)
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