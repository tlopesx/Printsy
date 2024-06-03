package cart.service;

import cart.dto.CartResult;
import cart.model.Cart;
import cart.dto.TransactionInput;
import cart.service.integration.TransactionGatewayService;
import cart.tasks.*;
import cart.repository.CartRepository;

import cart.util.DTOMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;

@Service
public class CartService {

    private static final Logger LOGGER = Logger.getLogger(CartService.class.getName());

    private final CartRepository cartRepository;
    private final QueueManager queueManager;
    private final DTOMapper dtoMapper;
    private final TransactionGatewayService transactionGatewayService;

    @Value("${printsy.image.count}")
    private int MAX_IMAGE_COUNT;


    @Autowired
    public CartService(CartRepository cartRepository,
                       TransactionGatewayService transactionGatewayService, QueueManager queueManager, DTOMapper dtoMapper) {
        this.cartRepository = cartRepository;
        this.transactionGatewayService = transactionGatewayService;
        this.queueManager = queueManager;
        this.dtoMapper = dtoMapper;
    }

    public boolean isImageAvailable(String imageId) {
        return getImageCountByImageId(imageId) < MAX_IMAGE_COUNT;
    }

    public boolean completePurchase(Long userId) {
        // Cancel the scheduled task
        List<Cart> cartItems = cartRepository.findAllByUserId(userId);
        List<TransactionInput> transactionInputs = cartItems.stream()
                .map(cart -> dtoMapper.convertCartItemToTransactionInput(cart))
                .toList();

        if (transactionInputs.isEmpty()) {
            LOGGER.info("No items in the cart for user ID " + userId);
            return false;
        }
        try {
            boolean success = transactionGatewayService.completeTransaction(transactionInputs);
            if (success) {
                queueManager.deleteCartAndProductEntitiesByUser(userId);
                queueManager.cancelScheduledTask(userId);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Failed to execute transaction");
            return false;
        }
    }

    public Integer getImageCountByImageId(String imageId) {
        int countInCarts = cartRepository.countByProductImageId(imageId);
        int countInCartQueues = queueManager.checkImagesInQueue(imageId);
        int countInTransactions = transactionGatewayService.getTransactionImageAvailability(imageId);

        return countInCartQueues + countInCarts + countInTransactions;
    }

    public List<CartResult> getCartItemsByUserId(Long userId) {
        List<Cart> cartItems = cartRepository.findAllByUserId(userId);
        return dtoMapper.convertCartItemsToCartResults(cartItems);
    }

    public void deleteCartItemsByUserId(Long userId) {
        LOGGER.info("Deleting all cart entries and products for userID: " + userId);
        queueManager.deleteCartAndProductEntitiesByUser(userId);
    }

    public String addItemToCart(String imageId, Long stockId, Integer price, Long userId) {

        LOGGER.info("Attempting to create product with Image ID: " + imageId + ", Stock ID: " + stockId + ", Price: " + price);

        // Check if image is available
        if (!isImageAvailable(imageId)) {
            String errorMessage = "Image with ID " + imageId + " is not available";
            LOGGER.warning(errorMessage);
            return "limit exceeded";
        }
        PendingCartItem task = queueManager.createTask(imageId, stockId, price, userId);
        boolean success = queueManager.addToQueue(imageId, task);
        if (success) {
            return "successfully added item to cart";
        } else {
            return "failed to add item to cart";
        }
    }

    public Long getRemainingCleanupTime(Long userId) {
        Long remainingTime = queueManager.getRemainingCartTime(userId).getSeconds();
        LOGGER.info("Remaining clean up time for userID " + userId + ": " + remainingTime + "seconds");
        return remainingTime;
    }
}