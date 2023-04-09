/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.file.Paths;

/**
 * Provides a means to cleanly shutdown an instance from the command line.
 *
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Shutdown {

    private static final int DEFAULT_PORT = 9999;
    private static final String DEFAULT_TOKEN = "cafebeef";

    /**
     * Connects to the given port (default: 9999) and invokes shutdown.
     * <ul>
     * <li>Arg 0 = port number</li>
     * <li>Arg 1 = token</li>
     * </ul>
     *
     * @param args
     *            see args list
     */
    public static void main(String[] args) {
        // check for port
        int port = parsePort(args);
        String token = getToken(args);
        connectAndSendShutdown(port, token);
        

       
    }

    public static int parsePort(String[] args){
        if (args.length == 0) {
            System.err.println("No port specified, using default: " + DEFAULT_PORT);
            args = new String[] { String.valueOf(DEFAULT_PORT) };
        }
        return Integer.valueOf(args[0]);
    }

    private static String getToken(String[] args){
        String token = DEFAULT_TOKEN;
        if(args.length > 1){
            token = args[1];
        } else {
            // read the token from the file
            try {
                File tokenFile = Paths.get("shutdown.token").toFile();
                RandomAccessFile raf = new RandomAccessFile(tokenFile, "r");
                byte[] buf = new byte[36];
                raf.readFully(buf);
                token = new String(buf);
                System.out.printf("Token loaded: %s%n", token);
                raf.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return token;
    }

    private static void connectAndSendShutdown(int port, String token){
         // check for host
         String host = System.getProperty("red5.shutdown.host", "127.0.0.1");
         try (Socket clientSocket = new Socket(host, port);
              PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); 
              BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));) {
             System.out.printf("Connected - local: %s remote: %s%n", clientSocket.getLocalSocketAddress().toString(), clientSocket.getRemoteSocketAddress().toString());
            
                // send the token
                sendToken(out, token);
         } catch (Exception e) {
             System.err.printf("Exception connecting to %s%n", host);
             e.printStackTrace();
             System.exit(1);
         }
    }

    private static void sendToken(PrintWriter out, String token){
        out.println(token);
    }
}
