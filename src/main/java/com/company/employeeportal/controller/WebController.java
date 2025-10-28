package com.company.employeeportal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller for serving HTML pages.
 * Handles navigation and page routing for the Employee Portal web interface.
 */
@Controller
public class WebController {

    /**
     * Serve the login page.
     */
    @GetMapping({"/", "/login"})
    public String login() {
        return "login";
    }

    /**
     * Serve the main dashboard page.
     * This page will use JavaScript to load content based on user role.
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}