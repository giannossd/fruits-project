import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class ManagerApp {

    private static final String MASTER_IP = "localhost";
    private static final int MASTER_PORT = 8000; 

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("=== Καλώς ήρθατε στο Manager Console App ===");

        while (running) {
            System.out.println("\nΕπιλέξτε ενέργεια:");
            System.out.println("1. Προσθήκη Παιχνιδιού (Διάβασμα game.json)");
            System.out.println("2. Έξοδος");
            System.out.print("Επιλογή: ");
            
            String choice = scanner.nextLine();

            if (choice.equals("1")) {
                processAndSendGame();
            } else if (choice.equals("2")) {
                System.out.println("Κλείσιμο εφαρμογής...");
                running = false;
            } else {
                System.out.println("Λάθος επιλογή. Παρακαλώ επιλέξτε 1 ή 2.");
            }
        }
        
        scanner.close();
    }

    private static void processAndSendGame() {
        String filePath = "game.json";

        try {
            // 1. Διαβάζουμε όλο το αρχείο ως ένα απλό κείμενο (String)
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            
            String gameName = "";
            double minBet = 0.0;

            // 2. Χειροκίνητη εξαγωγή δεδομένων από το JSON κείμενο
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.contains("\"GameName\"")) {
                    // Παίρνουμε αυτό που είναι μετά την άνω κάτω τελεία και καθαρίζουμε τα εισαγωγικά και τα κόμματα
                    gameName = line.split(":")[1].replace("\"", "").replace(",", "").trim();
                } else if (line.contains("\"MinBet\"")) {
                    // Ομοίως για το ελάχιστο ποντάρισμα, αλλά το μετατρέπουμε σε αριθμό (double)
                    String betString = line.split(":")[1].replace(",", "").trim();
                    minBet = Double.parseDouble(betString);
                }
            }

            // 3. Υπολογισμός κατηγορίας πονταρίσματος
            String betCategory = "";
            if (minBet == 0.1) {
                betCategory = "$";
            } else if (minBet == 1.0) {
                betCategory = "$$";
            } else if (minBet >= 5.0) {
                betCategory = "$$$";
            } else {
                betCategory = "Άγνωστη";
            }

            System.out.println("\n[ΕΠΙΤΥΧΙΑ] Το παιχνίδι '" + gameName + "' διαβάστηκε! Κατηγορία: " + betCategory);
            
            // 4. Φτιάχνουμε ένα νέο απλό String που περιέχει τα δεδομένα για να τα στείλουμε
            String dataToSend = "GameName:" + gameName + ",MinBet:" + minBet + ",Category:" + betCategory;
            
            // 5. Αποστολή στον Master μέσω TCP Socket
            sendToMaster(dataToSend);

        } catch (Exception e) {
            System.out.println("\n[ΣΦΑΛΜΑ] Δεν βρέθηκε το αρχείο game.json ή υπήρξε λάθος στην ανάγνωση.");
            e.printStackTrace();
        }
    }

    private static void sendToMaster(String data) {
        System.out.println("Προσπάθεια σύνδεσης με τον Master...");
        
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            out.println("ADD_GAME");
            out.println(data);
            
            System.out.println("[ΕΠΙΤΥΧΙΑ] Τα δεδομένα στάλθηκαν στον Master!");

        } catch (IOException e) {
            System.out.println("[ΑΠΟΤΥΧΙΑ] Ο Master δεν είναι ανοιχτός αυτή τη στιγμή για να λάβει τα δεδομένα (Error: " + e.getMessage() + ").");
        }
    }
}