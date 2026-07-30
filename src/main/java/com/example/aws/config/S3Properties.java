package com.example.aws.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.s3")
public class S3Properties {

    /** Default bucket used by the application; callers may still target other buckets explicitly. */
    private String bucket;

    private String region = "us-east-1";

    /** Set only for local/test profiles to point the client at LocalStack instead of real AWS. */
    private String endpointOverride;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
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
