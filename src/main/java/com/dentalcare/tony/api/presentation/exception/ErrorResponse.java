package com.dentalcare.tony.api.presentation.exception;


import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private String message;
    private int status;
    private String path;
    private LocalDateTime timestamp;
    private List<FieldErrorDetail> errors;
}
