package info.kgeorgiy.ja.ilyin.concurrent;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author Yaroslav Ilin
 */
public class WaitingQueue {
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private boolean isClosed = false;

    /**
     * add task to queue
     *
     * @param task to add
     */
    public synchronized void add(Runnable task) {
        if (!isClosed) {
            tasks.add(task);
            notify();
        }
    }

    /**
     * get next task to execution
     *
     * @return task
     * @throws InterruptedException if queue was close or wait failed
     */
    public synchronized Runnable get() throws InterruptedException {
        if (!isClosed) {
            while (tasks.isEmpty()) {
                wait();
            }
            return tasks.poll();
        } else {
            throw new RuntimeException("Queue was close");
        }
    }

    /**
     * get all tasks
     *
     * @return queue with tasks
     */
    public synchronized Queue<Runnable> getTasks() {
        return tasks;
    }

    /**
     * Close queue
     */
    public synchronized void close() {
        isClosed = true;
    }
}
