package org.esercizi.expensetrackerapi.exceptions;

public class RegistrationConflictException extends RuntimeException {
    public enum Field { USERNAME, EMAIL }

    private final Field field;

    public RegistrationConflictException(Field field) {
        super(field == Field.USERNAME ? "Username già registrato" : "Email già registrata");
        this.field = field;
    }

    public String getErrorName() {
        return field.name() + "_ALREADY_EXISTS";
    }
}
