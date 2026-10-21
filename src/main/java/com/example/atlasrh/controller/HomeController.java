package com.example.atlasrh.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HomeController
 */
@RestController
public class HomeController {
  @GetMapping("/")
  public String getMethodName() {
    return new String("Hello World");
  }

}
