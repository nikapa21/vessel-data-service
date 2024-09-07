package com.deepsea.vesseldataservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VesselController {

    @GetMapping("/hello")
    public String sayHello() {

        return "Hello!";
    }
}