package com.amalitech.qa;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.*;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class InsightFlowApiTest {
    private String authToken;

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "http://localhost:8080";
        authToken = given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"admin@amalitech.com\",\"password\":\"password123\"}")
        .when().post("/api/auth/login").then().extract().path("token");
    }

    @Test
    public void testGetDataSources() {
        given().header("Authorization", "Bearer " + authToken)
        .when().get("/api/datasources")
        .then().statusCode(200);
    }

    @Test
    public void testCreateDataSource() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + authToken)
            .body("{\"name\":\"Test CSV\",\"type\":\"CSV\",\"description\":\"Test data source\"}")
        .when().post("/api/datasources")
        .then().statusCode(200).body("name", equalTo("Test CSV"));
    }

    @Test
    public void testGetPipelines() {
        given().header("Authorization", "Bearer " + authToken)
        .when().get("/api/pipelines")
        .then().statusCode(200);
    }

    // TODO: testCreatePipeline
    // TODO: testRunPipeline
    // TODO: testUnauthorizedAccess
}
