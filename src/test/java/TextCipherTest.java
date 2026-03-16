import com.simpleblog.common.utils.TextCipher;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class TextCipherTest {
    @SneakyThrows
    @Test
    public void test() {
        String text = "今天晚上一起吃饭";
        String key = "secret123";
        String cipher = TextCipher.encrypt(text,key);
        System.out.println(cipher);
        String plain = TextCipher.decrypt(cipher,key);
        System.out.println(plain);
    }
}
