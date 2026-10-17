package com.example.emailassistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** action: draft | reply | summarize | improve | subjects */
public record EmailRequest(
        @NotBlank String action,
        String tone,
        @NotBlank @Size(max = 20000) String content,
        @Size(max = 2000) String instructions) {}
