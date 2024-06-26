package cart.model;

import cart.dto.CartResult;
import cart.dto.ProductResult;
import jakarta.persistence.*;

@Entity
@Table(name = "Products")
public class Product {

    @Id
    @Column(name = "product_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long productId;

    @Column(name = "image_id")
    private String imageId;

    @Column(name = "stock_id")
    private Long stockId;

    @Column(name = "price")
    private Integer price;


    public Product() {
    }

    public Product(String imageId, Long stockId, Integer price) {
        this.imageId = imageId;
        this.stockId = stockId;
        this.price = price;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

}
