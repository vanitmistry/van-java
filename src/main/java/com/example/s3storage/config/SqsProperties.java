package com.example.s3storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sqs")
public class SqsProperties {

    private String outQueueName;

    private String inQueueName;

    private String region = "us-east-1";

    /** Set only for local/test profiles to point the client at LocalStack instead of real AWS. */
    private String endpointOverride;

    public String getOutQueueName() {
        return outQueueName;
    }

    public void setOutQueueName(String outQueueName) {
        this.outQueueName = outQueueName;
    }

    public String getInQueueName() {
        return inQueueName;
    }

    public void setInQueueName(String inQueueName) {
        this.inQueueName = inQueueName;
    }

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
