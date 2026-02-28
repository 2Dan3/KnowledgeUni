package com.ftn.research.knowledgeuniverse.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.ftn.research.knowledgeuniverse.repository.index")
public class ElasticsearchConfiguration
    extends org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    @Value("${elasticsearch.username}")
    private String userName;

    @Value("${elasticsearch.password}")
    private String password;

//    @Override
//    public ClientConfiguration clientConfiguration() {
//        return ClientConfiguration.builder().connectedTo(host + ":" + port)
//            .withBasicAuth(userName, password).build();
//    }
    @Override
    public ClientConfiguration clientConfiguration() {
        var builder = ClientConfiguration.builder()
                .connectedTo(host + ":" + port);

        if (userName != null && !userName.isEmpty()) {
            builder = (ClientConfiguration.MaybeSecureClientConfigurationBuilder) builder.withBasicAuth(userName, password);
        }

        return builder.build();
    }
}
