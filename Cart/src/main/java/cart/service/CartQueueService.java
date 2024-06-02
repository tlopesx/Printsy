package cart.service;

import cart.model.Cart;
import cart.model.Product;
import cart.queue.CartItemTask;
import cart.queue.CartQueue;
import cart.repository.CartRepository;
import cart.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.logging.Logger;

@Service
public class CartQueueService {

    private static final Logger LOGGER = Logger.getLogger(CartQueueService.class.getName());

    // Use ConcurrentHashMap for thread-safe operations
    private final Map<String, CartQueue> queueMap = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> executorMap = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Duration delay = Duration.ofSeconds(120);
    private final TaskSchedulerService taskSchedulerService;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;

    @Autowired
    public CartQueueService(CartRepository cartRepository, ProductRepository productRepository, TaskSchedulerService taskSchedulerService){
        this.taskSchedulerService = taskSchedulerService;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        startProcessing();
    }

    // Adds a new CartQueue to the dictionary with the given imageId
    public void addToQueue(String imageId, CartItemTask cartItemTask) {
        CartQueue cartQueue = queueMap.computeIfAbsent(imageId, k -> new CartQueue());
        LOGGER.info("Added to queue: " + imageId);
        if (cartQueue.enqueue(cartItemTask)) {
            LOGGER.info("success");
        } else {
            LOGGER.severe("failed to enqueue task");
        }
    }

    public int checkImagesInQueue(String imageId) {
        CartQueue imageQueue = queueMap.get(imageId);
        if (imageQueue != null) {
            return imageQueue.getQueueSize();
        } else {
            return 0;
        }
    }

    private void startProcessing() {
        Executors.newCachedThreadPool().submit(this::processAllQueues);
    }

    public void processAllQueues() {
        while (running.get()) {
            try {
                queueMap.forEach((imageId, cartQueue) -> {
                    if (!executorMap.containsKey(imageId)) {
                        executorMap.put(imageId, Executors.newSingleThreadExecutor());
                        executorMap.get(imageId).submit(() -> processQueue(imageId));
                    }
                });
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processQueue(String imageId) {
        CartQueue cartQueue = queueMap.get(imageId);
        while (running.get() && cartQueue != null) {
            try {
                CartItemTask task = cartQueue.dequeue();
                if (task != null) {
                    processTask(task);
                } else {
                    Thread.sleep(1000); // Wait before checking the queue again
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void shutdown() {
        running.set(false);
        executorMap.values().forEach(ExecutorService::shutdown);
        try {
            for (ExecutorService executorService : executorMap.values()) {
                if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
            executorMap.values().forEach(ExecutorService::shutdownNow);
        }
    }



    private void processTask(CartItemTask task){
        // Create product and add to Products table
        Product newProduct = new Product(
                task.getImageId(),
                task.getStockId(),
                task.getPrice());
        productRepository.save(newProduct);

        Cart newCartItem = new Cart(
                task.getUserId(),
                newProduct);
        cartRepository.save(newCartItem);


        if (newProduct.getProductId() == null ) {
            String errorMessage = "Failed to save product in the database for Image ID: " + task.getImageId();
            LOGGER.severe(errorMessage);
            return;
        }

        scheduleCartCleanupWithDelay(task.getUserId());

        // Add product to cart queue
        LOGGER.info("Product created with Product ID: " + newProduct.getProductId() + ". Added to cart.");

    }

    public void scheduleCartCleanupWithDelay(Long userId){
        Instant scheduledTime = Instant.now().plus(delay);
        scheduleCartCleanup(userId, scheduledTime);
    }

    public void scheduleCartCleanup(Long userId, Instant scheduledTime) {
        LOGGER.info("Scheduling cleanup task for used ID: " + userId);
        Runnable cleanupTask = () -> {

            LOGGER.info("Running scheduled cart cleanup for user ID: " + userId);

            List<Cart> cartItems = cartRepository.findAllByUserId(userId);

            if (cartItems.isEmpty()) {
                throw new RuntimeException("No cart items found for user with ID " + userId);
            }

            cartRepository.deleteAll(cartItems);
        };

        taskSchedulerService.scheduleTask(userId, cleanupTask, scheduledTime);
    }

}
