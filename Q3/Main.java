import java.io.*;

class Tracked implements AutoCloseable {
    static int open = 0;
    private final BufferedReader br;
    Tracked(String name) throws IOException {
        br = new BufferedReader(new FileReader(name));
        open++;
    }
    int sum() throws IOException {
        int s = 0; String line;
        while ((line = br.readLine()) != null) s += Integer.parseInt(line.trim());
        return s;
    }
    public void close() throws IOException { br.close(); open--; }
}

class Res implements AutoCloseable {
    final String n;
    Res(String n) { this.n = n; System.out.println("opening " + n); }
    public void close() throws Exception {
        System.out.println("closing " + n);
        throw new Exception("close failed " + n);
    }
}

class Main {
    static void log(String f, Exception e) { System.out.println("  LOG: failed on " + f + " -> " + e); }

    static int buggy(String[] files) {            // close() inside try: skipped on exception
        int total = 0;
        for (String f : files) {
            try {
                Tracked t = new Tracked(f);
                total += t.sum();
                t.close();
            } catch (IOException | NumberFormatException e) { log(f, e); }
        }
        return total;
    }

    static int withFinally(String[] files) {      // manual close in finally
        int total = 0;
        for (String f : files) {
            Tracked t = null;
            try {
                t = new Tracked(f);
                total += t.sum();
            } catch (IOException | NumberFormatException e) { log(f, e); }
            finally {
                if (t != null) { try { t.close(); } catch (IOException e) { } }
            }
        }
        return total;
    }

    static int withTwr(String[] files) {          // try-with-resources
        int total = 0;
        for (String f : files) {
            try (Tracked t = new Tracked(f)) {
                total += t.sum();
            } catch (IOException | NumberFormatException e) { log(f, e); }
        }
        return total;
    }

    static void write(String name, String text) throws IOException {
        try (FileWriter w = new FileWriter(name)) { w.write(text); }
    }

    public static void main(String[] args) throws Exception {
        write("a.txt", "10\n20\n");
        write("b.txt", "5\n");
        write("bad1.txt", "x\n");
        write("bad2.txt", "7\nabc\n");
        String[] files = {"a.txt", "bad1.txt", "bad2.txt", "b.txt", "missing.txt"};

        System.out.println("--- 1. Manual close inside try (buggy) ---");
        Tracked.open = 0;
        int r1 = buggy(files);
        System.out.println("Total = " + r1 + ", files left open = " + Tracked.open);

        System.out.println("--- 2. Manual close in finally ---");
        Tracked.open = 0;
        int r2 = withFinally(files);
        System.out.println("Total = " + r2 + ", files left open = " + Tracked.open);

        System.out.println("--- 3. try-with-resources ---");
        Tracked.open = 0;
        int r3 = withTwr(files);
        System.out.println("Total = " + r3 + ", files left open = " + Tracked.open);

        System.out.println("--- 4. Suppressed exceptions and closing order ---");
        try (Res a = new Res("A"); Res b = new Res("B")) {
            throw new RuntimeException("body failed");
        } catch (Exception e) {
            System.out.println("Primary: " + e.getMessage());
            for (Throwable s : e.getSuppressed()) System.out.println("Suppressed: " + s.getMessage());
        }
    }
                  }
