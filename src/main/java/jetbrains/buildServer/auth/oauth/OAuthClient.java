package jetbrains.buildServer.auth.oauth;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.log4j.Logger;
import org.json.simple.JSONValue;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Map;

public class OAuthClient {

    private AuthenticationSchemeProperties properties;
    private static final Logger log = Logger.getLogger(OAuthClient.class);
    private final OkHttpClient httpClient;

    public OAuthClient(AuthenticationSchemeProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClientFactory.createClient(true);
    }

    public String getRedirectUrl(String state) {
        return String.format("%s?response_type=code&client_id=%s&scope=%s&state=%s&redirect_uri=%s",
                properties.getAuthorizeEndpoint(),
                properties.getClientId(),
                properties.getScope(),
                state,
                properties.getRootUrl());
    }

    public String getAccessToken(String code) throws IOException {
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", properties.getRootUrl())
                .add("client_id", properties.getClientId())
                .add("client_secret", properties.getClientSecret())
                .build();

        Request request = new Request.Builder()
                .url(properties.getTokenEndpoint())
                .addHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .post(formBody)
                .build();
        Response response = httpClient.newCall(request).execute();
        Map jsonResponse = (Map) JSONValue.parse(response.body().string());
        return (String) jsonResponse.get("access_token");
    }

    public Map getUserData(String token) throws IOException {
        String url = String.format("%s?access_token=%s", properties.getUserEndpoint(), token);
        Request request = new Request.Builder().url(url).build();
        String response = httpClient.newCall(request).execute().body().string();
        log.debug("Fetched user data: " + response);
        return (Map) JSONValue.parse(response);
    }
}
