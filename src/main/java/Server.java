
import com.yubico.u2f.U2F;
import com.yubico.u2f.data.DeviceRegistration;
import com.yubico.u2f.data.messages.*;
import com.yubico.u2f.exceptions.DeviceCompromisedException;
import com.yubico.u2f.exceptions.NoEligibleDevicesException;

import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;


/**
 * Created by jakob on 03/08/2016.
 */

@Path("server")
public class Server {

    //public static final String APP_ID = "https://localhost:8443";
    public static final String APP_ID = "https://graugaard.bobach.eu:8443";

    private final U2F u2f = new U2F();
    private final Map<String, String> requestStorage = new HashMap<String, String>();

    @Context
    ServletContext context;


    @GET
    @Path("start_registration")
    public Response startRegistration(@QueryParam("user") String user) throws ExecutionException {
        RegisterRequestData registerRequestData = u2f.startRegistration(APP_ID, getRegistrations(user));
        addRequst(registerRequestData.getRequestId(), registerRequestData.toJson());
        return buildResponse(Response.Status.OK, registerRequestData.toJson());
    }

    @Context
    private void setContext(ServletContext context) {
        this.context = context;
    }

    private ServletContext getContext() {
        return context;
    }

    @POST
    @Path("finish_registration")
    public Response finishRegistration(@FormParam("tokenResponse") String response,
                                     @FormParam("user") String user)
            throws CertificateException, NoSuchFieldException {
        RegisterResponse registerResponse = RegisterResponse.fromJson(response);

        // lookup
        RegisterRequestData registerData = RegisterRequestData.fromJson(
                removeRequest(registerResponse.getRequestId()));
        DeviceRegistration registration = u2f.finishRegistration(registerData, registerResponse);

        //Attestation attestation = metadataService.getAttestation(registration.getAttestationCertificate());

        addRegistration(user, registration);
        StringBuilder buf = new StringBuilder();
        buf.append("<p>Successfully registered device:</p>");

        return buildResponse(Response.Status.OK, buf.toString());
    }

    @GET
    @Path("start_authentication")
    public Response startAuthentication(@QueryParam("user") String user)
            throws ExecutionException, NoEligibleDevicesException
    {
        String s = "";
        try {
            AuthenticateRequestData authenticateRequestData = u2f.startAuthentication(APP_ID, getRegistrations(user));
            addRequst(authenticateRequestData.getRequestId(), authenticateRequestData.toJson());
            s = authenticateRequestData.toJson();
        } catch (NoEligibleDevicesException e) {
            s = "";
        }

        return buildResponse(Response.Status.OK, s);
    }

    @POST
    @Path("end_authentication")
    public Response endAuthentication (@FormParam("user") String user,
                                     @FormParam("tokenResponse") String response) throws ExecutionException {
        DeviceRegistration registration = null;
        String s = "";
        try {
        AuthenticateResponse authenticateResponse = AuthenticateResponse.fromJson(response);
        AuthenticateRequestData authenticateRequest =
                AuthenticateRequestData.fromJson(removeRequest(authenticateResponse.getRequestId()));
            registration = u2f.finishAuthentication(authenticateRequest, authenticateResponse, getRegistrations(user));
        } catch (DeviceCompromisedException e) {
            return buildResponse(Response.Status.OK,
                    "<p>Device possibly compromised and therefore blocked: "
                            + e.getMessage() + "</p>");
        } catch (Exception e){
            return buildResponse(Response.Status.OK, "");
        }
        finally {
            if (registration != null) {
                getUserStorage().get(user).put(registration.getKeyHandle(), registration.toJson());
            }
        }
        return buildResponse(Response.Status.OK,"<p>Successfully authenticated!<p>");
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

    private void addRequst(String requestID, String requestData)
    {
        Map<String, String> m = (Map<String,String>) getContext().getAttribute("requestStorage");
        if (m == null) {
            m = new HashMap<String, String>();
            getContext().setAttribute("requestStorage", m);
        }
        m.put(requestID, requestData);
    }

    private Map<String, String> getRequestStorage()
    {
        return (Map<String,String>) getContext().getAttribute("requestStorage");
    }

    private Response buildResponse(Response.Status status, Object e)
    {
        return Response.status(status)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
                .header("Access-Control-Max-Age", "1209600")
                .entity(e).build();
    }
    private String removeRequest(String requestID)
    {
        return getRequestStorage().remove(requestID);
    }
}
