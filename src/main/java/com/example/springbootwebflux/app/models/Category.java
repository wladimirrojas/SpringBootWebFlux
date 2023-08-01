package com.example.springbootwebflux.app.models;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Document("categories")
public class Category {

    @Id
    @NotEmpty
    private String id;
    private String name;

    public Category(String name) {
        this.name = name;
    }
}
