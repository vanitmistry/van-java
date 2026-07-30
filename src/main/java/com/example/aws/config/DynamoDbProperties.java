package com.example.aws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dynamodb")
public class DynamoDbProperties {

    private String region;
    private String endpointOverride;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpointOverride() {
        return endpointOverride;
    }

    public void setEndpointOverride(String endpointOverride) {
        this.endpointOverride = endpointOverride;
    }
}
