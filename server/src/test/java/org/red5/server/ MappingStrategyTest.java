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

    @Test
    public void testMapResourcePrefixWithNull(){
        MappingStrategy strategy = new MappingStrategy();
        assertThrows(IllegalArgumentException.class, () -> {
            strategy.mapResourcePrefix(null);
        });
    }

    @Test
    public void testMapServiceNameWithNull(){
        MappingStrategy strategy = new MappingStrategy();
        assertThrows(IllegalArgumentException.class, () -> {
            strategy.mapServiceName(null);
        });
    }

    @Test
    public void testMapServiceNameWithRoot(){
        MappingStrategy strategy = new MappingStrategy();
        String result = strategy.mapServiceName("/");
        assertEquals("default.service", result);
    }

    @Test
    public void testMapServiceNameWithPath(){
        MappingStrategy strategy = new MappingStrategy();
        String result = strategy.mapServiceName("/test");
        assertEquals("/test.service", result);
    }

    @Test
    public void testMapHandlerNameWithNull(){
        MappingStrategy strategy = new MappingStrategy();
        assertThrows(IllegalArgumentException.class, () -> {
            strategy.mapHandlerName(null);
        });
    }

    @Test
    public void testMapHandlerNameWithRoot(){
        MappingStrategy strategy = new MappingStrategy();
        String result = strategy.mapHandlerName("/");
        assertEquals("default.handler", result);
    }

    @Test
    public void testMapHandlerNameWithPath(){
        MappingStrategy strategy = new MappingStrategy();
        String result = strategy.mapHandlerName("/test");
        assertEquals("/test.handler", result);
    }

    @Test
    public void testSetDefaultAppWithNull(){
        MappingStrategy strategy = new MappingStrategy();
        assertThrows(IllegalArgumentException.class, () -> {
            strategy.setDefaultApp(null);
        });
    }

    @Test
    public void testSetDefaultAppWithEmptyString(){
        MappingStrategy strategy = new MappingStrategy();
        assertThrows(IllegalArgumentException.class, () -> {
            strategy.setDefaultApp("");
        });
    }

    
}
