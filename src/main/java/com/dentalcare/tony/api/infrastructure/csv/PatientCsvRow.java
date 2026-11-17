package com.dentalcare.tony.api.infrastructure.csv;

/**
 * One data row of a patient CSV file, already mapped and trimmed by the parser.
 *
 * @param row        physical line number in the file (header included, blank lines counted)
 * @param name       value of the {@code name} column, {@code null} if the row is too short
 * @param email      value of the {@code email} column, {@code null} if the row is too short
 * @param phone      value of the {@code phone} column, {@code null} if the row is too short
 * @param birthDate  raw value of the {@code birthDate} column ({@code yyyy-MM-dd}), {@code null} if the row is too short
 */
public record PatientCsvRow(
        int row,
        String name,
        String email,
        String phone,
        String birthDate
) {
}
