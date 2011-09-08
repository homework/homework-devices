/**
* @author Magnus Morton
*/

/**
 * Provides a Java implementation of Homework's Simple RPC system - a UDP based RPC protocol.
 * <p>
 *     To use this, you only need to know about {@link org.hwdb.srpc.SRPC}, {@link org.hwdb.srpc.Connection}, {@link org.hwdb.srpc.Service} and {@link org.hwdb.srpc.Message}
 * </p>
 * <p>
 * <h2>Usage</h2>
 * to use this package, it is first necessary to instantiate an SRPC instance:
 * </p>
 * <p>
 * <code>SRPC srpc = new SRPC(port)</code>
 * <p>
 * port is optional here
 * </p>
 * </p>
 * <p>
 * To connect, invoke the connect method on the SRPC object:
 * <p>
 * <code>Connection connection = srpc.connect(host, port, serviceName) </code>
 * </p>
 * </p>
 * <p>
 * To call:
 *     <p>
 *   <code>connection.call(query)</code>
 *     </p>
 * </p>
 *
 * <p>
 *     To offer a service:
 *     <p>
 *         <code>Service service = srpc.offer(serviceName)</code>
 *     </p>
 * </p>
 * <p>
 *     To obtain a received query/connection from a service:
 *     <p>
 *         <code>Message query = service.query()</code>
 *     </p>
 * </p>
 * <p>
 *     To disconnect a connection:
 *     <p>
 *         <code>query.disconnect();</code>
 *     </p>
 * </p>
 *
 * <h2>Example</h2>
 * <pre>
 * {@code
 *
 *     public class HWDBClient implements Runnable
 *{
 *
 *   public static void main(String[] args){
 *       try {
 *           String serviceName = "Handler";
 *           SRPC srpc = new SRPC();
 *           service = srpc.offer(serviceName);
            (new Thread(new HWDBClient())).start();
            Connection conn = srpc.connect("localhost", 987,"HWDB");
            int port = srpc.details().getPort();
            System.out.println(conn.call(String.format("SQL:subscribe TestQuery 127.0.0.1 %d %s", port, serviceName)));

            } catch (Exception e) {
                System.exit(1);
            }

    }



    static Service service;
    public void run() {
        try {
            Message query;
            while ((query = service.query()) != null) {
                System.out.println(query.getContent());
                query.getConnection().response("OK");
            }
        } catch (IOException e) {
            System.exit(1);
        }
    }
 * }
 * }
 * </pre>
 */
package org.hwdb.srpc;
