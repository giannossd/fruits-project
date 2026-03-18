import java.io.*;
import java.net.*;

public class DummyMasterServer {

    private static final int PORT = 8000; // Η πόρτα που ψάχνουν ο Manager και ο Player

    public static void main(String[] args) {
        System.out.println("🚀 Ο (Dummy) Master Server ξεκίνησε!");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🎧 Ακούει στην πόρτα " + PORT + " και περιμένει συνδέσεις...\n");

            // Ατέρμονος βρόχος για να δέχεται συνεχώς αιτήματα
            while (true) {
                Socket clientSocket = serverSocket.accept();
                
                // Για κάθε πελάτη (Manager ή Player), ανοίγουμε ένα νέο Νήμα (Thread)
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Σφάλμα εκκίνησης του Master: " + e.getMessage());
        }
    }

    // Η μέθοδος που διαχειρίζεται το τι έστειλε ο Manager ή ο Player
    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Διαβάζουμε την πρώτη γραμμή (την εντολή)
            String request = in.readLine();
            if (request == null) return;

            System.out.println("📥 ΝΕΟ ΜΗΝΥΜΑ: " + request);

            // Ελέγχουμε τι μας ζήτησαν
            if (request.equals("ADD_GAME")) {
                // Ο Manager στέλνει και 2η γραμμή με τα δεδομένα του JSON
                String gameData = in.readLine();
                System.out.println("   --> ΔΕΔΟΜΕΝΑ ΠΑΙΧΝΙΔΙΟΥ: " + gameData);
                System.out.println("   --> [Ο Master αποθήκευσε το παιχνίδι (υποθετικά)]\n");

            } else if (request.startsWith("SEARCH_GAMES")) {
                // Ο Player ψάχνει παιχνίδια, οπότε του απαντάμε εμείς κάτι εικονικό
                System.out.println("   --> [Ο Master ψάχνει στους Workers (υποθετικά)]");
                out.println("Βρέθηκαν 2 παιχνίδια που ταιριάζουν: SuperSlots (Risk: low), MegaJackpot (Risk: high)");
                System.out.println("   --> Έστειλα απάντηση στον Παίκτη.\n");

            } else if (request.startsWith("PLAY_GAME")) {
                // Ο Player ποντάρει
                System.out.println("   --> [Ο Master προωθεί το ποντάρισμα στον Worker (υποθετικά)]");
                out.println("✅ Συγχαρητήρια! Κέρδισες +50 FUN Tokens από το ποντάρισμά σου!");
                System.out.println("   --> Έστειλα απάντηση στον Παίκτη.\n");
            }

        } catch (IOException e) {
            System.out.println("Σφάλμα επικοινωνίας με πελάτη.");
        }
    }
}