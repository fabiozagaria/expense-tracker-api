package org.esercizi.expensetrackerapi.dto;

import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Elenco con tetto massimo e indicazione che altri elementi sono rimasti fuori.
 * Serializable perché quello delle spese passa dalla cache Redis.
 */
public record CappedList<T>(List<T> items, boolean truncated) implements java.io.Serializable {
    public static final int MAX_SIZE = 1000;
    public static final String TRUNCATED_HEADER = "X-Result-Truncated";

    /** Corpo = array JSON degli elementi (forma storica); l'header compare solo se l'elenco è stato tagliato. */
    public ResponseEntity<List<T>> toResponse() {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (truncated) {
            response.header(TRUNCATED_HEADER, "true");
        }
        return response.body(items);
    }
}
