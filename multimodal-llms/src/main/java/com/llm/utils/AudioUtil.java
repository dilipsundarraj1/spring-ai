package com.llm.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;

public class AudioUtil {
    private static final Logger log = LoggerFactory.getLogger(AudioUtil.class);

    public static void writeMP3ToFile(byte[] mp3Bytes, String filePath) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(mp3Bytes);
            log.info("MP3 file written to: " + filePath);
        } catch (IOException e) {
            log.error("Error writing MP3 file: {}  " , e.getMessage(), e);
        }
    }
}
