package com.polatholding.procurementsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomErrorController {

    @GetMapping("/error/403")
    public String accessDenied() {
        return "403"; // This corresponds to src/main/resources/templates/403.html
    }
}