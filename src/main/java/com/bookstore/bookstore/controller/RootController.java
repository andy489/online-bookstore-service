package com.bookstore.bookstore.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    record AppInfo(String app, String version, String status, String docs) {}

    @GetMapping("/")
    public AppInfo root() {
        return new AppInfo(
                "Online Bookstore Service",
                "1.0.0",
                "running",
                "http://localhost:8080/swagger-ui.html"
        );
    }
}
