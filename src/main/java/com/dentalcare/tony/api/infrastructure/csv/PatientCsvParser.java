package com.dentalcare.tony.api.infrastructure.csv;

import com.dentalcare.tony.api.presentation.exception.BusinessException;
import org.apache.commons.csv.CSVException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses the patient CSV import file with Apache Commons CSV (never string splitting).
 *
 * <p>The header must contain the exact, case-sensitive columns
 * {@code name,email,phone,birthDate} (order does not matter). Blank lines are
 * skipped by the library. Row numbers are physical line numbers in the file.</p>
 */
@Component
public class PatientCsvParser {

    private static final List<String> REQUIRED_COLUMNS = List.of("name", "email", "phone", "birthDate");

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader() // no-arg: read the header from the first record
            .get();

    /**
     * Reads every data row of the given CSV stream.
     *
     * @throws BusinessException        if the file is empty or the header is missing/wrong
     * @throws UncheckedIOException     if the stream cannot be read
     */
    public List<PatientCsvRow> parse(InputStream inputStream) {
        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8), FORMAT)) {

            validateHeader(parser);

            List<PatientCsvRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                rows.add(new PatientCsvRow(
                        (int) parser.getCurrentLineNumber(), // physical line of the record just read
                        value(record, "name"),
                        value(record, "email"),
                        value(record, "phone"),
                        value(record, "birthDate")
                ));
            }
            return rows;
        } catch (IllegalArgumentException e) {
            // blank or duplicate header names are rejected by Commons CSV itself
            throw new BusinessException("Invalid CSV header: " + e.getMessage());
        } catch (CSVException e) {
            // malformed CSV (e.g. unclosed quote) is a client error, not a 500
            throw new BusinessException("Malformed CSV file: " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read CSV file", e);
        }
    }

    private void validateHeader(CSVParser parser) {
        Map<String, Integer> header = parser.getHeaderMap();
        if (header.isEmpty()) {
            throw new BusinessException("CSV file is empty or has no header row");
        }
        for (String column : REQUIRED_COLUMNS) {
            if (!header.containsKey(column)) {
                throw new BusinessException(
                        "CSV header must contain the columns: name, email, phone, birthDate");
            }
        }
    }

    private String value(CSVRecord record, String column) {
        // short rows do not map every column — report null instead of throwing
        return record.isSet(column) ? record.get(column).trim() : null;
    }
}
