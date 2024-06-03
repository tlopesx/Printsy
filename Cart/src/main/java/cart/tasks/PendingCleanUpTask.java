package cart.tasks;

import cart.model.Cart;
import cart.repository.CartRepository;
import java.util.List;
import java.util.logging.Logger;

public class PendingCleanUpTask implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(PendingCleanUpTask.class.getName());
    private final Long userId;
    private final CartRepository cartRepository;

    public PendingCleanUpTask(Long userId, CartRepository cartRepository) {
        this.userId = userId;
        this.cartRepository = cartRepository;
    }

    @Override
    public void run() {
        LOGGER.info("Running scheduled cart cleanup for user ID: " + userId);
        List<Cart> cartItems = cartRepository.findAllByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("No cart items found for user with ID " + userId);
        }
        cartRepository.deleteAll(cartItems);
    }
}
