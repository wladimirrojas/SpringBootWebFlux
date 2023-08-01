package com.example.springbootwebflux.app.controller;

import com.example.springbootwebflux.app.models.Category;
import com.example.springbootwebflux.app.models.Product;
import com.example.springbootwebflux.app.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@SessionAttributes("product")
@Controller
public class ProductController {

    @Autowired
    private ProductService service;

    @Value("${config.pictures.path}")
    private String path;

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @ModelAttribute("categories")
    public Flux<Category> categories() {
        return service.findAllCategory();
    }

    @GetMapping("/look/{id}")
    public Mono<String> look(Model model, @PathVariable String id) {
        return service.findById(id)
                .doOnNext(p -> {
                    model.addAttribute("product", p);
                    model.addAttribute("title", "Product detail");
                }).switchIfEmpty(Mono.just(new Product()))
                .flatMap(p -> {
                    if (p.getId() == null) return Mono.error(new InterruptedException("Product does not exist"));
                    return Mono.just(p);
                }).then(Mono.just("look"))
                .onErrorResume(ex -> Mono.just("redirect:/listing?error=product+does+not+exist"));
    }

    @GetMapping("/pictures/img/{pictureName:.+}")
    public Mono<ResponseEntity<Resource>> lookPicture(@PathVariable String pictureName) throws MalformedURLException {
        Path picturePath = Paths.get(path).resolve(pictureName).toAbsolutePath();

        Resource picture = new UrlResource(picturePath.toUri());

        return Mono.just(
                ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + picture.getFilename() + "\"")
                        .body(picture)
        );
    }

    @GetMapping("/listing-chunked")
    public String toListChunked(Model model) {

        Flux<Product> products = service.findAllToUpper()
                .repeat(5000); //todo: 6.87

        model.addAttribute("products", products);
        model.addAttribute("title", "List of products");

        return "toList-chunked";
    }

    @GetMapping("/listing-full")
    public String toListFull(Model model) {

        Flux<Product> products = service.findAllToUpper()
                .repeat(5000); //todo: 6.87

        model.addAttribute("products", products);
        model.addAttribute("title", "List of products");

        return "toList";
    }

    @GetMapping("/listing-datadriven")
    public String toListDataDriven(Model model) {

        Flux<Product> products = service.findAllToUpper()
                .delayElements(Duration.ofSeconds(1));

        products.subscribe(product -> log.info(product.getName()));

        model.addAttribute("products", new ReactiveDataDriverContextVariable(products, 2));
        model.addAttribute("title", "List of products");

        return "toList";
    }

    @GetMapping({"/listing", "/"})
    public String toList(Model model) {

        Flux<Product> products = service.findAllToUpper();
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
        Mono<Product> productMono = service.findById(id)
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
        return service.findById(id)
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
    public Mono<String> save(@Valid Product product, BindingResult result, Model model, @RequestPart FilePart file, SessionStatus status) {

        if (result.hasErrors()) {
            model.addAttribute("title", "Error in product form");
            model.addAttribute("button", "Save");
            return Mono.just("form");
        }

        status.setComplete();



        Mono<Category> category = service.findCategoryById(product.getCategory().getId());

        return category.flatMap(c -> {
            if (product.getCreatedAt() == null) product.setCreatedAt(LocalDateTime.now());
            if (!file.filename().isEmpty()) product.setPicture(UUID.randomUUID().toString() + "-" + file.filename()
                    .replace(" ", "")
                    .replace(":", "")
                    .replace("\\", ""));

            product.setCategory(c);
            return service.save(product);
        }).doOnNext(p -> {
            log.info("Product saved: " + p.getName() + " Id: " + p.getId());
            log.info("With Category: " + p.getCategory().getName() + " Id: " + p.getCategory().getId());

        }).flatMap(p -> {
                    if (!file.filename().isEmpty()) return file.transferTo(new File(path + p.getPicture()));
                    return Mono.empty();
                })
                .thenReturn("redirect:/listing?success=product+saved+successfully");
    }

    @GetMapping("/delete/{id}")
    public Mono<String> delete(@PathVariable String id) {
        return service.findById(id)
                .defaultIfEmpty(new Product())
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(new InterruptedException("Product does not exist to delete"));
                    }
                    return Mono.just(p);
                }).flatMap(service::delete)
                .then(Mono.just("redirect:/listing?success=product+deleted+succesfully"))
                .onErrorResume(ex -> Mono.just("redirect:/listing?error=product+does+not+exist+to+delete"));
    }

}
