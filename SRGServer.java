import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.Random;

public class SRGServer {

    private static final int PORT = 9000; // Η πόρτα στην οποία ακούει (ίδια με του DummyWorker)
    private static final String SECRET_KEY = "mySecretKey123"; // Το ίδιο μυστικό με τον Worker

    // Το "Κουτί" (Buffer) μας και το μέγιστο όριό του
    private static final LinkedList<Integer> buffer = new LinkedList<>();
    private static final int MAX_BUFFER_SIZE = 100;

    public static void main(String[] args) {
        System.out.println("🚀 Η Γεννήτρια (SRG Server) ξεκινάει...");

        // 1. Ξεκινάμε τον Παραγωγό (Producer) σε ένα δικό του Νήμα
        Thread producer = new Thread(new NumberProducer());
        producer.start();

        // 2. Ξεκινάμε τον TCP Server
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🎧 Η SRG ακούει στην πόρτα " + PORT + " και περιμένει Workers...");

            // Ατέρμονος βρόχος για να δέχεται συνεχώς νέους Workers
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Εδώ το πρόγραμμα σταματάει και περιμένει
                System.out.println("✅ Νέος Worker μόλις συνδέθηκε!");
                
                // Φτιάχνουμε ένα νέο Νήμα (Consumer) αποκλειστικά για αυτόν τον Worker
                Thread workerHandler = new Thread(new WorkerHandler(clientSocket));
                workerHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // ΚΛΑΣΗ 1: Ο Παραγωγός (Producer)
    // Παράγει συνεχώς αριθμούς και τους βάζει στο κουτί.
    // =========================================================================
    static class NumberProducer implements Runnable {
        private final Random random = new Random();

        @Override
        public void run() {
            while (true) {
                // "Κλειδώνουμε" το κουτί για να μην το πειράξει κανείς άλλος ταυτόχρονα
                synchronized (buffer) {
                    // Αν το κουτί γέμισε (έχει 100 αριθμούς), ο Παραγωγός πάει για "ύπνο" (wait)
                    while (buffer.size() >= MAX_BUFFER_SIZE) {
                        try {
                            buffer.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    // Αν υπάρχει χώρος, φτιάχνει έναν θετικό τυχαίο αριθμό
                    int newNumber = Math.abs(random.nextInt(1000000));
                    buffer.add(newNumber);

                    // "Ξυπνάει" τους Workers (που ίσως κοιμούνται επειδή το κουτί ήταν άδειο)
                    buffer.notifyAll();
                }
            }
        }
    }

    // =========================================================================
    // ΚΛΑΣΗ 2: Ο Καταναλωτής (Consumer / Worker Handler)
    // Αναλαμβάνει να μιλάει με τον Worker που συνδέθηκε.
    // =========================================================================
    static class WorkerHandler implements Runnable {
        private Socket socket;

        public WorkerHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String request;
                // Διαβάζουμε τι μας ζητάει ο Worker
                while ((request = in.readLine()) != null) {
                    if (request.equals("GET_NUMBER")) {
                        int numberToSend;

                        // "Κλειδώνουμε" το κουτί για να πάρουμε έναν αριθμό
                        synchronized (buffer) {
                            // Αν το κουτί είναι εντελώς άδειο, ο Worker πάει για "ύπνο" (wait)
                            while (buffer.isEmpty()) {
                                buffer.wait();
                            }
                            
                            // Παίρνουμε τον πρώτο διαθέσιμο αριθμό από το κουτί
                            numberToSend = buffer.removeFirst();
                            
                            // Ξυπνάμε τον Παραγωγό (ώστε να βάλει νέο αριθμό τώρα που άδειασε μια θέση)
                            buffer.notifyAll();
                        }

                        // Υπολογίζουμε το Hash (Αριθμός + Μυστικό)
                        String hash = calculateSHA256(numberToSend + SECRET_KEY);

                        // Στέλνουμε την απάντηση (Μορφή: "Αριθμός,Hash")
                        out.println(numberToSend + "," + hash);
                        System.out.println("📤 Έστειλα τον αριθμό " + numberToSend + " στον Worker.");
                    }
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("❌ Ένας Worker αποσυνδέθηκε.");
            }
        }
    }

    // =========================================================================
    // ΒΟΗΘΗΤΙΚΗ: Υπολογισμός SHA-256 (Ίδια με του Worker)
    // =========================================================================
    private static String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes("UTF-8"));
            
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