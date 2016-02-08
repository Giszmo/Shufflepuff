package com.shuffle.sim;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Implementation using a mock network for testing purposes.
 *
 * Created by Daniel Krawisz on 2/3/16.
 */
public class Player {
    Adversary adversary;

    private Player() {}

    static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static void main(String args) {
        String fileName = "config.json";

        try {
            String content = readFile(fileName, Charset.defaultCharset());
        } catch (IOException e) {
            return;
        }


    }
}
