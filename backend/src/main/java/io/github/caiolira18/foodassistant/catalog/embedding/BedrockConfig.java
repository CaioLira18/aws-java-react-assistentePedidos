package io.github.caiolira18.foodassistant.catalog.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
class BedrockConfig {

    @Bean(destroyMethod = "close")
    BedrockRuntimeClient bedrockRuntimeClient(
            @Value("${aws.region}") String region,
            @Value("${aws.profile:}") String profile) {
        AwsCredentialsProvider credentials = profile.isBlank()
                ? DefaultCredentialsProvider.builder().build()
                : ProfileCredentialsProvider.create(profile);
        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .build();
    }
}
