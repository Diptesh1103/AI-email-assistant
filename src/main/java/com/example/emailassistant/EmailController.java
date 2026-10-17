package com.example.emailassistant;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final AiService ai;

    public EmailController(AiService ai) { this.ai = ai; }

    @PostMapping("/assist")
    public EmailResponse assist(@Valid @RequestBody EmailRequest request) {
        return new EmailResponse(ai.run(request));
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody
    public org.springframework.http.ResponseEntity<Map<String, String>> handle(ResponseStatusException e) {
        return org.springframework.http.ResponseEntity.status(e.getStatusCode())
                .body(Map.of("error", e.getReason() == null ? "Request failed" : e.getReason()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public Map<String, String> invalid() {
        return Map.of("error", "Add some email text first (max 20,000 characters).");
    }
}
