package org.commonjava;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.specification.ProxySpecification;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class HttproxConfigTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("Hello RESTEasy"));
    }

    @Test
    public void testProxyEndpoint() {
        given().proxy(ProxySpecification.port(8082))
            .when().options("/hello")
            .then()
                .statusCode(200)
                .header("Allow", "HEAD,GET,OPTIONS");
    }

}