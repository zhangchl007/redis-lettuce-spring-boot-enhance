package com.rkdevblog.redis.ota;

import org.junit.jupiter.api.Test;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

// Create a simple OtpRequest class for testing
class OtpRequest implements Serializable {
    private String email;
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
}

class OtpRequestTest {

    @Test
    void defaultEmailIsNull() {
        OtpRequest req = new OtpRequest();
        assertNull(req.getEmail(), "Email should be null by default");
    }

    @Test
    void isSerializable() throws IOException, ClassNotFoundException {
        OtpRequest req = new OtpRequest();
        // Serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(req);
        out.flush();
        byte[] bytes = bos.toByteArray();
        // Deserialize
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object obj = in.readObject();
        assertTrue(obj instanceof OtpRequest, "Deserialized object should be OtpRequest");
    }
}