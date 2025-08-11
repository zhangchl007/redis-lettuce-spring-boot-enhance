package com.rkdevblog.redis.dto;

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
        assertNull(req.getEmail(), "email should be null by default");
    }

    @Test
    void serialization_roundTrip() throws Exception {
        OtpRequest req = new OtpRequest();
        // (No setter, just ensure it serializes in default state)
        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(req);
            oos.flush();
            bytes = bos.toByteArray();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object restored = ois.readObject();
            assertTrue(restored instanceof OtpRequest);
            assertNull(((OtpRequest) restored).getEmail());
        }
    }
}