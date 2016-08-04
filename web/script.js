/**
 * Created by jakob on 03/08/2016.
 */

var domain = "http://localhost:8080";
var register = {};

window.onload = function() {
    sendRequest("GET", domain + "/rest/server/start_registration?user=hans",
    null,
    function (response) {
        register = JSON.parse(response);
        console.log(register);
        document.getElementById("hello").innerHTML =
            register.registerRequests[0].challenge;
        window.u2f.register(register.registerRequests[0]
        , []
        , function(data) {
               console.log(data);
            });
    });
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