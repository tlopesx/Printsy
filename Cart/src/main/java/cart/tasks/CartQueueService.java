package cart.tasks;

import cart.model.Cart;
import cart.model.Product;
import cart.repository.CartRepository;
import cart.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    // Delay config
    @Value("${printsy.expiration.delay}")
    private int delayInSeconds;
    private final Duration delay = Duration.ofSeconds(delayInSeconds);

    // Dependencies
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
    public boolean addToQueue(String imageId, PendingCartItem pendingCartItem) {
        CartQueue cartQueue = queueMap.computeIfAbsent(imageId, k -> new CartQueue());
        LOGGER.info("Added to queue: " + imageId);

        boolean addedToQueue = cartQueue.enqueue(pendingCartItem);
        if (addedToQueue) {
            LOGGER.info("success");
        } else {
            LOGGER.severe("failed to enqueue task");
        }

        return addedToQueue;
    }

    public int countImagesInQueue(String imageId) {
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
                PendingCartItem pendingCartItem = cartQueue.dequeue();
                if (pendingCartItem != null) {
                    processTask(pendingCartItem);
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



    private void processTask(PendingCartItem pendingCartItem){
        // Create product and add to Products table
        Product newProduct = new Product(
                pendingCartItem.getImageId(),
                pendingCartItem.getStockId(),
                pendingCartItem.getPrice());
        productRepository.save(newProduct);

        Cart newCartItem = new Cart(
                pendingCartItem.getUserId(),
                newProduct);
        cartRepository.save(newCartItem);


        if (newProduct.getProductId() == null ) {
            LOGGER.severe("Failed to save product in the database for Image ID: " + pendingCartItem.getImageId());
        }
        else {
            LOGGER.info("Product created with Product ID: " + newProduct.getProductId() + ". Added to cart.");
            scheduleCartCleanup(pendingCartItem.getUserId(), Instant.now().plus(delay));
        }


    }

    public void scheduleCartCleanup(Long userId, Instant scheduledTime) {
        LOGGER.info("Scheduling cleanup task for used ID: " + userId);

        PendingCleanUpTask cleanUpTask = new PendingCleanUpTask(userId, cartRepository);
        taskSchedulerService.scheduleTask(userId, cleanUpTask, scheduledTime);
    }

}
