package fhv.aktor.akka.ui;

public interface UserCommandParser {
    CommandResponse execute(String input) throws InputParsingException;
}
