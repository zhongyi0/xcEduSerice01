import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class testt {
    @Test
    public void fristTest(){
        List<String> strings = Arrays.asList("a", "b", "c");
        Arrays.asList("a", "b", "c").forEach(e -> System.out.println(new Date().toString()+"...."+e));
    }
}
