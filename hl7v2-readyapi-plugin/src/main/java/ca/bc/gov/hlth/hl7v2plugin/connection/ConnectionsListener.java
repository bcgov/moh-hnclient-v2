package ca.bc.gov.hlth.hl7v2plugin.connection;

public interface ConnectionsListener {
    public void connectionChanged(Connection connection, String propertyName, Object oldPropertyValue,
            Object newPropertyValue);

    public void connectionListChanged();
}
