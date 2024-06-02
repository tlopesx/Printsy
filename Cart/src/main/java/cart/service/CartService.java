package cart.service;

import cart.dto.CartResult;
import cart.dto.ProductResult;
import cart.model.Cart;
import cart.model.Product;
import cart.dto.TransactionInput;
import cart.queue.CartItemTask;
import cart.queue.CartQueue;
import cart.repository.CartRepository;
import cart.repository.ProductRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;

@Service
public class CartService {

    private static final Logger LOGGER = Logger.getLogger(CartService.class.getName());

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    public final ProductService productService;
    private final CleanUpService cleanUpService;
    private final TaskSchedulerService taskSchedulerService;
    private final CartQueueService cartQueueService;
    private final TransactionGatewayService transactionGatewayService;


    @Autowired
    public CartService(CartRepository cartRepository, ProductRepository productRepository, ProductService productService, CleanUpService  cleanUpService,
                       TaskSchedulerService taskSchedulerService, CartQueue cartQueue,
                       TransactionGatewayService transactionGatewayService, CartQueueService cartQueueService, GalleryService galleryService) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.cleanUpService = cleanUpService;
        this.taskSchedulerService = taskSchedulerService;
        this.transactionGatewayService = transactionGatewayService;
        this.cartQueueService = cartQueueService;

    }

    public boolean isImageAvailable(String imageId) {
        // Determine availability (assuming a threshold of 10)
        return getImageCountByImageId(imageId) < 10;
    }

    public boolean completePurchase(Long userId) {
        // Cancel the scheduled task
        List<Cart> cartItems = cartRepository.findAllByUserId(userId);
        List<TransactionInput> transactionInputs = cartItems.stream()
                .map(Cart::toTransactionInput)
                .toList();

        if (transactionInputs.isEmpty()) {
            LOGGER.info("No items in the cart for user ID " + userId);
            return false;
        }
        try {
            boolean success = transactionGatewayService.completeTransaction(transactionInputs);
            if (success) {
                cleanUpService.deleteCartAndProductEntitiesByUser(userId);
                taskSchedulerService.cancelScheduledTask(userId);
            }
            return success;
        } catch (Exception e) {
            LOGGER.severe("Failed to execute transaction");
            return false;
        }
    }

    public Integer getImageCountByImageId(String imageId) {

        int countInCartQueues = cartQueueService.checkImagesInQueue(imageId);
        int countInCarts = cartRepository.countByProductImageId(imageId);
        int countInTransactions = transactionGatewayService.getTransactionImageAvailability(imageId);

        return countInCartQueues + countInCarts + countInTransactions;
    }

    public List<CartResult> getCartItemsByUserId(Long userId) {
        List<Cart> cartItems = cartRepository.findAllByUserId(userId);
        return convertToCartResults(cartItems);
    }

    public List<ProductResult> checkCartProductsByUserId(Long userId) {
        List<Cart> cartItems = cartRepository.findAllByUserId(userId);

        List<Product> products = cartItems.stream()
                .map(Cart::getProduct)
                .toList();

        if (products.isEmpty()) {
            throw new RuntimeException("No products found for user with ID " + userId);
        }
        return productService.convertToProductResults(products);
    }

    public void deleteCartItemsByUserId(Long userId) {
        cleanUpService.deleteCartAndProductEntitiesByUser(userId);
    }

    public String addItemToCart(String imageId, Long stockId, Integer price, Long userId) {

        LOGGER.info("Attempting to create product with Image ID: " + imageId + ", Stock ID: " + stockId + ", Price: " + price);

        // Check if image is available
        if (!isImageAvailable(imageId)) {
            String errorMessage = "Image with ID " + imageId + " is not available";
            LOGGER.warning(errorMessage);
            return "limit exceeded";
        }
        CartItemTask task = new CartItemTask(imageId, stockId, price, userId);
        cartQueueService.addToQueue(imageId, task);
        return "successfully added";
    }    

    public Long getRemainingCleanupTime(Long userId) {
        Long remainingTime = taskSchedulerService.getRemainingTime(userId).getSeconds();
        LOGGER.info("Remaining clean up time for userID " + userId + ": " + remainingTime + "seconds");
        return remainingTime;
    }

    public List<CartResult> convertToCartResults(List<Cart> cartItems) {
        // Fetch all imageUrls at once if possible to minimize calls
        List<Product> products = cartItems.stream()
                .map(Cart::getProduct)
                .toList();
        List<ProductResult> productResults = productService.convertToProductResults(products);

        List<CartResult> cartResults = new ArrayList<>();
        // Convert each product to ProductResult and collect into a list
        for (int i = 0; i < cartItems.size(); i++) {
            CartResult cartResult = new CartResult(cartItems.get(i));
            cartResult.setProductResult(productResults.get(i));
            cartResults.add(cartResult);
        }
        return cartResults;
    }

}