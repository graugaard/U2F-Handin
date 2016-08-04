/**
 * Created by jakob on 03/08/2016.
 */

//var domain = "https://localhost:8443";
var domain = "https://graugaard.bobach:8443";
var service = '/U2F';
var registerRequest = {};

function startRegistration() {
    sendRequest("GET", domain + service + "/rest/server/start_registration?user=" + getUser(),
    null,
    function (response) {

        registerRequest = JSON.parse(response);
        console.log(registerRequest);
        document.getElementById("hello").innerHTML =
            "Please use your key";
        window.u2f.register(domain, registerRequest.registerRequests,[]
        , function(data) {
                console.log(data);
                finishRegistration(data);
            });
    });
}

function finishRegistration(data) {
    sendRequest("POST", domain + service + "/rest/server/finish_registration",
        "tokenResponse=" + JSON.stringify(data) +"&"+
        "user=" + getUser(),
    function (response) {
        if (response === "") {
            document.getElementById("hello").innerHTML = "UNAUTHORIZED ACCESS!"
        }
        document.getElementById("hello").innerHTML =
            response;
    })
}

function startAuthentication() {
    document.getElementById("hello").innerHTML = "Please use your key";
    sendRequest("GET", domain + service +"/rest/server/start_authentication?user="+getUser(),
        null,
        function (response) {
            if (response === "") {
                document.getElementById("hello").innerHTML = "User has not registered device";
            } else {
                var authRequest = JSON.parse(response);
                console.log(authRequest);
                console.log(authRequest.authenticateRequests[0]);

                var req = authRequest.authenticateRequests[0];

                u2f.sign(domain, req.challenge, [req],
                    function (data) {
                        console.log(data);
                        endAuthentication(data);
                    });
            }
        });
}

function endAuthentication(data) {
    sendRequest("POST", domain + service + "/rest/server/end_authentication",
        "user=" + getUser() +
        "&tokenResponse=" + JSON.stringify(data),
        function (response) {
            if (response==="") {
                document.getElementById("hello").innerHTML = "Did not recognize device"
            } else {
                document.getElementById("hello").innerHTML = response;
            }
        })
}

function getUser() {
    var user = "" + document.getElementById("user").value;
    console.log(user);
    return user;
}

// From the dWebTek course
function sendRequest(httpMethod, url, body, responseHandler) {

    var xhttp = new XMLHttpRequest();
    xhttp.open(httpMethod, url, true);
    if (httpMethod == "POST") {
        xhttp.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
    }
    xhttp.onreadystatechange = function () {
        if (xhttp.readyState == XMLHttpRequest.DONE && xhttp.status == 200) {
            console.log("Succes: Request went through");
            responseHandler(xhttp.responseText);
        } else if (xhttp.readyState == XMLHttpRequest.DONE) {
            console.log("Error: " + xhttp.responseText);
        }
    };
    xhttp.send(body);
}