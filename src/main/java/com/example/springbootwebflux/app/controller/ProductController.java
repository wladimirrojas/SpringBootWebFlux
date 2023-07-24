package com.example.springbootwebflux.app.controller;

import com.example.springbootwebflux.app.models.Product;
import com.example.springbootwebflux.app.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@SessionAttributes("product")
@Controller
public class ProductController {

    @Autowired
    private ProductService productService;

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @GetMapping("/listing-chunked")
    public String toListChunked(Model model) {

        Flux<Product> products = productService.findAllToUpper()
                .repeat(5000); //todo: 6.87

        model.addAttribute("products", products);
        model.addAttribute("title", "List of products");

        return "toList-chunked";
    }

    @GetMapping("/listing-full")
    public String toListFull(Model model) {

        Flux<Product> products = productService.findAllToUpper()
                .repeat(5000); //todo: 6.87

        model.addAttribute("products", products);
        model.addAttribute("title", "List of products");

        return "toList";
    }

    @GetMapping("/listing-datadriven")
    public String toListDataDriven(Model model) {

        Flux<Product> products = productService.findAllToUpper()
                .delayElements(Duration.ofSeconds(1));

        products.subscribe(product -> log.info(product.getName()));

        model.addAttribute("products", new ReactiveDataDriverContextVariable(products, 2));
        model.addAttribute("title", "List of products");

        return "toList";
    }

    @GetMapping({"/listing", "/"})
    public String toList(Model model) {

        Flux<Product> products = productService.findAllToUpper();
        products.subscribe(product -> log.info(product.getName()));

        model.addAttribute("products", products);
        model.addAttribute("title", "List of products");

        return "toList";
    }

    @GetMapping("/form")
    public Mono<String> create(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("title", "Product form");
        model.addAttribute("button", "Create");
        return Mono.just("form");
    }

    @GetMapping("/form/{id}")
    public Mono<String> edit(@PathVariable String id, Model model) {
        Mono<Product> productMono = productService.findById(id)
                .doOnNext(p -> {
                    log.info("Product: " + p.getName());
                }).defaultIfEmpty(new Product());
        model.addAttribute("title", "Edit product");
        model.addAttribute("product", productMono);
        model.addAttribute("button", "Edit");

        return Mono.just("form");
    }

    @GetMapping("/form-v2/{id}")
    public Mono<String> editv2(@PathVariable String id, Model model) {
        return productService.findById(id)
                .doOnNext(p -> {
                    log.info("Product: " + p.getName());
                    model.addAttribute("title", "Edit product");
                    model.addAttribute("product", p);
                    model.addAttribute("button", "Edit");


                }).defaultIfEmpty(new Product())
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(new InterruptedException("Product does not exist"));
                    }
                    return Mono.just(p);
                })
                .then(Mono.just("form"))
                .onErrorResume(ex -> Mono.just("redirect:/listing?error=product+does+not+exist"));

    }

    @PostMapping("/form")
    public Mono<String> save(@Valid Product product, BindingResult result, Model model, SessionStatus status) {

        if (result.hasErrors()) {
            model.addAttribute("title", "Error in product form");
            model.addAttribute("button", "Save");
            return Mono.just("form");
        }

        status.setComplete();

        if (product.getCreatedAt() == null) {
            product.setCreatedAt(LocalDateTime.now());
        }

        return productService.save(product).doOnNext(p -> {
            log.info("Product saved: " + p.getName() + " Id: " + p.getId());
        }).thenReturn("redirect:/listing?success=product+saved+successfully");
    }

}
