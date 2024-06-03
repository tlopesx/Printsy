package cart.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class QueueManagerImpl implements QueueManager {
    private final CartQueueService cartQueueService;
    private final TaskSchedulerService taskSchedulerService;
    private final CleanUpService cleanUpService;

    @Autowired
    public QueueManagerImpl(CartQueueService cartQueueService, TaskSchedulerService taskSchedulerService, CleanUpService cleanUpService) {
        this.cartQueueService = cartQueueService;
        this.taskSchedulerService = taskSchedulerService;
        this.cleanUpService = cleanUpService;
    }

    @Override
    public CartItemTask createTask(String imageId, Long stockId, Integer price, Long userId){
        return new CartItemTask(imageId, stockId, price, userId);
    }

    @Override
    public boolean addToQueue(String imageId, CartItemTask task) {
        return cartQueueService.addToQueue(imageId, task);
    }

    @Override
    public void cancelScheduledTask(Long userId) {
        taskSchedulerService.cancelScheduledTask(userId);
    }

    @Override
    public void scheduleTask(Long userId, Runnable task, Instant scheduledTime) {
        taskSchedulerService.scheduleTask(userId, task, scheduledTime);
    }

    @Override
    public Duration getRemainingCartTime(Long userId) {
        return taskSchedulerService.getRemainingTime(userId);
    }

    @Override
    public int checkImagesInQueue(String imageId) {
        return cartQueueService.checkImagesInQueue(imageId);
    }

    @Override
    public void deleteCartAndProductEntitiesByUser(Long userId) {
        cleanUpService.deleteCartAndProductEntitiesByUser(userId);
    }
}
