import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Created by jakob on 03/08/2016.
 */
@Path("hello")
public class Hello {
    @GET
    public String hello() {
        return "Hello world";
    }
}
