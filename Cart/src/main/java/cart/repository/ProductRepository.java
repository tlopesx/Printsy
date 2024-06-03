package cart.repository;

import cart.model.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findByProductId(Long productId);

    @Query("SELECT p FROM Product p WHERE p.productId NOT IN (SELECT c.product.productId FROM Cart c WHERE c.userId = :userId)")
    List<Product> findProductsByUserId(@Param("userId") Long userId);


    @Query("SELECT p FROM Product p WHERE p.productId NOT IN (SELECT c.product.productId FROM Cart c)")
    List<Product> findProductsNotInCart();
    
}