package com.github.kahalemakai.whirlpool;

import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class WhirlpoolSingleThreadedTest {
    private Whirlpool<Integer> pool;
    private final long expirationTime = 100;
    private volatile int counter;
    private static final Method TRIGGER_EVICTION;
    static {
        Method m = null;
        try {
            m = Whirlpool.class.getDeclaredMethod("triggerEviction");
            m.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        TRIGGER_EVICTION = m;
    }

    @Test
    public void autoClosing() throws Exception {
        try (val autoClosing = pool.borrowKindly()) {
            autoClosing.get();
            assertEquals(1, pool.totalSize());
        }
        assertEquals(1, pool.availableElements());
        TRIGGER_EVICTION.invoke(pool);
        Thread.sleep(2 * expirationTime);
        assertEquals(0, pool.availableElements());
    }

    @Test
    public void evict() throws Exception {
        val borrowed = pool.borrow();
        assertEquals(Integer.valueOf(1), Integer.valueOf(pool.totalSize()));
        Thread.sleep(2 * expirationTime);
        assertEquals(Integer.valueOf(1), Integer.valueOf(pool.totalSize()));
        pool.unhand(borrowed);
        assertEquals(Integer.valueOf(1), Integer.valueOf(pool.totalSize()));
        TRIGGER_EVICTION.invoke(pool);
        Thread.sleep(2 * expirationTime);
        assertEquals(Integer.valueOf(0), Integer.valueOf(pool.totalSize()));
        assertEquals(borrowed, pool.borrow());
    }

    @Test
    public void evictAll() throws Exception {
        val borrowed = pool.borrow();
        val borrowed2 = pool.borrow();
        assertEquals(Integer.valueOf(2), Integer.valueOf(pool.totalSize()));
        Thread.sleep(2 * expirationTime);
        assertEquals(Integer.valueOf(2), Integer.valueOf(pool.totalSize()));
        pool.unhand(borrowed);
        pool.unhand(borrowed2);
        assertEquals(Integer.valueOf(2), Integer.valueOf(pool.totalSize()));
        TRIGGER_EVICTION.invoke(pool);
        Thread.sleep(2 * expirationTime);
        assertEquals(Integer.valueOf(0), Integer.valueOf(pool.totalSize()));
        assertEquals(borrowed, pool.borrow());
        assertEquals(borrowed2, pool.borrow());
    }

    @Test
    public void sizes() throws Exception {
        val list = new ArrayList<Integer>();
        list.add(pool.borrow());
        list.add(pool.borrow());
        list.add(pool.borrow());
        list.add(pool.borrow());
        pool.unhand(list.get(3));
        assertEquals(Integer.valueOf(4), Integer.valueOf(pool.totalSize()));
        assertEquals(Integer.valueOf(4), Integer.valueOf(pool.totalSizeEstimate()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(pool.availableElements()));
        assertEquals(Integer.valueOf(1), Integer.valueOf(pool.availableElementsEstimate()));
    }

    @Test
    public void borrowing() throws Exception {
        assertEquals(Integer.valueOf(0), pool.borrow());
        assertEquals(Integer.valueOf(1), pool.borrow());
        assertEquals(Integer.valueOf(2), pool.borrow());
        val borrowed = pool.borrow();
        assertEquals(Integer.valueOf(3), borrowed);
        pool.unhand(borrowed);
        assertEquals(Integer.valueOf(3), pool.borrow());
    }

    @Test
    public void validation() throws Exception {
        final int maxAllowedInt = 5;
        val intPool = Whirlpool.<Integer>builder()
                .expirationTime(expirationTime)
                .onCreate(() -> counter++)
                .onClose(t -> counter = 0)
                .onValidation(t -> t < maxAllowedInt)
                .build();
        val list = new ArrayList<Integer>();
        for (int i = 0; i <= maxAllowedInt + 1; ++i) {
            list.add(intPool.borrow());
            assertEquals(Integer.valueOf(i), list.get(i));
        }
        intPool.unhand(list.get(maxAllowedInt + 1));
        assertEquals(Integer.valueOf(0), intPool.borrow());
    }

    @Test
    public void expirationTime() throws Exception {
        assertEquals(expirationTime, pool.getExpirationTime());
    }

    @Test
    public void closeIsIdempotent() throws Exception {
        pool.close();
        pool.close();
    }

    @Test(expected = PoolException.class)
    public void closedInstanceThrows() throws Exception {
        pool.close();
        pool.borrow();
    }

    @Before
    public void setUp() throws Exception {
        counter = 0;
        pool = Whirlpool.<Integer>builder()
                .onCreate(() -> counter++)
                .expirationTime(expirationTime)
                .onClose(t -> counter--)
                .build();
    }

}