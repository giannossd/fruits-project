import java.io.*;
import java.net.*;
import java.security.MessageDigest;

public class DummyWorker {

    // Το κοινό μυστικό που μοιράζεται το παιχνίδι με την SRG
    private static final String SECRET_KEY = "mySecretKey123";

    public static void main(String[] args) {
        String serverAddress = "localhost"; // Η SRG τρέχει στον ίδιο υπολογιστή
        int serverPort = 9000;              // Η πόρτα (port) που ακούει η SRG (βάλε όποια θες, π.χ. 9000)

        // Θα ζητήσουμε 5 αριθμούς συνεχόμενα για να τεστάρουμε τον buffer (Producer-Consumer)
        for (int i = 1; i <= 5; i++) {
            System.out.println("--- Αίτημα " + i + " ---");
            requestRandomNumber(serverAddress, serverPort);
            
            try {
                Thread.sleep(1000); // Περιμένουμε 1 δευτερόλεπτο ανάμεσα στα αιτήματα
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void requestRandomNumber(String ip, int port) {
        // Χρησιμοποιούμε try-with-resources για να κλείσει το Socket αυτόματα στο τέλος
        try (Socket socket = new Socket(ip, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 1. Στέλνουμε το αίτημα στην SRG
            out.println("GET_NUMBER");
            System.out.println("Εστάλη αίτημα στην SRG...");

            // 2. Διαβάζουμε την απάντηση της SRG (μορφή: "Αριθμός,Hash")
            String response = in.readLine();
            System.out.println("Έλαβα απάντηση: " + response);

            if (response != null && response.contains(",")) {
                // 3. Χωρίζουμε την απάντηση στο κόμμα (,)
                String[] parts = response.split(",");
                String receivedNumberStr = parts[0];
                String receivedHash = parts[1];

                // 4. Επαληθεύουμε το Hash στον Worker
                String myCalculatedHash = calculateSHA256(receivedNumberStr + SECRET_KEY);

                System.out.println("Τυχαίος αριθμός: " + receivedNumberStr);
                System.out.println("Hash που έλαβα  : " + receivedHash);
                System.out.println("Hash που βρήκα  : " + myCalculatedHash);

                // 5. Ελέγχουμε αν τα δύο hashes είναι ίδια
                if (receivedHash.equals(myCalculatedHash)) {
                    System.out.println("✅ ΑΠΟΤΕΛΕΣΜΑ: Η επικοινωνία είναι ΑΣΦΑΛΗΣ! Τα Hashes ταιριάζουν.");
                } else {
                    System.out.println("❌ ΑΠΟΤΕΛΕΣΜΑ: ΠΡΟΒΛΗΜΑ ΑΣΦΑΛΕΙΑΣ! Τα Hashes ΔΕΝ ταιριάζουν.");
                }
            } else {
                System.out.println("Λάθος μορφή απάντησης από τον server.");
            }

        } catch (IOException e) {
            System.out.println("Δεν μπόρεσα να συνδεθώ στην SRG. Μήπως δεν την έχεις ξεκινήσει;");
            // e.printStackTrace(); // Το κάνουμε comment για να μην πετάει τεράστιο σφάλμα αν η SRG είναι κλειστή
        }
    }

    // Βοηθητική μέθοδος για τον υπολογισμό του SHA-256
    private static String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes("UTF-8"));
            
            // Μετατροπή των bytes σε δεκαεξαδική μορφή (String) για να μπορούμε να το διαβάσουμε
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (int i = 0; i < encodedhash.length; i++) {
                String hex = Integer.toHexString(0xff & encodedhash[i]);
                if(hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}