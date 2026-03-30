package td5springingredientapp.repository;

import jdk.jfr.Registered;
import org.springframework.stereotype.Repository;
import td5springingredientapp.entity.DishOrder;
import td5springingredientapp.entity.Order;
import td5springingredientapp.entity.enums.OrderStatusEnum;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.List;

@Repository
public class OrderRepository {
    private final DataSource dataSource;

    public OrderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Order save(Order order) {
        validateOrder(order);

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            if (order.getId() != null) {
                updateOrder(conn, order);
                conn.commit();
                return order;
            }

            checkStock(conn, order);

            int orderId = insertOrder(conn, order);

            insertDishOrders(conn, orderId, order.getDishOrders());

            processStockOut(conn, order);

            conn.commit();
            return order;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void validateOrder(Order order) {
        if (order.getId() != null && order.getStatus() == OrderStatusEnum.DELIVERED) {
            throw new RuntimeException("A delivered order cannot be modified");
        }
    }

    private void insertDishOrders(Connection conn, int orderId, List<DishOrder> dishOrders) throws SQLException {

        PreparedStatement ps = conn.prepareStatement("""
        INSERT INTO dish_order(id_order, id_dish, quantity)
        VALUES (?, ?, ?)
    """);

        for (DishOrder d : dishOrders) {
            ps.setInt(1, orderId);
            ps.setInt(2, d.getDish().getId());
            ps.setInt(3, d.getQuantity());
            ps.executeUpdate();
        }
    }

    private void updateOrder(Connection conn, Order order) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("""
        UPDATE "order"
        SET type = ?::order_type,
            status = ?::order_status
        WHERE id = ?
    """);

        ps.setString(1, order.getType().name());
        ps.setString(2, order.getStatus().name());
        ps.setInt(3, order.getId());

        ps.executeUpdate();
    }

    private int insertOrder(Connection conn, Order order) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("""
        INSERT INTO "order"(reference, creation_datetime, type, status)
        VALUES (?, ?, ?::order_type, ?::order_status)
        RETURNING id
    """);

        ps.setString(1, order.getReference());
        ps.setTimestamp(2, Timestamp.from(order.getCreationDateTime()));
        ps.setString(3, order.getType().name());
        ps.setString(4, order.getStatus().name());

        ResultSet rs = ps.executeQuery();
        rs.next();

        int id = rs.getInt(1);
        order.setId(id);

        return id;
    }

    private void checkStock(Connection conn, Order order) throws SQLException {
        for (DishOrder dishOrder : order.getDishOrders()) {
            int dishId = dishOrder.getDish().getId();
            int orderedQty = dishOrder.getQuantity();
            List<Integer> ingredientIds = findIngredientIdsByDish(conn, dishId);
            for (Integer ingredientId : ingredientIds) {
                double stockIn = getTotalStock(conn, ingredientId, "IN");
                double stockOut = getTotalStock(conn, ingredientId, "OUT");
                double stock = stockIn - stockOut;
                if (stock < orderedQty) {
                    conn.rollback();
                    throw new RuntimeException("Stock insuffisant pour l'ingrédient " + ingredientId);
                }
            }
        }
    }

    private double getTotalStock(Connection conn, int ingredientId, String type) throws SQLException {

        PreparedStatement ps = conn.prepareStatement("""
                    SELECT SUM(quantity) AS total
                    FROM stock_movement
                    WHERE id_ingredient = ? AND type = ?
                """);

        ps.setInt(1, ingredientId);
        ps.setString(2, type);

        ResultSet rs = ps.executeQuery();

        if (rs.next() && rs.getObject("total") != null) {
            return rs.getDouble("total");
        }

        return 0;
    }

    private List<Integer> findIngredientIdsByDish(Connection conn, int dishId) throws SQLException {
        List<Integer> ids = new java.util.ArrayList<>();

        PreparedStatement ps = conn.prepareStatement("""
                    SELECT id_ingredient
                    FROM dish_ingredient
                    WHERE id_dish = ?
                """);

        ps.setInt(1, dishId);

        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            ids.add(rs.getInt("id_ingredient"));
        }

        return ids;
    }

    private void processStockOut(Connection conn, Order order) throws SQLException {

        PreparedStatement psStockOut = conn.prepareStatement("""
                    INSERT INTO stock_movement(id_ingredient, quantity, unit, type, creation_datetime)
                    VALUES (?, ?, ?::unit_type, 'OUT', ?)
                """);

        for (DishOrder d : order.getDishOrders()) {

            PreparedStatement psIng = conn.prepareStatement("""
                        SELECT di.id_ingredient, di.required_quantity, i.unit
                        FROM dish_ingredient di
                        JOIN ingredient i ON i.id = di.id_ingredient
                        WHERE di.id_dish = ?
                    """);

            psIng.setInt(1, d.getDish().getId());
            ResultSet rs = psIng.executeQuery();

            while (rs.next()) {
                psStockOut.setInt(1, rs.getInt("id_ingredient"));
                psStockOut.setDouble(2, d.getQuantity());
                psStockOut.setString(3, rs.getString("unit"));
                psStockOut.setTimestamp(4, Timestamp.from(Instant.now()));
                psStockOut.executeUpdate();
            }
        }
    }
}
