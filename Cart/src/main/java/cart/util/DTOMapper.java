package cart.util;

import cart.dto.CartResult;
import cart.dto.ProductResult;
import cart.dto.TransactionInput;
import cart.model.Cart;
import cart.model.Product;
import cart.service.integration.GalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DTOMapper {

    private final GalleryService galleryService;

    @Autowired
    public DTOMapper(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    public List<CartResult> convertCartItemsToCartResults(List<Cart> cartItems) {

        List<Product> products = cartItems.stream()
                .map(Cart::getProduct)
                .toList();
        List<ProductResult> productResults = convertProductsToProductResults(products);

        List<CartResult> cartResults = new ArrayList<>();

        for (int i = 0; i < cartItems.size(); i++) {
            CartResult cartResult = new CartResult(cartItems.get(i));
            cartResult.setProductResult(productResults.get(i));
            cartResults.add(cartResult);
        }
        return cartResults;
    }

    public ProductResult convertProductToProductResult(Product product) {
        // Create a new ProductResult instance
        ProductResult productResult = new ProductResult();

        // Copy properties from Product to ProductResult
        productResult.setImageId(product.getImageId());
        productResult.setStockId(product.getStockId());
        productResult.setPrice(product.getPrice());

        // Fetch the imageUrl using the GalleryService
        String imageUrl = galleryService.getImageUrl(product.getImageId());
        productResult.setImageUrl(imageUrl);

        return productResult;
    }


    public List<ProductResult> convertProductsToProductResults(List<Product> products) {
        // Fetch all imageUrls at once if possible to minimize calls
        List<String> imageIds = products.stream()
                .map(Product::getImageId)
                .collect(Collectors.toList());
        Map<String, String> imageUrls = galleryService.getImageUrlsByImageIds(imageIds);

        // Convert each product to ProductResult and collect into a list
        return products.stream().map(product -> {
            ProductResult productResult = new ProductResult();
            productResult.setImageId(product.getImageId());
            productResult.setStockId(product.getStockId());
            productResult.setPrice(product.getPrice());

            // Use the fetched imageUrl from the map
            String imageUrl = imageUrls.get(product.getImageId());
            productResult.setImageUrl(imageUrl);

            return productResult;
        }).collect(Collectors.toList());
    }

    public TransactionInput convertCartItemToTransactionInput(Cart cartItem){
        return new TransactionInput(
                cartItem.getUserId(),
                cartItem.getProduct().getProductId(),
                cartItem.getProduct().getImageId());
    }

}
