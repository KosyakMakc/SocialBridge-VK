package io.github.kosyakmakc.socialBridgeVk.Utils;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

public class CacheContainer<T extends Comparable<T>> {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final int cacheSize;
    private final LinkedList<T> cacheLine = new LinkedList<>();

    public CacheContainer() {
        this(500);
    }

    public CacheContainer(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public @Nullable T tryGet(Predicate<T> predicate) {
        AtomicReference<T> result = new AtomicReference<>(null);;
        executor.execute(() -> {
            for (var i = 0; i < cacheLine.size(); i++) {
                var item = cacheLine.get(i);
                if (predicate.test(item)) {
                    result.set(item);

                    // last accessed item place to near in cache line
                    cacheLine.remove(i);
                    cacheLine.addFirst(item);
                    return;
                }
            }
        });

        return result.get();
    }

    public void checkAndAdd(T newItem) {
        if (newItem == null) {
            throw new RuntimeException("null value not allowed here");
        }
        executor.execute(() -> {
            for (var i = 0; i < cacheLine.size(); i++) {
                var item = cacheLine.get(i);
                if (newItem.compareTo(newItem) == 0) {
                    // last accessed item place to near in cache line
                    cacheLine.remove(i);
                    cacheLine.addFirst(item);
                    return;
                }
            }

            if (cacheLine.size() >= cacheSize) {
                cacheLine.removeLast();
            }
            cacheLine.addFirst(newItem);
        });
    }
}
