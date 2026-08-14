package com.yekdb.query.expression;

import com.yekdb.query.exception.AmbiguousColumnException;
import com.yekdb.query.exception.UnknownColumnException;
import com.yekdb.query.executor.JoinedRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QualifiedColumnResolverTest {

    private QualifiedColumnResolver resolver;
    private JoinedRow joinedRow;

    @BeforeEach
    void setUp() {

        resolver = new QualifiedColumnResolver();

        joinedRow = new JoinedRow();

        /*
         * Testlerde iki tabloyu temsil eden örnek JOIN satırı oluşturuyoruz.
         *
         * employee -> e
         * department -> d
         */
        joinedRow.put("e.id", 1);
        joinedRow.put("e.name", "Yunus Emre");
        joinedRow.put("e.department_id", 10);

        joinedRow.put("d.id", 10);
        joinedRow.put("d.name", "Software");
    }

    @Test
    void qualifiedColumnShouldResolve() {

        /*
         * e.name açıkça belirtildiği için doğrudan employee
         * tarafındaki name kolonu çözülmelidir.
         */
        Object result = resolver.resolve(
                joinedRow,
                "e",
                "name"
        );

        assertEquals("Yunus Emre", result);
    }

    @Test
    void uniqueUnqualifiedColumnShouldResolve() {

        /*
         * department_id sadece employee tablosunda bulunduğu için
         * qualifier belirtilmeden güvenli şekilde çözülebilir.
         */
        Object result = resolver.resolve(
                joinedRow,
                null,
                "department_id"
        );

        assertEquals(10, result);
    }

    @Test
    void ambiguousColumnShouldThrow() {

        /*
         * id kolonu hem e.id hem de d.id olarak bulunduğu için
         * qualifier kullanılmadan çözülmeye çalışılması belirsizdir.
         */
        assertThrows(
                AmbiguousColumnException.class,
                () -> resolver.resolve(
                        joinedRow,
                        null,
                        "id"
                )
        );
    }

    @Test
    void unknownQualifiedColumnShouldThrow() {

        /*
         * e.salary şeklinde bir kolon JOIN satırında bulunmadığı için
         * UnknownColumnException fırlatılmalıdır.
         */
        assertThrows(
                UnknownColumnException.class,
                () -> resolver.resolve(
                        joinedRow,
                        "e",
                        "salary"
                )
        );
    }

    @Test
    void unknownUnqualifiedColumnShouldThrow() {

        /*
         * salary hiçbir tabloda bulunmadığı için
         * UnknownColumnException fırlatılmalıdır.
         */
        assertThrows(
                UnknownColumnException.class,
                () -> resolver.resolve(
                        joinedRow,
                        null,
                        "salary"
                )
        );
    }
}