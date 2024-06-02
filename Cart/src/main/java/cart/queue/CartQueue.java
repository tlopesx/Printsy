package cart.queue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class CartQueue {
    private Queue<CartItemTask> queue = new ConcurrentLinkedQueue<>();

    @Value("${printsy.image.count}")
    private int MAX_IMAGE_COUNT; // there will be a maximum of 10 objects in the queue

    public boolean enqueue(CartItemTask task) {
        if (queue.size() >= MAX_IMAGE_COUNT) {
            return false;
        }
        queue.add(task);
        return true;
    }

    public CartItemTask dequeue() {
        return queue.poll();
    }

    public CartItemTask peekQueue() {
        return queue.peek();
    }

    public int getQueueSize() {
        return queue.size();
    }
}