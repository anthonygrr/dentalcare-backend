package com.dentalcare.tony.api.infrastructure.csv;

import com.dentalcare.tony.api.presentation.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain unit test for the CSV parser: no Spring context, no database.
 */
class PatientCsvParserTest {

    private final PatientCsvParser parser = new PatientCsvParser();

    private static InputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parsesRowsAndReportsPhysicalLineNumbers() {
        // header line 1, data line 2, blank line 3, data line 4
        String content = """
                name,email,phone,birthDate
                Import Alpha,imp-a@example.com,555-2001,1990-01-15

                Import Beta,imp-b@example.com,555-2002,1991-02-16
                """;

        List<PatientCsvRow> rows = parser.parse(csv(content));

        assertEquals(2, rows.size(), "blank lines must be skipped");

        PatientCsvRow first = rows.get(0);
        assertEquals(2, first.row(), "first data row sits on physical line 2");
        assertEquals("Import Alpha", first.name());
        assertEquals("imp-a@example.com", first.email());
        assertEquals("555-2001", first.phone());
        assertEquals("1990-01-15", first.birthDate());

        PatientCsvRow second = rows.get(1);
        assertEquals(4, second.row(), "blank line 3 shifts the second row to line 4");
        assertEquals("Import Beta", second.name());
    }

    @Test
    void acceptsColumnsInAnyOrder() {
        String content = """
                birthDate,name,phone,email
                1992-03-17,Import Gamma,555-2003,imp-g@example.com
                """;

        List<PatientCsvRow> rows = parser.parse(csv(content));

        assertEquals(1, rows.size());
        assertEquals("Import Gamma", rows.get(0).name());
        assertEquals("imp-g@example.com", rows.get(0).email());
        assertEquals("555-2003", rows.get(0).phone());
        assertEquals("1992-03-17", rows.get(0).birthDate());
    }

    @Test
    void rejectsHeaderMissingRequiredColumns() {
        String content = """
                name,email,phone
                Import Alpha,imp-a@example.com,555-2001
                """;

        BusinessException ex = assertThrows(BusinessException.class, () -> parser.parse(csv(content)));
        assertTrue(ex.getMessage().contains("birthDate"), "message must name the missing column");
    }

    @Test
    void rejectsEmptyFile() {
        BusinessException ex = assertThrows(BusinessException.class, () -> parser.parse(csv("")));
        assertTrue(ex.getMessage().contains("header"), "empty file must be reported as a header problem");
    }

    @Test
    void rejectsBlankHeaderColumnName() {
        String content = """
                name,,phone,birthDate
                Import Alpha,imp-a@example.com,555-2001,1990-01-15
                """;

        BusinessException ex = assertThrows(BusinessException.class, () -> parser.parse(csv(content)));
        assertTrue(ex.getMessage().contains("Invalid CSV header"));
    }

    @Test
    void mapsShortRowsToNullValuesInsteadOfThrowing() {
        String content = """
                name,email,phone,birthDate
                OnlyName
                """;

        List<PatientCsvRow> rows = parser.parse(csv(content));

        assertEquals(1, rows.size());
        PatientCsvRow row = rows.get(0);
        assertEquals(2, row.row());
        assertEquals("OnlyName", row.name());
        assertNull(row.email());
        assertNull(row.phone());
        assertNull(row.birthDate());
    }
}
