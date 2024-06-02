package cart.repository;

import cart.model.Cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Date;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Integer countByProductImageId(String imageId);

    List<Cart> findAllByUserId(Long userId);

    List<Cart> findByExpirationTimeBefore(Instant currentTime);

    @Query("SELECT DISTINCT c.userId FROM Cart c")
    List<Long> findDistinctUserIds();

    Cart findTopByUserId(Long userId);

}
