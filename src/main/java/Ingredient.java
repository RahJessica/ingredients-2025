import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


public class Ingredient {

    private Integer id;
    private String name;
    private CategoryEnum category;
    private Double price;
    private List<StockMovement> stockMovementList;

    public Ingredient(Integer id, String name, CategoryEnum category, Double price, List<StockMovement> stockMovementList) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockMovementList = stockMovementList;
    }

    public Ingredient() {
    }

    public Ingredient(Integer id) {
        this.id = id;
    }

    public Ingredient(Integer id, String name, CategoryEnum category, Double price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryEnum getCategory() {
        return category;
    }

    public void setCategory(CategoryEnum category) {
        this.category = category;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public List<StockMovement> getStockMovementList() {
        return stockMovementList;
    }

    public void setStockMovementList(List<StockMovement> stockMovementList) {
        this.stockMovementList = stockMovementList;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && category == that.category
                && Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, category, price);
    }

    @Override
    public String toString() {
        return "Ingredient{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", price=" + price +
                ", stockMovementList=" + stockMovementList +
                '}';
    }

    /* public StockValue getStockValueAt(Instant t) {
        if (this.id == null) {
            throw new RuntimeException("Ingredient id is null");
        }

        String sql = """
        SELECT
        SUM(CASE WHEN type = 'IN' THEN quantity ELSE 0 END) AS total_in,
        SUM(CASE WHEN type = 'OUT' THEN quantity ELSE 0 END) AS total_out,
        MIN(unit) AS unit
         FROM stock_movement
         WHERE id_ingredient = ? AND creation_datetime <= ?                                
    """;

        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, this.id);
            ps.setTimestamp(2, Timestamp.from(t));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double totalIn = rs.getDouble("total_in");
                    double totalOut = rs.getDouble("total_out");
                    String unitStr = rs.getString("unit");

                    UnitEnum unit = (unitStr != null) ? UnitEnum.valueOf(unitStr) : UnitEnum.KG;

                    return new StockValue(totalIn - totalOut, unit);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // pas de mouvement → stock = 0
        return new StockValue(0.0, UnitEnum.KG);
    }

 */
    
    public StockValue getStockValueAt(Instant t) {
        if (stockMovementList == null || stockMovementList.isEmpty()) return null;

        Map<UnitEnum, List<StockMovement>> unitSet = stockMovementList.stream()
                .filter(sm -> sm.getValue() != null && sm.getValue().getUnit() != null)
                .collect(Collectors.groupingBy(sm -> sm.getValue().getUnit()));

        if (unitSet.keySet().size() > 1) {
            throw new RuntimeException("Multiple unit found and not handle for conversion");
        }

        List<StockMovement> stockMovements = stockMovementList.stream()
                .filter(sm -> sm.getCreationDatetime() != null && !sm.getCreationDatetime().isAfter(t))
                .collect(Collectors.toList());

        double movementIn = stockMovements.stream()
                .filter(sm -> MovementTypeEnum.IN.equals(sm.getType()))
                .mapToDouble(sm -> sm.getValue().getQuantity())
                .sum();

        double movementOut = stockMovements.stream()
                .filter(sm -> MovementTypeEnum.OUT.equals(sm.getType()))
                .mapToDouble(sm -> sm.getValue().getQuantity())
                .sum();

        UnitEnum unit = unitSet.keySet().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No unit found"));

        StockValue stockValue = new StockValue();
        stockValue.setQuantity(movementIn - movementOut);
        stockValue.setUnit(unit);

        return stockValue;
    }

}
