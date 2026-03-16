import com.simpleblog.common.constants.RedisKeys;
import com.simpleblog.common.utils.RedisKeyBuilder;
import com.simpleblog.common.utils.RedisKeyQuickBuilder;
import org.junit.jupiter.api.Test;

//@AllArgsConstructor
//@SpringBootTest
public class TestRedisKeyBuilder {
//    @Autowired
//    private IRedisKeyBuilder redisKeyBuilder;
    @Test
    public void test() {
        var s = RedisKeyQuickBuilder.quickBuild(RedisKeyQuickBuilder.spaces.docSystem.node.id, "123");
        System.out.println(s);
    }

    @Test
    public void test2() {
//        System.out.println((RedisKeyBuilder.key(RedisKeys.DOC_NODE, "123")));
//        System.out.println(RedisKeyBuilder.key(RedisKeys.DOC_NODE_NEXT_SORTKEY, "6_1"));
        System.out.println(Integer.valueOf(null));
    }
}
