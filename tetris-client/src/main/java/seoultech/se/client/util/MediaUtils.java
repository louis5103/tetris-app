package seoultech.se.client.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javafx.scene.media.Media;

public class MediaUtils {

    /**
     * 리소스로부터 Media 객체를 생성합니다.
     * "nested" 프로토콜(JAR 내부 파일) 문제를 해결하기 위해 임시 파일로 복사하여 로드합니다.
     * 
     * @param resourceUrl 리소스 URL
     * @return 생성된 Media 객체
     */
    public static Media loadMedia(URL resourceUrl) {
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Resource URL cannot be null");
        }

        String protocol = resourceUrl.getProtocol();
        
        // JAR 내부 파일(nested, jar)인 경우 임시 파일로 추출
        if ("nested".equals(protocol) || "jar".equals(protocol)) {
            try {
                File tempFile = File.createTempFile("tetris_bgm_", ".mp3");
                tempFile.deleteOnExit();
                
                try (InputStream is = resourceUrl.openStream();
                     OutputStream os = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }
                
                return new Media(tempFile.toURI().toString());
            } catch (Exception e) {
                System.err.println("❌ Failed to extract media to temp file: " + e.getMessage());
                e.printStackTrace();
                // 실패 시 원본 시도 (예외 발생 가능성 높음)
                return new Media(resourceUrl.toString());
            }
        } else {
            // 일반 파일 시스템인 경우 그대로 사용
            return new Media(resourceUrl.toString());
        }
    }
}
