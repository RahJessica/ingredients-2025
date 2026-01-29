    import java.sql.*;
    import java.time.Instant;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.stream.Collectors;

    public class DataRetriever {
        Dish findDishById(Integer id) {
            DBConnection dbConnection = new DBConnection();
            Connection connection = dbConnection.getConnection();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        """
                                select dish.id as dish_id, dish.name as dish_name, dish_type, dish.price as dish_price
                                from dish
                                where dish.id = ?;
                                """);
                preparedStatement.setInt(1, id);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    Dish dish = new Dish();
                    dish.setId(resultSet.getInt("dish_id"));
                    dish.setName(resultSet.getString("dish_name"));
                    dish.setDishType(DishTypeEnum.valueOf(resultSet.getString("dish_type")));
                    dish.setPrice(resultSet.getObject("dish_price") == null
                            ? null : resultSet.getDouble("dish_price"));
                    return dish;
                }
                dbConnection.closeConnection(connection);
                throw new RuntimeException("Dish not found " + id);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        Dish saveDish(Dish toSave) {
            String upsertDishSql = """
                        INSERT INTO dish (id, price, name, dish_type)
                        VALUES (?, ?, ?, ?::dish_type)
                        ON CONFLICT (id) DO UPDATE
                        SET name = EXCLUDED.name,
                        dish_type = EXCLUDED.dish_type
                        RETURNING id  -- retourne l'id de la ligne insérée ou updatée
                    """;

            try (Connection conn = new DBConnection().getConnection()) {
                conn.setAutoCommit(false);
                Integer dishId;
                try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                    if (toSave.getId() != null) {
                        ps.setInt(1, toSave.getId());
                    } else {
                        ps.setInt(1, getNextSerialValue(conn, "dish", "id"));
                    }
                    if (toSave.getPrice() != null) {
                        ps.setDouble(2, toSave.getPrice());
                    } else {
                        ps.setNull(2, Types.DOUBLE);
                    }
                    ps.setString(3, toSave.getName());
                    ps.setString(4, toSave.getDishType().name());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        dishId = rs.getInt(1);
                    }
                }

                List<Ingredient> newIngredients = toSave.getIngredients();
                detachIngredients(conn, dishId, newIngredients);
                attachIngredients(conn, dishId, newIngredients);

                conn.commit();
                return findDishById(dishId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
            if (newIngredients == null || newIngredients.isEmpty()) {
                return List.of();
            }
            List<Ingredient> savedIngredients = new ArrayList<>();
            DBConnection dbConnection = new DBConnection();
            Connection conn = dbConnection.getConnection();
            try {
                conn.setAutoCommit(false);
                String insertSql = """
                            INSERT INTO ingredient (id, name, category, price, required_quantity)
                            VALUES (?, ?, ?::ingredient_category, ?, ?)
                            RETURNING id
                        """;
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    for (Ingredient ingredient : newIngredients) {
                        if (ingredient.getId() != null) {
                            ps.setInt(1, ingredient.getId());
                        } else {
                            ps.setInt(1, getNextSerialValue(conn, "ingredient", "id"));
                        }
                        ps.setString(2, ingredient.getName());
                        ps.setString(3, ingredient.getCategory().name());
                        ps.setDouble(4, ingredient.getPrice());

                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            int generatedId = rs.getInt(1);
                            ingredient.setId(generatedId);
                            savedIngredients.add(ingredient);
                        }
                    }
                    conn.commit();
                    return savedIngredients;
                } catch (SQLException e) {
                    conn.rollback();
                    throw new RuntimeException(e);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                dbConnection.closeConnection(conn);
            }
        }


        private void detachIngredients(Connection conn, Integer dishId, List<Ingredient> ingredients)
                throws SQLException {
            if (ingredients == null || ingredients.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM dish_ingredient WHERE id_dish = ?")) {
                    ps.setInt(1, dishId);
                    ps.executeUpdate();
                }
                return;
            }

            String baseSql = """
                        DELETE FROM dish_ingredient
                        WHERE id_dish = ? AND id_ingredient NOT IN (%s)
                    """;

            String inClause = ingredients.stream()
                    .map(i -> "?")
                    .collect(Collectors.joining(","));

            String sql = String.format(baseSql, inClause);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, dishId);
                int index = 2;
                for (Ingredient ingredient : ingredients) {
                    ps.setInt(index++, ingredient.getId());
                }
                ps.executeUpdate();
            }
        }

        private void attachIngredients(Connection conn, Integer dishId, List<Ingredient> ingredients)
                throws SQLException {

            if (ingredients == null || ingredients.isEmpty()) {
                return;
            }

            String attachSql = """
                        INSERT INTO dish_ingredient (id_dish, id_ingredient)
                        VALUES (?, ?)
                        ON CONFLICT (id_dish, id_ingredient) DO NOTHING
                    """;

            try (PreparedStatement ps = conn.prepareStatement(attachSql)) {
                for (Ingredient ingredient : ingredients) {
                    ps.setInt(1, dishId);
                    ps.setInt(2, ingredient.getId());

                    ps.addBatch(); // Can be substitute ps.executeUpdate() but bad performance
                }
                ps.executeBatch();
            }
        }

        private List<Ingredient> findIngredientByDishId(Integer idDish) {
            DBConnection dbConnection = new DBConnection();
            Connection connection = dbConnection.getConnection();
            List<Ingredient> ingredients = new ArrayList<>();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        """
                                select ingredient.id, ingredient.name, ingredient.price, ingredient.category, ingredient.required_quantity
                                from ingredient 
                                JOIN dish_ingredient di
                                ON di.id_ingredient = ingredient.id
                                WHERE di.id_dish = ?;
                                """);
                preparedStatement.setInt(1, idDish);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    Ingredient ingredient = new Ingredient();
                    ingredient.setId(resultSet.getInt("id"));
                    ingredient.setName(resultSet.getString("name"));
                    ingredient.setPrice(resultSet.getDouble("price"));
                    ingredient.setCategory(CategoryEnum.valueOf(resultSet.getString("category")));
                    Object requiredQuantity = resultSet.getObject("required_quantity");
                    ingredients.add(ingredient);
                }
                dbConnection.closeConnection(connection);
                return ingredients;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public Ingredient saveIngredient(Ingredient ingredient) {

            String upsertIngredientSql = """
            INSERT INTO ingredient (id, name, price, category)
            VALUES (?, ?, ?, ?::ingredient_category)
            ON CONFLICT (id) DO UPDATE
            SET name = EXCLUDED.name,
                price = EXCLUDED.price,
                category = EXCLUDED.category
            RETURNING id
        """;

            String insertStockMovementSql = """
            INSERT INTO stock_movement (id, id_ingredient, quantity, unit, type, creation_datetime)
            VALUES (?, ?, ?, ?::unit_type, ?::movement_type, ?)
            ON CONFLICT (id) DO NOTHING
        """;

            try (Connection conn = new DBConnection().getConnection()) {
                conn.setAutoCommit(false);

                try (PreparedStatement ps = conn.prepareStatement(upsertIngredientSql)) {

                    if (ingredient.getId() != null)
                        ps.setInt(1, ingredient.getId());
                    else
                        ps.setNull(1, Types.INTEGER);

                    ps.setString(2, ingredient.getName());
                    ps.setDouble(3, ingredient.getPrice());
                    ps.setString(4, ingredient.getCategory().name());

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        ingredient.setId(rs.getInt(1));
                    }
                }

                if (ingredient.getStockMovementList() != null && !ingredient.getStockMovementList().isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(insertStockMovementSql)) {
                        for (StockMovement sm : ingredient.getStockMovementList()) {

                            if (sm.getId() > 0)
                                ps.setInt(1, sm.getId());
                            else
                                ps.setNull(1, Types.INTEGER);

                            ps.setInt(2, ingredient.getId());

                            ps.setDouble(3, sm.getValue().getQuantity());

                            ps.setString(4, sm.getValue().getUnit().name());

                            ps.setString(5, sm.getType().name());

                            ps.setTimestamp(6, Timestamp.from(sm.getCreationDatetime()));

                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                conn.commit();
                return ingredient;

            } catch (SQLException e) {
                throw new RuntimeException("Error saving ingredient", e);
            }
        }

        public StockValue getStockValueAt(Instant t) {

            throw new RuntimeException("Not implemented");
        }

        Order findOrderByReference(String reference) {
            DBConnection dbConnection = new DBConnection();
            try (Connection connection = dbConnection.getConnection()) {
                PreparedStatement preparedStatement = connection.prepareStatement("""
                        select id, reference, creation_datetime, type, status from "order" where reference like ?""");
                preparedStatement.setString(1, reference);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    Order order = new Order();
                    Integer idOrder = resultSet.getInt("id");
                    order.setId(idOrder);
                    order.setReference(resultSet.getString("reference"));
                    order.setCreationDateTime(resultSet.getTimestamp("creation_datetime").toInstant());
                    order.setDishOrders(findDishOrderByIdOrder(idOrder));
                    order.setType(OrderTypeEnum.valueOf(resultSet.getString("type")));
                    order.setStatus(OrderStatusEnum.valueOf(resultSet.getString("status")));
                    return order;
                }
                throw new RuntimeException("Order not found with reference " + reference);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        List<DishOrder> findDishOrderByIdOrder(Integer idOrder) {
            DBConnection dbConnection = new DBConnection();
            Connection connection = dbConnection.getConnection();
            List<DishOrder> dishOrders = new ArrayList<>();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        """
                                select id, id_dish, quantity from dish_order where dish_order.id_order = ?
                                """);
                preparedStatement.setInt(1, idOrder);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    Dish dish = findDishById(resultSet.getInt("id_dish"));
                    DishOrder dishOrder = new DishOrder(    );
                    dishOrder.setId(resultSet.getInt("id"));
                    dishOrder.setQuantity(resultSet.getInt("quantity"));
                    dishOrder.setDish(dish);
                    dishOrders.add(dishOrder);
                }
                dbConnection.closeConnection(connection);
                return dishOrders;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public Order saveOrder(Order orderToSave) {

            if (orderToSave.getStatus() == OrderStatusEnum.DELIVERED) {
                throw new RuntimeException("A delivered order cannot be modified");
            }

            try (Connection conn = new DBConnection().getConnection()) {
                conn.setAutoCommit(false);

                for (DishOrder dishOrder : orderToSave.getDishOrders()) {
                    int dishId = dishOrder.getDish().getId();
                    int orderedQty = dishOrder.getQuantity();

                    // on cible l'ingrédient du order
                    PreparedStatement ps = conn.prepareStatement("""
                SELECT id_ingredient
                FROM dish_ingredient
                WHERE id_dish = ?
            """);
                    ps.setInt(1, dishId);
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        int ingredientId = rs.getInt("id_ingredient");

                        PreparedStatement psIn = conn.prepareStatement("""
                    SELECT SUM(quantity) AS total
                    FROM stock_movement
                    WHERE id_ingredient = ? AND type = 'IN'
                """);
                        psIn.setInt(1, ingredientId);
                        ResultSet rsIn = psIn.executeQuery();

                        double stockIn = 0;
                        if (rsIn.next() && rsIn.getObject("total") != null) {
                            stockIn = rsIn.getDouble("total");
                        }

                        PreparedStatement psOut = conn.prepareStatement("""
                    SELECT SUM(quantity) AS total
                    FROM stock_movement
                    WHERE id_ingredient = ? AND type = 'OUT'
                """);
                        psOut.setInt(1, ingredientId);
                        ResultSet rsOut = psOut.executeQuery();

                        double stockOut = 0;
                        if (rsOut.next() && rsOut.getObject("total") != null) {
                            stockOut = rsOut.getDouble("total");
                        }

                        double stock = stockIn - stockOut;

                        if (stock < orderedQty) {
                            conn.rollback();
                            throw new RuntimeException("Stock insuffisant pour l'ingrédient " + ingredientId);
                        }
                    }
                }

                PreparedStatement psOrder = conn.prepareStatement("""
            INSERT INTO "order"(reference, creation_datetime, type, status)
            VALUES (?, ?, ?::order_type, ?::order_status)
            RETURNING id
        """);
                psOrder.setString(1, orderToSave.getReference());
                psOrder.setTimestamp(2, Timestamp.from(orderToSave.getCreationDateTime()));
                psOrder.setString(3, orderToSave.getType().name());
                psOrder.setString(4, orderToSave.getStatus().name());

                ResultSet rsOrder = psOrder.executeQuery();
                rsOrder.next();
                int orderId = rsOrder.getInt(1);
                orderToSave.setId(orderId);

                PreparedStatement psDishOrder = conn.prepareStatement("""
            INSERT INTO dish_order(id_order, id_dish, quantity)
            VALUES (?, ?, ?)
        """);

                for (DishOrder d : orderToSave.getDishOrders()) {
                    psDishOrder.setInt(1, orderId);
                    psDishOrder.setInt(2, d.getDish().getId());
                    psDishOrder.setInt(3, d.getQuantity());
                    psDishOrder.executeUpdate();
                }

                // Sortie du stock
                PreparedStatement psStockOut = conn.prepareStatement("""
            INSERT INTO stock_movement(id_ingredient, quantity, unit, type, creation_datetime)
            VALUES (?, ?, ?::unit_type, 'OUT', ?)
        """);

                for (DishOrder d : orderToSave.getDishOrders()) {
                    PreparedStatement psIng2 = conn.prepareStatement("""
                SELECT di.id_ingredient, di.required_quantity, i.unit
                FROM dish_ingredient di
                JOIN ingredient i ON i.id = di.id_ingredient
                WHERE di.id_dish = ?
            """);
                    psIng2.setInt(1, d.getDish().getId());
                    ResultSet rsIng2 = psIng2.executeQuery();

                    while (rsIng2.next()) {
                        int ingId = rsIng2.getInt("id_ingredient");

                        psStockOut.setInt(1, ingId);
                        psStockOut.setDouble(2, d.getQuantity());
                        psStockOut.setString(3, rsIng2.getString("unit"));
                        psStockOut.setTimestamp(4, Timestamp.from(Instant.now()));
                        psStockOut.executeUpdate();
                    }
                }

                conn.commit();
                return orderToSave;

            } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erreur technique lors du saveOrder", e);
        }
        }

        private void insertIngredientStockMovements(Connection conn, Ingredient ingredient) {
            List<StockMovement> stockMovementList = ingredient.getStockMovementList();
            String sql = """
                insert into stock_movement(id, id_ingredient, quantity, type, unit, creation_datetime)
                values (?, ?, ?, ?::movement_type, ?::unit, ?)
                on conflict (id) do nothing
                """;
            try {
                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                for (StockMovement stockMovement : stockMovementList) {
                    if (ingredient.getId() != null) {
                        preparedStatement.setInt(1, ingredient.getId());
                    } else {
                        preparedStatement.setInt(1, getNextSerialValue(conn, "stock_movement", "id"));
                    }
                    preparedStatement.setInt(2, ingredient.getId());
                    preparedStatement.setDouble(3, stockMovement.getValue().getQuantity());
                    preparedStatement.setObject(4, stockMovement.getType());
                    preparedStatement.setObject(5, stockMovement.getValue().getUnit());
                    preparedStatement.setTimestamp(6, Timestamp.from(stockMovement.getCreationDatetime()));
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        List<StockMovement> findStockMovementsByIngredientId(Integer id) {

            DBConnection dbConnection = new DBConnection();
            Connection connection = dbConnection.getConnection();
            List<StockMovement> stockMovementList = new ArrayList<>();
            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        """
                                select id, quantity, unit, type, creation_datetime
                                from stock_movement
                                where stock_movement.id_ingredient = ?;
                                """);
                preparedStatement.setInt(1, id);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    StockMovement stockMovement = new StockMovement();
                    stockMovement.setId(resultSet.getInt("id"));
                    stockMovement.setType(MovementTypeEnum.valueOf(resultSet.getString("type")));
                    stockMovement.setCreationDatetime(resultSet.getTimestamp("creation_datetime").toInstant());

                    StockValue stockValue = new StockValue();
                    stockValue.setQuantity(resultSet.getDouble("quantity"));
                    stockValue.setUnit(UnitEnum.valueOf(resultSet.getString("unit")));
                    stockMovement.setValue(stockValue);

                    stockMovementList.add(stockMovement);
                }
                dbConnection.closeConnection(connection);
                return stockMovementList;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        private String getSerialSequenceName(Connection conn, String tableName, String columnName)
                throws SQLException {

            String sql = "SELECT pg_get_serial_sequence(?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, tableName);
                ps.setString(2, columnName);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                }
            }
            return null;
        }

        private int getNextSerialValue(Connection conn, String tableName, String columnName)
                throws SQLException {

            String sequenceName = getSerialSequenceName(conn, tableName, columnName);
            if (sequenceName == null) {
                throw new IllegalArgumentException(
                        "Any sequence found for " + tableName + "." + columnName
                );
            }
            updateSequenceNextValue(conn, tableName, columnName, sequenceName);

            String nextValSql = "SELECT nextval(?)";

            try (PreparedStatement ps = conn.prepareStatement(nextValSql)) {
                ps.setString(1, sequenceName);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }

        private void updateSequenceNextValue(Connection conn, String tableName, String columnName, String sequenceName) throws SQLException {
            String setValSql = String.format(
                    "SELECT setval('%s', (SELECT COALESCE(MAX(%s), 0) FROM %s))",
                    sequenceName, columnName, tableName
            );

            try (PreparedStatement ps = conn.prepareStatement(setValSql)) {
                ps.executeQuery();
            }
        }
    }
