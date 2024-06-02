package cart.service;
import cart.dto.ProductResult;
import cart.model.Product;
import cart.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final GalleryService galleryService;
    private final ProductRepository productRepository;

    @Autowired
    public ProductService(GalleryService galleryService, ProductRepository productRepository) {
        this.galleryService = galleryService;
        this.productRepository = productRepository;
    }

    public ProductResult convertToProductResult(Product product) {
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


    public List<ProductResult> convertToProductResults(List<Product> products) {
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

    public ProductResult getProductById(Long productId) {

        Optional<Product> product = productRepository.findByProductId(productId);

        if (product.isPresent()) {
            return convertToProductResult(product.get());
        }
        else {
            throw new RuntimeException("Product with ID " + productId + " not found");
        }
    }

    public List<ProductResult> getAllProducts() {
        List<Product> allProducts = productRepository.findAll();
        if (allProducts.isEmpty()) {
            throw new RuntimeException("No products found");
        }
        return convertToProductResults(allProducts);
    }
}
