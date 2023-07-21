package com.example.springbootwebflux.app;

import com.example.springbootwebflux.app.models.Product;
import com.example.springbootwebflux.app.repository.ProductRepository;
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
    private ProductRepository productRepository;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    private static final Logger log = LoggerFactory.getLogger(SpringBootWebFluxApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootWebFluxApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        mongoTemplate.dropCollection("products").subscribe();
        Flux.just(
                new Product("TV Samsung", 350.89),
                new Product("Redmi 11 pro", 400.99),
                new Product("Laptop HP omen 16", 955.89),
                new Product("Mouse gaming", 20.99),
                new Product("Monitor 4k Ozone", 350.89),
                new Product("Kindle 11", 200.59),
                new Product("Desk", 100.99))
                .flatMap(product -> {
                    product.setCreatedAt(LocalDateTime.now());
                    return productRepository.save(product);
                })
                .subscribe(product -> log.info("Insert: " + product.getId() + " " + product.getName()));
    }
}
