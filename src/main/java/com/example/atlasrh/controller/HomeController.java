package com.example.atlasrh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HomeController
 */
@RestController
@Tag(name = "Home", description = "Application landing endpoint")
public class HomeController {
  @GetMapping("/")
  @Operation(summary = "Get the application greeting")
  public String home() {
    return new String("Hello World");
  }

}
