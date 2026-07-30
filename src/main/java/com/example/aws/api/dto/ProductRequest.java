package com.example.aws.api.dto;

public class ProductRequest {
    private String name;
    private String description;
    private double cost;

    public ProductRequest() {
    }

    public ProductRequest(String name, String description, double cost) {
        this.name = name;
        this.description = description;
        this.cost = cost;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }
}
