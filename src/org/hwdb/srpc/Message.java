package org.hwdb.srpc;

/**
* @author Magnus Morton
*/
public class Message {
    Connection connection;
    String content;


    Message(Connection conn, String s) {
        connection = conn;
        content = s;
    }

    /**
     * The content of the query
     * @return the content of the Message
     */
    public String getContent() {
        return content;
    }

    /**
     * The connection which sent this Message
     * @return a Connection object representing the remote connection
     */
    public Connection getConnection() {
        return connection;
    }
}
