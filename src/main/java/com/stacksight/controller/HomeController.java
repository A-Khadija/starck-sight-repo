package com.stacksight.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }
    
    @GetMapping("/about")
    public String about() {
        return "about"; // Create this view later
    }
    
    @GetMapping("/profile")
    public String profile() {
        return "profile"; // Create this view later
    }
    
    @GetMapping("/questions")
    public String questions() {
        return "questions"; // Create this view later
    }
    
    @GetMapping("/trends")
    public String trends() {
        return "trends"; // Create this view later
    }
}