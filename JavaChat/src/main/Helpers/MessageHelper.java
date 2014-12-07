package main.Helpers;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/* this class help build message for each instance
 */
public class MessageHelper {

    public static final int CFG_ID_KEY = 1;
    public static final int CFG_NICK_KEY = 2;

    private BufferedReader reader;
    private BufferedWriter writer;
    private Socket socket;

    public MessageHelper(Socket socket) throws IOException {
        if (socket != null){
            this.socket = socket;
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
    }

    /* create a configure message with format C[key]:[value] */
    public String createConfigMessage(int type, String value){
        return String.format("C%2d:%s", type, value);
    }

    public String createChatMessage(String receiver, String content){
        return String.format("M%20s%s", receiver, content);
    }

    public String createUploadFirstMessage(String receiver, String filename){
        return String.format("F%20s%s", receiver, filename);
    }

    public byte[] createUploadMessage(int id, byte[] data, int count){
        byte[] buf = new byte[5+count];
        String idStr = String.format("T%4d", id);
        // copy header
        byte[] header = idStr.getBytes();
        for (int i=0; i < header.length; i++){
            buf[i] = header[i];
        }

        for (int i=0; i < count; i++){
            buf[i+header.length] = data[i];
        }
        return buf;
    }

    public void sendMessage(String message) throws IOException {
            writer.write(message);
            writer.flush();
    }

    public void sendRawMessage(byte[] message, int length) throws IOException {
        socket.getOutputStream().write(message);
        writer.flush();
    }

    public String getMessage() throws IOException {
        String str = "";
            char[] buf = new char[1029];
            int c;
            if((c = reader.read(buf, 0,  1029)) > 0){
                str += String.copyValueOf(buf, 0, c);
            }
        return str;
    }

    /* get a list of messages, stop when number of chars is less than maxLength*/
    public List<String> getMessages(int maxLength) throws IOException {
        ArrayList<String> result = new ArrayList<String>();
        char[] buf = new char[1029];
        int c;
        while ((c = reader.read(buf, 0,  1029)) > 0){
            result.add(String.copyValueOf(buf, 0, c));
            if (c < maxLength) break;
        }

        return result;
    }

    public List<String> getMessages() throws IOException {
        return getMessages(1029);
    }
}
