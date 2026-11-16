import java.io.*;
import java.util.*;
import java.util.concurrent.*;

class Main {
    static boolean missing(int id) { return id % 5 == 0; }
    static boolean corrupt(int id) { return id == 3 || id == 7 || (id > 9 && id % 11 == 0); }

    static long run(int n, int pool) throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(pool);
        ConcurrentHashMap<Integer, Integer> scores = new ConcurrentHashMap<>();
        List<Future<Integer>> fs = new ArrayList<>();
        long st = System.nanoTime();
        for (int i = 0; i < n; i++) {
            final int id = i;
            fs.add(ex.submit(() -> {
                Thread.sleep(1);                                   // simulates reading a file
                if (missing(id)) throw new FileNotFoundException("score file " + id);
                if (corrupt(id)) throw new IllegalStateException("corrupt file " + id);
                int score = id % 100;
                scores.put(id, score);
                return score;
            }));
        }
        int ok = 0, miss = 0, bad = 0, other = 0;
        for (Future<Integer> f : fs) {
            try { f.get(); ok++; }
            catch (ExecutionException e) {                         // one failure never stops the run
                Throwable c = e.getCause();
                if (c instanceof FileNotFoundException) miss++;
                else if (c instanceof IllegalStateException) bad++;
                else other++;
            }
        }
        ex.shutdown();
        long ms = (System.nanoTime() - st) / 1000000;
        long sum = 0;
        for (int v : scores.values()) sum += v;
        System.out.println("n=" + n + " pool=" + pool + " | ok=" + ok + " missing=" + miss
            + " corrupt=" + bad + " other=" + other + " | map size=" + scores.size()
            + " score sum=" + sum + " | " + ms + " ms");
        return ms;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("--- Hand-checked set: 10 files ---");
        run(10, 2);
        System.out.println("--- 4000 files, pool 1, 2, 4, 8 ---");
        for (int p : new int[]{1, 2, 4, 8}) run(4000, p);
    }
    }
