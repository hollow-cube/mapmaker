package net.hollowcube.schem.reader;

public class SchematicReadException extends RuntimeException {

    SchematicReadException(String message) {
        super(message);
    }

    SchematicReadException(String message, Throwable cause) {
        super(message, cause);
    }

    SchematicReadException(Throwable cause) {
        super(cause);
    }
}
