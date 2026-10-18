package in.codekerdos.classifier.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration 
public class AiClientConfig {

    @Bean 
    public RestClientCustomizer groqTimeoutCustomizer(){
        return 
        builder ->{
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(3_000);
            factory.setReadTimeout(8_000);
            builder.requestFactory(factory);
        };
    }
    
}
