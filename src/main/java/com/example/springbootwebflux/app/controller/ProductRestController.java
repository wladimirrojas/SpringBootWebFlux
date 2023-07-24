package com.example.springbootwebflux.app.controller;

import com.example.springbootwebflux.app.models.Product;
import com.example.springbootwebflux.app.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/products")
public class ProductRestController {

    @Autowired
    private ProductService productService;

    private static final Logger log = LoggerFactory.getLogger(ProductRestController.class);

    @GetMapping()
    public Flux<Product> index() {

        Flux<Product> products = productService.findAllToUpper()
                .doOnNext(product -> log.info(product.getName()));

        return products;
    }

    @GetMapping("/{id}")
    public Mono<Product> productById(@PathVariable String id) {

        Flux<Product> products = productService.findAll();

        Mono<Product> product = products.filter(p -> p.getId().equals(id)).next()
                .doOnNext(p -> log.info(p.getName()));

        //Mono<Product> product = productRepository.findById(id);

        return product;
    }
}
