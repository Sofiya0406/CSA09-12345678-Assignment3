import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

abstract class Screen {
    static volatile double sink;
    static void pricing() { double x = 0; for (int i = 0; i < 2000; i++) x += Math.sqrt(i); sink = x; }
    abstract void book(int seat);
    abstract int made();
}
class Unsafe extends Screen {
    final boolean[] b; int made = 0;
    Unsafe(int n) { b = new boolean[n]; }
    void book(int seat) { pricing(); if (!b[seat]) { Thread.yield(); b[seat] = true; made++; } }
    int made() { return made; }
}
class SyncMethod extends Screen {
    final boolean[] b; int made = 0;
    SyncMethod(int n) { b = new boolean[n]; }
    synchronized void book(int seat) { pricing(); if (!b[seat]) { Thread.yield(); b[seat] = true; made++; } }
    synchronized int made() { return made; }
}
class SyncBlock extends Screen {
    final boolean[] b; int made = 0;
    SyncBlock(int n) { b = new boolean[n]; }
    void book(int seat) {
        pricing();                                   // outside the lock
        synchronized (this) { if (!b[seat]) { Thread.yield(); b[seat] = true; made++; } }
    }
    synchronized int made() { return made; }
}
class AtomicScreen extends Screen {
    final AtomicBoolean[] b; final AtomicInteger made = new AtomicInteger();
    AtomicScreen(int n) { b = new AtomicBoolean[n]; for (int i = 0; i < n; i++) b[i] = new AtomicBoolean(); }
    void book(int seat) { pricing(); if (b[seat].compareAndSet(false, true)) made.incrementAndGet(); }
    int made() { return made.get(); }
}

class SeatBooking {                                   // the given program, TODO 1 completed
    private final boolean[] booked;
    private int bookingsMade = 0;
    SeatBooking(int seats) { booked = new boolean[seats]; }
    synchronized void book(int seat) {
        if (!booked[seat]) { booked[seat] = true; bookingsMade++; }
    }
    public static void main(String[] args) throws InterruptedException {
        SeatBooking screen = new SeatBooking(10);
        Thread[] users = new Thread[100];
        for (int i = 0; i < users.length; i++) {
            final int seat = i % 10;
            users[i] = new Thread(() -> screen.book(seat));
            users[i].start();
        }
        for (Thread t : users) t.join();
        System.out.println("Seats sold: " + screen.bookingsMade);
    }
}

class Main {
    static String[] names = {"Unsafe", "synchronized method", "synchronized block", "AtomicBoolean CAS"};
    static Screen make(int k, int n) {
        switch (k) {
            case 0: return new Unsafe(n);
            case 1: return new SyncMethod(n);
            case 2: return new SyncBlock(n);
            default: return new AtomicScreen(n);
        }
    }
    static int race(Screen s) throws Exception {
        Thread[] u = new Thread[100];
        for (int i = 0; i < 100; i++) { final int seat = i % 10; u[i] = new Thread(() -> s.book(seat)); u[i].start(); }
        for (Thread t : u) t.join();
        return s.made();
    }
    static double timing(Screen s) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(8);
        long st = System.nanoTime();
        for (int i = 0; i < 10000; i++) { final int seat = i % 1000; ex.submit(() -> s.book(seat)); }
        ex.shutdown(); ex.awaitTermination(60, TimeUnit.SECONDS);
        return (System.nanoTime() - st) / 1e6;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("--- Given program (TODO 1 completed) ---");
        SeatBooking.main(args);

        System.out.println("--- 100 users, 10 seats, 10 runs: seats sold (correct = 10) ---");
        for (int k = 0; k < 4; k++) {
            StringBuilder sb = new StringBuilder();
            for (int run = 0; run < 10; run++) sb.append(race(make(k, 10))).append(' ');
            System.out.println(names[k] + ": " + sb);
        }

        System.out.println("--- 10000 bookings, 8 threads, avg of 3 runs ---");
        for (int k = 0; k < 4; k++) {
            double t = 0; int sold = 0;
            for (int run = 0; run < 3; run++) { Screen s = make(k, 1000); t += timing(s); sold = s.made(); }
            System.out.printf("%-20s %.1f ms   seats sold = %d (correct = 1000)%n", names[k], t / 3, sold);
        }
    }
      }
