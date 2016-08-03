import com.google.common.cache.*;
import com.yubico.u2f.U2F;
import com.yubico.u2f.data.DeviceRegistration;
import com.yubico.u2f.data.messages.*;

import java.util.*;

import javax.ws.rs.*;


/**
 * Created by jakob on 03/08/2016.
 */

@Path("server")
public class Server {

    public static final String APP_ID = "https://localhost:8080";

    private U2F u2f = new U2F();
    private final Map<String, String> requestStorage = new HashMap<String, String>();

    private final LoadingCache<String, Map<String, String>> userStorage =
            CacheBuilder.newBuilder().build(new CacheLoader<String, Map<String, String>>() {
        @Override
        public Map<String, String> load(String key) throws Exception {
            return new HashMap<String, String>();
        }
    });

    @GET
    @Path("start_registration")
    public String startAutheticate(@QueryParam("user") String user){
        RegisterRequestData registerRequestData = u2f.startRegistration(APP_ID, getRegistrations(user));
        requestStorage.put(registerRequestData.getRequestId(), registerRequestData.toJson());

        return "";
    }

    private Iterable<DeviceRegistration> getRegistrations(String username) {
        List<DeviceRegistration> registrations = new ArrayList<DeviceRegistration>();
        for (String serialized : userStorage.getUnchecked(username).values()) {
            registrations.add(DeviceRegistration.fromJson(serialized));
        }
        return registrations;
    }
}
