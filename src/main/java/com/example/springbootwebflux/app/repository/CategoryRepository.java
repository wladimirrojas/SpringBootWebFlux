package com.example.springbootwebflux.app.repository;

import com.example.springbootwebflux.app.models.Category;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CategoryRepository extends ReactiveMongoRepository<Category, String> {
}
