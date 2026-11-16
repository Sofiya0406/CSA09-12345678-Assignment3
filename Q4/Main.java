import java.util.*;
import java.util.concurrent.*;

class StationThread extends Thread {
    final int scripts; volatile int done = 0;
    StationThread(String name, int scripts) { super(name); this.scripts = scripts; }
    @Override public void run() {
        for (int i = 0; i < scripts; i++) {
            done++;
            try { Thread.sleep(5); } catch (InterruptedException e) { return; }
        }
        System.out.println(getName() + " finished: " + done);
    }
}

class StationTask implements Runnable {
    final String name; final int scripts; volatile int done = 0;
    StationTask(String name, int scripts) { this.name = name; this.scripts = scripts; }
    public void run() {
        for (int i = 0; i < scripts; i++) {
            done++;
            try { Thread.sleep(5); } catch (InterruptedException e) { return; }
        }
        System.out.println(name + " finished: " + done);
    }
}

class Main {
    static void burn(String name) {
        long x = 0;
        for (long i = 0; i < 100_000_000L; i++) x += i;
        System.out.println(name + " done " + (x > 0));
    }

    public static void main(String[] args) throws Exception {
        System.out.println("--- 1. extends Thread ---");
        StationThread[] st = new StationThread[4];
        for (int i = 0; i < 4; i++) {
            st[i] = new StationThread("Station-" + (i + 1), 10 * (i + 1));
            st[i].start();
        }
        System.out.println("Station-4 alive right after start: " + st[3].isAlive());
        int total = 0;
        for (StationThread s : st) { s.join(); total += s.done; }
        System.out.println("Total scanned: " + total + " (expected 100), Station-4 alive after join: " + st[3].isAlive());

        System.out.println("--- 2. implements Runnable ---");
        StationTask[] tk = new StationTask[4];
        Thread[] th = new Thread[4];
        for (int i = 0; i < 4; i++) {
            tk[i] = new StationTask("Runnable-Station-" + (i + 1), 10 * (i + 1));
            th[i] = new Thread(tk[i]);
            th[i].start();
        }
        total = 0;
        for (int i = 0; i < 4; i++) { th[i].join(); total += tk[i].done; }
        System.out.println("Total scanned: " + total + " (expected 100)");

        System.out.println("--- 3. run() instead of start() ---");
        Runnable r = () -> System.out.println("body runs in thread: " + Thread.currentThread().getName());
        new Thread(r, "Worker").run();
        Thread w = new Thread(r, "Worker"); w.start(); w.join();

        System.out.println("--- 4. Priorities ---");
        Thread lo = new Thread(() -> burn("Low-priority"));
        Thread hi = new Thread(() -> burn("High-priority"));
        lo.setPriority(Thread.MIN_PRIORITY); hi.setPriority(Thread.MAX_PRIORITY);
        lo.start(); hi.start(); lo.join(); hi.join();

        System.out.println("--- 5. ExecutorService, pool of 2 ---");
        ExecutorService ex = Executors.newFixedThreadPool(2);
        long s0 = System.nanoTime();
        int[] counts = {10, 20, 30, 40};
        for (int i = 0; i < 4; i++) ex.submit(new StationTask("Pool-Station-" + (i + 1), counts[i]));
        ex.shutdown(); ex.awaitTermination(10, TimeUnit.SECONDS);
        System.out.printf("Pool of 2 finished in %.0f ms%n", (System.nanoTime() - s0) / 1e6);

        System.out.println("--- 6. Daemon thread ---");
        Thread d = new Thread(() -> {
            try { Thread.sleep(2000); System.out.println("daemon finished"); }
            catch (InterruptedException e) { }
        });
        d.setDaemon(true); d.start();
        System.out.println("main ends now, daemon will NOT finish");
    }
          }
