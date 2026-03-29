package repository;

import entity.Ingredient;
import entity.StockMovement;
import entity.enums.CategoryEnum;
import entity.enums.MovementTypeEnum;
import entity.enums.UnitEnum;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class IngredientRepository {

    private final DataSource dataSource;

    public IngredientRepository(DataSource dataSource) {
        this.dataSource = dataSource;
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
                if (ingredient.getId() != null) ps.setInt(1, ingredient.getId());
                else ps.setNull(1, Types.INTEGER);

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
                if (sm.getId() != null && sm.getId() > 0) ps.setInt(1, sm.getId());
                else ps.setNull(1, Types.INTEGER);

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
                    sm.setValue(new entity.StockValue(rs.getDouble("quantity"), UnitEnum.valueOf(rs.getString("unit"))));
                    movements.add(sm);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return movements;
    }
}