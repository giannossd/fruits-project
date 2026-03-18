import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class DummyPlayerApp {

    // Τα στοιχεία του Master (όπως και στον Manager)
    private static final String MASTER_IP = "localhost";
    private static final int MASTER_PORT = 8000;
    
    // Το πορτοφόλι του παίκτη
    private static double balance = 0.0;
    private static String playerId = "Player_123"; // Ένα τυχαίο ID για τον παίκτη

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("=== 📱 Καλώς ήρθατε στο Casino App (Dummy Version) ===");

        while (running) {
            System.out.println("\n--- ΒΑΣΙΚΟ ΜΕΝΟΥ ---");
            System.out.println("Διαθέσιμο Υπόλοιπο: " + balance + " FUN Tokens");
            System.out.println("1. Αναζήτηση Παιχνιδιών (search)");
            System.out.println("2. Ποντάρισμα σε Παιχνίδι (play)");
            System.out.println("3. Προσθήκη Υπολοίπου (addBalance)");
            System.out.println("4. Έξοδος");
            System.out.print("Επιλογή: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    searchGames(scanner);
                    break;
                case "2":
                    playGame(scanner);
                    break;
                case "3":
                    addBalance(scanner);
                    break;
                case "4":
                    System.out.println("Αποσύνδεση... Καλή συνέχεια!");
                    running = false;
                    break;
                default:
                    System.out.println("Λάθος επιλογή. Προσπαθήστε ξανά.");
            }
        }
        scanner.close();
    }

    // --------------------------------------------------------
    // ΛΕΙΤΟΥΡΓΙΑ 1: Προσθήκη Υπολοίπου (addBalance)
    // --------------------------------------------------------
    private static void addBalance(Scanner scanner) {
        System.out.print("Πόσα FUN Tokens θέλετε να προσθέσετε; ");
        try {
            double amount = Double.parseDouble(scanner.nextLine());
            if (amount > 0) {
                balance += amount;
                System.out.println("✅ Το υπόλοιπό σας ανανεώθηκε! Νέο υπόλοιπο: " + balance);
            } else {
                System.out.println("❌ Παρακαλώ εισάγετε θετικό αριθμό.");
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Μη έγκυρο ποσό.");
        }
    }

    // --------------------------------------------------------
    // ΛΕΙΤΟΥΡΓΙΑ 2: Αναζήτηση με Φίλτρα (search)
    // --------------------------------------------------------
    private static void searchGames(Scanner scanner) {
        System.out.println("\n-- Φίλτρα Αναζήτησης --");
        System.out.print("Επίπεδο ρίσκου (low, medium, high) ή πατήστε Enter για όλα: ");
        String risk = scanner.nextLine();
        
        System.out.print("Κατηγορία πονταρίσματος ($, $$, $$$) ή πατήστε Enter για όλα: ");
        String betCategory = scanner.nextLine();

        // Δημιουργούμε ένα μήνυμα με τα φίλτρα για να το στείλουμε στον Master
        String filterMessage = "SEARCH_GAMES|" + playerId + "|Risk:" + risk + "|Category:" + betCategory;
        
        System.out.println("\nΑποστολή φίλτρων στον Master... (Ασύγχρονη αναζήτηση)");
        sendRequestToMaster(filterMessage);
    }

    // --------------------------------------------------------
    // ΛΕΙΤΟΥΡΓΙΑ 3: Ποντάρισμα (play)
    // --------------------------------------------------------
    private static void playGame(Scanner scanner) {
        if (balance <= 0) {
            System.out.println("❌ Δεν έχετε επαρκές υπόλοιπο. Παρακαλώ προσθέστε Tokens πρώτα.");
            return;
        }

        System.out.print("Δώστε το όνομα του παιχνιδιού που θέλετε να παίξετε (π.χ. SuperSlots): ");
        String gameName = scanner.nextLine();

        System.out.print("Πόσα Tokens θέλετε να ποντάρετε; ");
        try {
            double betAmount = Double.parseDouble(scanner.nextLine());
            
            if (betAmount > balance) {
                System.out.println("❌ Δεν έχετε τόσα χρήματα! Διαθέσιμο υπόλοιπο: " + balance);
                return;
            }

            // Δεσμεύουμε τα χρήματα προσωρινά
            balance -= betAmount;
            
            String playMessage = "PLAY_GAME|" + playerId + "|" + gameName + "|" + betAmount;
            System.out.println("Αποστολή αιτήματος πονταρίσματος στον Master...");
            sendRequestToMaster(playMessage);

        } catch (NumberFormatException e) {
            System.out.println("❌ Μη έγκυρο ποσό.");
        }
    }

    // --------------------------------------------------------
    // ΕΠΙΚΟΙΝΩΝΙΑ ΜΕ ΤΟΝ MASTER
    // --------------------------------------------------------
    private static void sendRequestToMaster(String message) {
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            // Στέλνουμε το αίτημα
            out.println(message);
            
            // Περιμένουμε την απάντηση (αποτελέσματα αναζήτησης ή κέρδος/χασούρα)
            String response = in.readLine();
            System.out.println("📩 Απάντηση από Master: " + response);

        } catch (IOException e) {
            System.out.println("⚠️ [ΣΦΑΛΜΑ ΣΥΝΔΕΣΗΣ] Ο Master Server δεν είναι ανοιχτός αυτή τη στιγμή για να εξυπηρετήσει το αίτημα.");
        }
    }
}