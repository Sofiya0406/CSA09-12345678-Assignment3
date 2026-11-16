class TokenCounter {
    private final int[] buffer = new int[5];
    private int count = 0, in = 0, out = 0;
    int maxSeen = 0, minSeen = 0;

    public synchronized void put(int token) throws InterruptedException {
        while (count == 5) wait();
        buffer[in] = token;
        in = (in + 1) % 5;
        count++;
        if (count > maxSeen) maxSeen = count;
        notifyAll();
    }
    public synchronized int take() throws InterruptedException {
        while (count == 0) wait();
        int t = buffer[out];
        out = (out + 1) % 5;
        count--;
        if (count < minSeen) minSeen = count;
        notifyAll();
        return t;
    }

    public static void main(String[] args) throws InterruptedException {
        TokenCounter counter = new TokenCounter();
        int[] collected = new int[2];
        Thread cook = new Thread(() -> {
            try { for (int t = 1; t <= 20; t++) counter.put(t); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread[] students = new Thread[2];
        for (int s = 0; s < 2; s++) {
            final int id = s;
            students[s] = new Thread(() -> {
                try { for (int k = 0; k < 10; k++) collected[id] += counter.take(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        cook.start(); for (Thread t : students) t.start();
        cook.join(); for (Thread t : students) t.join();
        System.out.println("Sum of tokens collected: " + (collected[0] + collected[1])
            + " (expected 210)");
        System.out.println("Counter size stayed between " + counter.minSeen + " and " + counter.maxSeen);
    }
}

class IfCounter {                                     // WHILE changed to IF
    final int[] buffer = new int[5];
    int count = 0, in = 0, out = 0, minSeen = 0;
    synchronized void put(int t) throws InterruptedException {
        if (count == 5) wait();
        buffer[in] = t; in = (in + 1) % 5; count++; notifyAll();
    }
    synchronized int take() throws InterruptedException {
        if (count == 0) wait();
        int t = buffer[out]; out = (out + 1) % 5; count--;
        if (count < minSeen) minSeen = count;
        notifyAll(); return t;
    }
}

class Account {
    final int id; int bal;
    Account(int id, int bal) { this.id = id; this.bal = bal; }
}

class Main {
    static Thread daemon(Runnable r) { Thread t = new Thread(r); t.setDaemon(true); return t; }
    static void pause() { try { Thread.sleep(100); } catch (InterruptedException e) { } }

    static void transfer(Account from, Account to, int amt, boolean ordered) {
        Account first = from, second = to;
        if (ordered && from.id > to.id) { first = to; second = from; }   // lock the lower id first
        synchronized (first) {
            pause();
            synchronized (second) { from.bal -= amt; to.bal += amt; }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("--- Part B: producer-consumer (while) ---");
        TokenCounter.main(args);

        System.out.println("--- Part B: same program with IF, 20 trials ---");
        int wrong = 0, hung = 0;
        for (int trial = 0; trial < 20; trial++) {
            IfCounter c = new IfCounter();
            int[] got = new int[2];
            Thread cook = daemon(() -> {
                try { for (int t = 1; t <= 20; t++) c.put(t); } catch (InterruptedException e) { }
            });
            Thread[] st = new Thread[2];
            for (int s = 0; s < 2; s++) {
                final int id = s;
                st[s] = daemon(() -> {
                    try { for (int k = 0; k < 10; k++) got[id] += c.take(); } catch (InterruptedException e) { }
                });
            }
            cook.start(); st[0].start(); st[1].start();
            cook.join(2000); st[0].join(2000); st[1].join(2000);
            if (cook.isAlive() || st[0].isAlive() || st[1].isAlive()) hung++;
            else if (got[0] + got[1] != 210 || c.minSeen < 0) wrong++;
        }
        System.out.println("IF version: wrong results = " + wrong + ", hung = " + hung + " (out of 20)");

        System.out.println("--- Part A: deadlock ---");
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        Thread t1 = daemon(() -> transfer(a, b, 100, false));
        Thread t2 = daemon(() -> transfer(b, a, 50, false));
        t1.start(); t2.start(); t1.join(1500); t2.join(1500);
        System.out.println("Unordered: t1 alive = " + t1.isAlive() + ", t2 alive = " + t2.isAlive()
            + "  (true/true = DEADLOCK)");

        Account c1 = new Account(1, 1000), c2 = new Account(2, 1000);
        Thread t3 = daemon(() -> transfer(c1, c2, 100, true));
        Thread t4 = daemon(() -> transfer(c2, c1, 50, true));
        t3.start(); t4.start(); t3.join(1500); t4.join(1500);
        System.out.println("Ordered: t3 alive = " + t3.isAlive() + ", t4 alive = " + t4.isAlive()
            + ", balances = " + c1.bal + " and " + c2.bal + " (expected 950 and 1050)");
    }
}
