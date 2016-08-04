
import com.yubico.u2f.U2F;
import com.yubico.u2f.data.DeviceRegistration;
import com.yubico.u2f.data.messages.*;

import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;


/**
 * Created by jakob on 03/08/2016.
 */

@Path("server")
public class Server {

    public static final String APP_ID = "https://localhost:8080";

    private final U2F u2f = new U2F();
    private final Map<String, String> requestStorage = new HashMap<String, String>();

    @Context
    ServletContext context;


    @GET
    @Path("start_registration")
    public String startRegistration(@QueryParam("user") String user) throws ExecutionException
    {
        RegisterRequestData registerRequestData = u2f.startRegistration(APP_ID, getRegistrations(user));
        requestStorage.put(registerRequestData.getRequestId(), registerRequestData.toJson());
        return registerRequestData.toJson();

    }

    @Context
    private void setContext(ServletContext context)
    {
        this.context = context;
    }

    private ServletContext getContext()
    {
        return context;
    }

    @POST
    @Path("finish_registration")
    public String finishRegistration(@FormParam("tokenResponse") String response,
                                     @FormParam("user") String user)
            throws CertificateException, NoSuchFieldException
    {
        RegisterResponse registerResponse = RegisterResponse.fromJson(response);

        // lookup
        RegisterRequestData registerData = RegisterRequestData.fromJson(
                requestStorage.remove(registerResponse.getRequestId()));
        DeviceRegistration registration = u2f.finishRegistration(registerData, registerResponse);

        //Attestation attestation = metadataService.getAttestation(registration.getAttestationCertificate());

        addRegistration(user, registration);
        StringBuilder buf = new StringBuilder();
        buf.append("<p>Successfully registered device:</p>");
        /*if(!attestation.getVendorProperties().isEmpty()) {
            buf.append("<p>Vendor metadata</p><pre>");
            for(Map.Entry<String, String> entry : attestation.getVendorProperties().entrySet()) {
                buf.append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append("\n");
            }
            buf.append("</pre>");
        } else {
            buf.append("<p>No vendor metadata present!</p>");
        }
        if(!attestation.getDeviceProperties().isEmpty()) {
            buf.append("<p>Device metadata</p><pre>");
            for(Map.Entry<String, String> entry : attestation.getDeviceProperties().entrySet()) {
                buf.append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append("\n");
            }
            buf.append("</pre>");
        } else {
            buf.append("<p>No device metadata present!</p>");
        }
        if(!attestation.getTransports().isEmpty()) {
            buf.append("<p>Device transports: ")
                    .append(attestation.getTransports())
                    .append("</p>");
        } else {
            buf.append("<p>No device transports reported!</p>");
        }
        buf.append("<p>Registration data</p><pre>")
                .append(registration)
                .append("</pre>");*/

        return buf.toString();
    }


    // Gives us all devices registrested to the user.
    private Iterable<DeviceRegistration> getRegistrations(String username) throws ExecutionException {
        List<DeviceRegistration> registrations = null;
        try {
             registrations = new ArrayList<DeviceRegistration>();
        } catch (NullPointerException e) {
            System.out.println("Nullpointer exception?!");
        }
        Collection<String> c = getUserRegistrations(username);
        if (c == null) {
            return registrations;
        }
        for (String serialized : getUserStorage().get(username).values()) {
            registrations.add(DeviceRegistration.fromJson(serialized));
        }
        return registrations;
    }

    private Collection<String> getUserRegistrations(String username) throws ExecutionException {
        Map<String, String > m = getUserStorage().get(username);
        if (m == null) {
            m = new HashMap<String, String>();
            getUserStorage().put(username, m);
        }
        return m.values();
    }

    private Map<String,Map<String,String>> getUserStorage()
    {
        Map<String,Map<String,String>> m =
                (Map<String,Map<String,String>>)getContext().getAttribute("userStorage");
        if (m == null) {
            m = new HashMap<String, Map<String, String>>();
            getContext().setAttribute("userStorage", m);
        }
        return m;
    }

    // add a registreted device to user. The keyhandle binds the registration
    private void addRegistration(String user, DeviceRegistration registration)
    {
        getUserStorage().get(user).put(registration.getKeyHandle(), registration.toJson());
    }
}
