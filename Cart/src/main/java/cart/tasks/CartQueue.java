package cart.tasks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class CartQueue {
    private Queue<PendingCartItem> queue = new ConcurrentLinkedQueue<>();
    private final ReentrantLock enqueueLock = new ReentrantLock();

    @Value("${printsy.image.count}")
    private int MAX_IMAGE_COUNT;

    public boolean enqueue(PendingCartItem pendingCartItem) {
        enqueueLock.lock(); // Lock to ensure only one thread can add at a time
        try {
            if (queue.size() >= MAX_IMAGE_COUNT) {
                return false;
            }
            queue.add(pendingCartItem);
            return true;
        } finally {
            enqueueLock.unlock(); // Ensure the lock is released after adding
        }
    }

    public PendingCartItem dequeue() {
        return queue.poll(); // Don't need locks for dequeue
    }

    public PendingCartItem peekQueue() {
        return queue.peek(); // Don't need locks for peeking
    }

    public int getQueueSize() {
        return queue.size(); // Don't need locks for size checking
    }
}