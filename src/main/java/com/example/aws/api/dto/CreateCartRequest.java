package com.example.aws.api.dto;

public class CreateCartRequest {
    private String name;
    private String address;

    public CreateCartRequest() {
    }

    public CreateCartRequest(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
