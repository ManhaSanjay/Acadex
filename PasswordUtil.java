import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtil {

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    public static String hashPassword(String password) {

        try {

            byte[] salt =
                    new byte[SALT_LENGTH];

            SecureRandom random =
                    new SecureRandom();

            random.nextBytes(salt);

            PBEKeySpec spec =
                    new PBEKeySpec(
                            password.toCharArray(),
                            salt,
                            ITERATIONS,
                            KEY_LENGTH
                    );

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            byte[] hash =
                    factory.generateSecret(spec)
                           .getEncoded();

            spec.clearPassword();

            return Base64.getEncoder()
                         .encodeToString(salt)
                    + ":"
                    + Base64.getEncoder()
                             .encodeToString(hash);

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    public static boolean verifyPassword(
            String password,
            String storedPassword) {

        try {

            String[] parts =
                    storedPassword.split(":");

            if (parts.length != 2) {
                return false;
            }

            byte[] salt =
                    Base64.getDecoder()
                          .decode(parts[0]);

            byte[] storedHash =
                    Base64.getDecoder()
                          .decode(parts[1]);

            PBEKeySpec spec =
                    new PBEKeySpec(
                            password.toCharArray(),
                            salt,
                            ITERATIONS,
                            KEY_LENGTH
                    );

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            byte[] newHash =
                    factory.generateSecret(spec)
                           .getEncoded();

            spec.clearPassword();

            return MessageDigest.isEqual(
                    storedHash,
                    newHash
            );

        } catch (Exception e) {

            return false;
        }
    }
}