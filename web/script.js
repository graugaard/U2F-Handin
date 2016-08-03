/**
 * Created by jakob on 03/08/2016.
 */

var domain = "http://localhost:8080";

window.onload = function() {
    sendRequest("GET", domain + "/rest/hello",
    null,
    function (response) {
        document.getElementById("hello").innerHTML = response;
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