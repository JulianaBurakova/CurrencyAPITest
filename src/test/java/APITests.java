import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertEquals;

public class APITests {
    private static Response response;

    @BeforeAll
    public static void setup(){
        given().baseUri(Consts.BASE_URL);
    }

    @Test
    public void testLiveEndpoint() {
        response = RestAssured.get(Consts.BASE_URL + "/live?apikey=" + Consts.API_KEY);

        response.then()
                .assertThat()
                .statusCode(200)
                .body("success", equalTo(true))
//                .body("terms", notNullValue())
//                .body("privacy", notNullValue())
                .body("timestamp", notNullValue())
//                .body("source", equalTo("USD"))
                .body("quotes.USDAED", notNullValue())
                .body("quotes.USDCAD", notNullValue())
                .body("quotes.USDINR", notNullValue())
                .body("quotes.USDEUR", notNullValue());
    }

    @Test
    public void testHistoricalEndpoint() {
        response = RestAssured.get(Consts.BASE_URL + "/historical?date=2018-01-01&apikey=" + Consts.API_KEY);
        response.then().statusCode(200);
        response.then().body("success", equalTo(true));
        response.then().body("source", equalTo("USD"));
        response.then().body("quotes", hasKey("USDCAD"));
        response.then().body("quotes", hasKey("USDEUR"));
//        response.then().body("quotes", hasKey("USDNIS"));
        response.then().body("quotes", hasKey("USDRUB"));
    }

    @Test
    public void testMissingAPIKey() {
        response = given()
                .when()
                .get(Consts.BASE_URL + "/live?apikey=")
                .then()
                .extract()
                .response();
        response.then().statusCode(401);
        response.then().body("message", equalTo("No API key found in request"));
    }

    private Map<String, Object> createParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("apikey", Consts.API_KEY);
        params.put("currencies", "NILNIL");
        return params;
    }

    @Test
    public void testInvalidCurrencyCode() {
        Response response = RestAssured.given()
                .params(createParams())
                .get(Consts.BASE_URL + "/live");

        response.then()
                .statusCode(200)
                .body("success", equalTo(false))
                .body("error.code", equalTo(202))
                .body("error.info", containsString("[Required format: currencies=EUR,USD,GBP,...]"));
    }

    @Test
    public void testLiveEndpointWithCurrenciesParameter() {
        response = RestAssured.get(Consts.BASE_URL + "/live?apikey=" + Consts.API_KEY + "&currencies=EUR,GBP,JPY");
        response.then().statusCode(200);
        response.then().body("quotes", hasKey("USDEUR"));
        response.then().body("quotes", hasKey("USDGBP"));
        response.then().body("quotes", hasKey("USDJPY"));
    }


    @Test
    public void testHistoricalEndpointWithCurrenciesParameter() {
        response = RestAssured.get(Consts.BASE_URL + "/historical?date=2018-01-01&apikey=" + Consts.API_KEY + "&currencies=EUR,GBP,JPY");
        response.then().statusCode(200);
        response.then().body("quotes", hasKey("USDEUR"));
        response.then().body("quotes", hasKey("USDGBP"));
        response.then().body("quotes", hasKey("USDJPY"));
    }

    @Test
    public void testInvalidAPIKey() {
        response = RestAssured.get(Consts.BASE_URL + "/live?apikey=" + Consts.INVALID_KEY);
        response.then().statusCode(401);
        response.then().body("message", equalTo("Invalid authentication credentials"));
    }
    @Test
    public void testMissingDateParameter() {
        response = given()
                .when()
                .get(Consts.BASE_URL + "/historical?apikey=" + Consts.API_KEY)
                .then()
                .extract()
                .response();

        response.then().statusCode(200);
        response.then().body("success", equalTo(false));
        response.then().body("error.code", equalTo(301));
        response.then().body("error.info", containsString("You have not specified a date. [Required format: date=YYYY-MM-DD]"));
    }
    @Test
    public void testInvalidDateParameter() {
        response = given()
                .when()
                .get(Consts.BASE_URL + "/historical?date=INVALID&apikey=" + Consts.API_KEY)
                .then()
                .extract()
                .response();

        response.then().statusCode(200);
        response.then().body("success", equalTo(false));
        response.then().body("error.code", equalTo(302));
        response.then().body("error.info", containsString("You have entered an invalid date. [Required format: date=YYYY-MM-DD]"));
    }

    @Test
    public void testHistoricalEndpointWithDateVerification() {
        String date = "2018-01-01";
        response = RestAssured.get(Consts.BASE_URL + "/historical?date=" + date + "&apikey=" + Consts.API_KEY);
        response.then().statusCode(200);

        long timestamp = response.jsonPath().getLong("timestamp");

        String responseDate = Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        assertEquals(date, responseDate);
    }
}
