package org.group1.coffeeshopapi.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Server-rendered pages that front the JSON API for a browser (account verification, login,
 * and — once logged in — connecting a Telegram chat). Each template drives the actual
 * {@code /api/auth/**} and {@code /api/users/**} endpoints itself via {@code fetch()}, since
 * the API is stateless JWT auth rather than a server session.
 */
@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/verify")
    public String verify() {
        return "verify";
    }
}
