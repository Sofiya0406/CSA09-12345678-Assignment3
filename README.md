# CSA09 Assignment 3 - Exceptions and Multithreading

- Name: S. Sofiya
- Register Number: 192521248
- Java Version: <fill>

## How to run
Each Q folder: javac Main.java && java Main
(Q2 also has the StorageDemo class, Q5 has the SeatBooking class)

## Results
Q1 ATM exceptions: all 6 tests matched the expected catch paths.
Q2 Exception chaining: cause preserved, matched expected output.
Q3 try-with-resources: files left open 2 vs 0.
Q4 Threads: total scanned 100, order varies per run.
Q5 Race condition: unsafe version double-booked, synchronized versions sold exactly 10.
Q6 Deadlock and producer-consumer: sum 210, ordered locking removed the deadlock.
Q7 ExecutorService: counts matched the hand-checked set.
