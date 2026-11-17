package com.dentalcare.tony.api.presentation.dto.response;

import java.util.List;

/**
 * Summary returned by {@code POST /patients/import}: counts plus row-level errors.
 */
public record PatientImportResponse(
        int total,
        int imported,
        int failed,
        List<RowError> errors
) {

    /**
     * One rejected row: {@code row} is the physical line number in the CSV file
     * (header included, blank lines counted), {@code message} the reason.
     */
    public record RowError(int row, String message) {
    }
}
