package com.kpfu.itis;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class MystemParser {

    public String stem(String word) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(System.getProperty("user.home") + "/mystem",
                "-ln", "-e", "utf-8",
                "--format", "text");
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String mystemWord;

        try (OutputStream in = process.getOutputStream();
             InputStream out = process.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(out, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(in, StandardCharsets.UTF_8))) {
            writer.write(word + "\n");
            writer.flush();
            mystemWord = reader.readLine();
            System.out.println(mystemWord);
        }
        return mystemWord;
    }
}