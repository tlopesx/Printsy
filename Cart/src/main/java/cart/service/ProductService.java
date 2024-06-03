package cart.service;
import cart.dto.ProductResult;
import cart.model.Product;
import cart.repository.ProductRepository;
import cart.service.integration.GalleryService;
import cart.util.DTOMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final GalleryService galleryService;
    private final ProductRepository productRepository;
    private final DTOMapper dtoMapper;

    @Autowired
    public ProductService(GalleryService galleryService, ProductRepository productRepository, DTOMapper dtoMapper) {
        this.galleryService = galleryService;
        this.productRepository = productRepository;
        this.dtoMapper = dtoMapper;
    }

    public ProductResult getProductById(Long productId) {

        Optional<Product> product = productRepository.findByProductId(productId);

        if (product.isPresent()) {
            return dtoMapper.convertProductToProductResult(product.get());
        }
        else {
            throw new RuntimeException("Product with ID " + productId + " not found");
        }
    }

    public List<ProductResult> getProductsByUserId(Long userId) {
        List<Product> products = productRepository.findProductsByUserId(userId);

        if (products.isEmpty()) {
            throw new RuntimeException("No products found for user with ID " + userId);
        }
        return dtoMapper.convertProductsToProductResults(products);
    }

    public List<ProductResult> getAllProducts() {
        List<Product> allProducts = productRepository.findAll();
        if (allProducts.isEmpty()) {
            throw new RuntimeException("No products found");
        }
        return dtoMapper.convertProductsToProductResults(allProducts);
    }
}
