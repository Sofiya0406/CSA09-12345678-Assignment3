import java.io.IOException;

class ExamException extends Exception {
    ExamException(String m) { super(m); }
    ExamException(String m, Throwable c) { super(m, c); }
}
class ExamClosedException extends ExamException {
    ExamClosedException(String m) { super(m); }
}
class FileTooLargeException extends ExamException {
    FileTooLargeException(String m) { super(m); }
}
class SubmissionStorageException extends ExamException {
    SubmissionStorageException(String m, Throwable c) { super(m, c); }
}
class NotRegisteredException extends RuntimeException {   // unchecked
    NotRegisteredException(String m) { super(m); }
}

class Storage {
    void save(String fileName, long sizeKb) throws SubmissionStorageException {
        try {
            if (sizeKb > 500) throw new IOException("Disk quota exceeded");
            System.out.println("Saved " + fileName);
        } catch (IOException e) {
            throw new SubmissionStorageException("Could not store " + fileName, e);  // TODO 1
        }
    }
}

class Service {
    static void submit(String student, boolean registered, boolean open,
                       String file, long kb) throws ExamException {
        if (!open) throw new ExamClosedException("Exam window is closed");
        if (!registered) throw new NotRegisteredException("Student " + student + " is not registered");
        if (kb > 1000) throw new FileTooLargeException("File too large: " + kb + " KB (limit 1000 KB)");
        new Storage().save(file, kb);
    }
}

class Controller {
    static void submit(String test, String st, boolean reg, boolean open, String f, long kb) {
        System.out.println("Test " + test);
        try {
            Service.submit(st, reg, open, f, kb);
            System.out.println("  Student sees: Submission accepted");
        } catch (ExamClosedException | FileTooLargeException e) {
            System.out.println("  Student sees: " + e.getMessage());
        } catch (ExamException e) {
            Throwable c = e.getCause();
            System.out.println("  Student sees: " + e.getMessage() + " (please retry)");
            System.out.println("  Logged cause: " + (c == null ? "NONE (lost!)"
                : c.getClass().getSimpleName() + ": " + c.getMessage()));
        } catch (NotRegisteredException e) {
            System.out.println("  Student sees: " + e.getMessage());
        }
    }
}

class StorageDemo {
    public static void main(String[] args) {
        Storage storage = new Storage();
        try {
            storage.save("answer01.pdf", 120);
            storage.save("answer02.pdf", 800);
        } catch (ExamException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Cause: " + e.getCause().getMessage());
        }
    }
}

class Main {
    public static void main(String[] args) {
        System.out.println("--- Complete the Code (StorageDemo) ---");
        StorageDemo.main(args);

        System.out.println("--- Layered scenarios ---");
        Controller.submit("1 valid", "Asha", true, true, "answer01.pdf", 120);
        Controller.submit("2 window closed", "Asha", true, false, "answer01.pdf", 120);
        Controller.submit("3 not registered", "Ravi", false, true, "answer01.pdf", 120);
        Controller.submit("4 file too large", "Asha", true, true, "big.pdf", 2000);
        Controller.submit("5 storage failure", "Asha", true, true, "answer02.pdf", 800);

        System.out.println("--- Chained stack trace ---");
        try { new Storage().save("answer02.pdf", 800); }
        catch (SubmissionStorageException e) { e.printStackTrace(System.out); }

        System.out.println("--- Re-thrown WITHOUT cause ---");
        try {
            try { new Storage().save("answer02.pdf", 800); }
            catch (SubmissionStorageException e) { throw new ExamException("Upload failed"); }
        } catch (ExamException e) {
            System.out.println("Message: " + e.getMessage() + " | cause = " + e.getCause());
        }
    }
                                                       }
