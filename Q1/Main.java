import java.io.*;

class InsufficientFundsException extends Exception {
    InsufficientFundsException(String m) { super(m); }
}

class Main {
    static double balance = 5000;

    static void readCard(String file) throws IOException {
        new FileReader(file).close();            // missing file -> FileNotFoundException
    }

    static void withdraw(String card, String amt)
            throws IOException, InsufficientFundsException {
        readCard(card);
        int a = Integer.parseInt(amt);           // letters -> NumberFormatException
        if (a <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (a > balance) throw new InsufficientFundsException("Requested " + a + " but balance is " + balance);
        balance -= a;
        System.out.println("  Dispensed Rs " + a + ", balance " + balance);
    }

    static void attempt(String test, String card, String amt) {
        System.out.println("Test " + test);
        try {
            withdraw(card, amt);
        } catch (NumberFormatException e) {                 // narrower, must come first
            System.out.println("  CATCH NumberFormatException: " + e.getMessage());
        } catch (IllegalArgumentException e) {              // its parent
            System.out.println("  CATCH IllegalArgumentException: " + e.getMessage());
        } catch (InsufficientFundsException | FileNotFoundException e) {   // multi-catch
            System.out.println("  CATCH " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (IOException e) {                           // parent of FileNotFoundException
            System.out.println("  CATCH IOException: " + e.getMessage());
        } finally {
            System.out.println("  FINALLY: card returned");
        }
    }

    public static void main(String[] args) throws IOException {
        new FileWriter("card.txt").close();                 // create a card file
        attempt("1 valid 1000", "card.txt", "1000");
        attempt("2 letters", "card.txt", "abc");
        attempt("3 negative", "card.txt", "-500");
        attempt("4 more than balance", "card.txt", "9000");
        attempt("5 card file missing", "missing.txt", "100");
        attempt("6 exact balance", "card.txt", "4000");

        System.out.println("Stack trace demo:");
        try { Integer.parseInt("xyz"); }
        catch (NumberFormatException e) { e.printStackTrace(System.out); }
    }
          }
