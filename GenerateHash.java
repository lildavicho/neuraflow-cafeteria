import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "Admin123!";
        String hash = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("Hash length: " + hash.length());
        
        // Verify
        boolean matches = encoder.matches(password, hash);
        System.out.println("Verification: " + matches);
        
        // Test with existing hash
        String existingHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        boolean matchesExisting = encoder.matches(password, existingHash);
        System.out.println("Matches existing hash: " + matchesExisting);
    }
}
