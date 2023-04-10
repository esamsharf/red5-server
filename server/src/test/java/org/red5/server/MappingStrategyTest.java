import org.junit.jupiter.api.Test;
import org.red5.server.MappingStrategy;
import org.red5.server.api.IMappingStrategy;
import static org.junit.jupiter.api.Assertions.*;

public class MappingStrategyTest {
    @Test
    public void testMapResourcePrefixWithRoot(){
        MappingStrategy strategy = new MappingStrategy();
        String result = strategy.mapResourcePrefix("/");
        assertEquals("default/", result);
    }
    
    @Test
    public void testMapResourcePrefixWithPath(){
        MappingStrategy strategy = new MappingStrategy();
        String result = strategy.mapResourcePrefix("/test");
        assertEquals("/test/", result);
    }
}
