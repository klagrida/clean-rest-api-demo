package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ProductRequest(

    @NotBlank(message = "name must not be blank")
    String name,

    @Positive(message = "price must be positive")
    double price
) {}
