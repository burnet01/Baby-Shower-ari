package com.tennantari.gallery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GalleryApplication {
    public static void main(String[] args) {
        SpringApplication.run(GalleryApplication.class, args);
    }
}
