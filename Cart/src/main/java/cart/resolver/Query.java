package cart.resolver;

import cart.dto.CartResult;
import cart.dto.ProductResult;
import cart.service.CartService;
import cart.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class Query {

    private final CartService cartService;
    private final ProductService productService;

    @Autowired
    public Query(CartService cartService, ProductService productService, ProductService productService1) {
        this.cartService = cartService;
        this.productService = productService1;
    }

    @QueryMapping
    public boolean findImageAvailability(@Argument String imageId) {
        return cartService.isImageAvailable(imageId);
    }

    @QueryMapping
    public Integer findImageByImageId(@Argument String imageId) {
        return cartService.getImageCountByImageId(imageId);
    }

    @QueryMapping
    public List<CartResult> findCartItemsByUserId(@Argument Long userId) {
        return cartService.getCartItemsByUserId(userId);
    }

    @QueryMapping
    public List<ProductResult> getCartProductsByUserId(@Argument Long userId) {
        return productService.getProductsByUserId(userId);
    }

    @QueryMapping
    public ProductResult findProductById(@Argument Long productId) {
        return productService.getProductById(productId);
    }

    @QueryMapping
    public List<ProductResult> findAllProducts() {
        return productService.getAllProducts();
    }

    @QueryMapping
    public Long getRemainingCleanupTime(@Argument Long userId) {
        return cartService.getRemainingCleanupTime(userId);
    }

    @QueryMapping
    public List<CartResult> findCartItemsByUserIdForPurchase(@Argument Long userId) {
        return cartService.getCartItemsByUserId(userId);
    }

}
