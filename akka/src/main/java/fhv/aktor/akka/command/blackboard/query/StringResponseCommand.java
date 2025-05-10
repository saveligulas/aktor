package fhv.aktor.akka.command.blackboard.query;

import fhv.aktor.akka.command.blackboard.BlackboardCommand;

public class StringResponseCommand implements QueryResponseCommand<String>, BlackboardCommand {
    private String string;

    @Override
    public void fromValue(String value) {
        this.string = string;
    }

    @Override
    public Class<String> getValueType() {
        return String.class;
    }
}
