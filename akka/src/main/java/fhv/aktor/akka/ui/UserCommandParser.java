package fhv.aktor.akka.ui;

public interface UserCommandParser {
    void execute(String input) throws InputParsingException;
}
