package cart.tasks;

import java.time.Duration;
import java.time.Instant;

public interface QueueManager {

    PendingCartItem createTask(String imageId, Long stockId, Integer price, Long userId);
    boolean addToQueue(String imageId, PendingCartItem task);
    void cancelScheduledTask(Long userId);
    void scheduleTask(Long userId, Runnable task, Instant scheduledTime);
    Duration getRemainingCartTime(Long userId);
    int checkImagesInQueue(String imageId);
    void deleteCartAndProductEntitiesByUser(Long userId);
}
