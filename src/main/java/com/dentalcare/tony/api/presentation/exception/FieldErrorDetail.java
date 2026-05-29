package com.dentalcare.tony.api.presentation.exception;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldErrorDetail {
    private String field;
    private String error;
}
