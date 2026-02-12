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
