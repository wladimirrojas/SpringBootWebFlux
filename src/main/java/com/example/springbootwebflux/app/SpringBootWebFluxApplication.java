package com.example.springbootwebflux.app;

import com.example.springbootwebflux.app.models.Category;
import com.example.springbootwebflux.app.models.Product;
import com.example.springbootwebflux.app.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@SpringBootApplication
public class SpringBootWebFluxApplication implements CommandLineRunner {

    @Autowired
    private ProductService service;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    private static final Logger log = LoggerFactory.getLogger(SpringBootWebFluxApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootWebFluxApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        mongoTemplate.dropCollection("products").subscribe();
        mongoTemplate.dropCollection("categories").subscribe();

        Category tv = new Category("TV");
        Category smartphone = new Category("Smartphone");
        Category laptop = new Category("Laptop");
        Category peripherals = new Category("Peripherals");
        Category book = new Category("Book");
        Category furniture = new Category("Furniture");

        Flux.just(tv, smartphone, laptop, peripherals, book, furniture)
                .flatMap(service::saveCategory)
                .doOnNext(category -> log.info("Insert: " + category.getId() + " " + category.getName()))
                .thenMany(Flux.just(
                                new Product("TV Samsung", 350.89, tv),
                                new Product("Redmi 11 pro", 400.99, smartphone),
                                new Product("Laptop HP omen 16", 955.89, laptop),
                                new Product("Mouse gaming", 20.99, peripherals),
                                new Product("Monitor 4k Ozone", 350.89, tv),
                                new Product("Kindle 11", 200.59, book),
                                new Product("Desk", 100.99, furniture))
                        .flatMap(product -> {
                            product.setCreatedAt(LocalDateTime.now());
                            return service.save(product);
                        }))
                .subscribe(product -> log.info("Insert: " + product.getId() + " " + product.getName()));
    }
}
