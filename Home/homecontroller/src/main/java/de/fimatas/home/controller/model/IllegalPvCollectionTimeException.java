package de.fimatas.home.controller.model;

@SuppressWarnings("unused")
public class IllegalPvCollectionTimeException extends IllegalStateException{
    public IllegalPvCollectionTimeException() {
        super();
    }
    public IllegalPvCollectionTimeException(String s) {
        super(s);
    }
    public IllegalPvCollectionTimeException(String message, Throwable cause) {
        super(message, cause);
    }
    public IllegalPvCollectionTimeException(Throwable cause) {
        super(cause);
    }
}
