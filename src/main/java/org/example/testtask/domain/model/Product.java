package org.example.testtask.domain.model;

import lombok.Data;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {
    private String productId;
    private String productName;
}