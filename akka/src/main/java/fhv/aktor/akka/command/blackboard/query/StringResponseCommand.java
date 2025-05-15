package fhv.aktor.akka.command.blackboard.query;

public class StringResponseCommand implements QueryResponseCommand<Object> {
    private String value;
    private String key;
    
    @Override
    public void build(String key, Object value) {
        this.value = value != null ? value.toString() : "UNDEFINED";
        this.key = key;
    }
    
    @Override
    public Class<Object> getValueType() {
        return Object.class;
    }
    
    public String value() {
        return value != null ? value : "UNDEFINED";
    }
    public String key() {
        return key;
    }
}
