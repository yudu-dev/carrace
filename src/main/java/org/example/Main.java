package org.example;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Main {

    private static final int COUNT_CARS_IN_TUNNEL = 3;
    private static final int COUNT_CARS = 10;

    private static final Semaphore tunnelSemaphore = new Semaphore(COUNT_CARS_IN_TUNNEL);
    private static final ExecutorService executorService = Executors.newFixedThreadPool(COUNT_CARS);
    private static final CyclicBarrier cyclicBarrier = new CyclicBarrier(COUNT_CARS);
    private static final CountDownLatch countDownLatch = new CountDownLatch(COUNT_CARS);
    private static final ConcurrentHashMap<Integer, Long> score = new ConcurrentHashMap<>();

    private static final Object monitor = new Object();
    private static int winnerIndex = -1;

    public static void main(String[] args) {
        for (int i = 0; i < COUNT_CARS; i++) {
            final int car = i;
            executorService.execute(() -> {
                long before = System.currentTimeMillis();
                preparing(car);
                try {
                    cyclicBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
                firstRoad(car);
                tunnel(car);
                secondRoad(car);
                synchronized (monitor) {
                    if (winnerIndex == -1) {
                        winnerIndex = car;
                    }
                }
                long after = System.currentTimeMillis();
                score.put(car, after - before);
                countDownLatch.countDown();
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (int key : score.keySet()) {
            System.out.println(key + ": " + score.get(key));
        }
        System.out.println("Winner - " + winnerIndex + ": "+ score.get(winnerIndex));
        executorService.shutdown();
    }

    private static void sleepRandomTime() {
        long millis = (long) (Math.random() * 5000 + 1000);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void preparing(int car) {
        System.out.println(car + " started preparing for race");
        sleepRandomTime();
        System.out.println(car + " finished preparing for race");
    }

    private static void firstRoad(int car) {
        System.out.println(car + " started race");
        sleepRandomTime();
        System.out.println(car + " finished first road");
    }

    private static void tunnel(int car) {
        try {
            tunnelSemaphore.acquire();
            System.out.println(car + " continued race into tunnel");
            sleepRandomTime();
            System.out.println(car + " finished tunnel");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            tunnelSemaphore.release();
        }
    }

    private static void secondRoad(int car) {
        System.out.println(car + " started final road");
        sleepRandomTime();
        System.out.println(car + " finished race");
    }

}
