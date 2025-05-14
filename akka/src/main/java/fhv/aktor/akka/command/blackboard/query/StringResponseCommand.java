package fhv.aktor.akka.command.blackboard.query;

public class StringResponseCommand implements QueryResponseCommand<Object> {
    private String value;
    
    @Override
    public void fromValue(Object value) {
        this.value = value != null ? value.toString() : "UNDEFINED";
    }
    
    @Override
    public Class<Object> getValueType() {
        return Object.class;
    }
    
    public String value() {
        return value != null ? value : "UNDEFINED";
    }
}
