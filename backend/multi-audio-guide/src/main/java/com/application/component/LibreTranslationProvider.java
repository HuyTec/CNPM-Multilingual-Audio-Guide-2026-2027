package com.application.component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
@Component @ConditionalOnProperty(name="translation.provider",havingValue="libretranslate")
public class LibreTranslationProvider implements TranslationProvider {
    private final URI endpoint;private final String apiKey;
    private final Duration requestTimeout;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final JsonMapper json=JsonMapper.builder().build();
    private record Body(String q,String source,String target,String format,String api_key) { }
    private record Reply(String translatedText) { }
    public LibreTranslationProvider(@Value("${translation.url:http://localhost:5000/translate}") String url,
                                    @Value("${translation.api-key:}") String apiKey,
                                    @Value("${translation.timeout-seconds:30}") long timeoutSeconds) {
        endpoint=URI.create(url);
        this.apiKey=apiKey;
        requestTimeout=Duration.ofSeconds(timeoutSeconds);
    }
    private static String providerLanguage(String code) {
        return "zh".equalsIgnoreCase(code) ? "zh-Hans" : code;
    }
    public String name() { return "libretranslate"; }
    public String translate(String text,String source,String target) {
        if(text==null || text.isBlank()) return "";
        try {
            var body=new Body(text,providerLanguage(source),providerLanguage(target),"text",apiKey);
            var request=HttpRequest.newBuilder(endpoint).timeout(requestTimeout).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200) throw new IllegalStateException("Provider HTTP "+response.statusCode());
            String result=json.readValue(response.body(),Reply.class).translatedText();
            if(result==null) throw new IllegalStateException("Missing translation");return result;
        } catch(InterruptedException error) { Thread.currentThread().interrupt();throw new IllegalStateException("Translation interrupted",error); }
        catch(Exception error) { throw new IllegalStateException("Translation unavailable",error); }
    }
}
