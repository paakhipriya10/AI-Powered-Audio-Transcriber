package com.audio.transcriber;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/transcribe")
public class TranscriptionController {

    @Value("${assembly.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    public ResponseEntity<String> transcribe(@RequestParam("file") MultipartFile file) throws Exception {
        System.out.println("Injected AssemblyAI API Key: " + apiKey);

        // 1. Upload file to AssemblyAI
        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        uploadHeaders.setBearerAuth(apiKey);

        HttpEntity<byte[]> uploadEntity = new HttpEntity<>(file.getBytes(), uploadHeaders);
        String uploadUrl = "https://api.assemblyai.com/v2/upload";

        ResponseEntity<Map> uploadResponse = restTemplate.postForEntity(uploadUrl, uploadEntity, Map.class);
        String audioUrl = (String) uploadResponse.getBody().get("upload_url");

        // 2. Send for transcription
        HttpHeaders transcriptionHeaders = new HttpHeaders();
        transcriptionHeaders.setContentType(MediaType.APPLICATION_JSON);
        transcriptionHeaders.setBearerAuth(apiKey);

        Map<String, Object> transcriptionPayload = Map.of("audio_url", audioUrl);
        HttpEntity<Map<String, Object>> transcriptionEntity = new HttpEntity<>(transcriptionPayload, transcriptionHeaders);

        ResponseEntity<Map> transcriptionResponse = restTemplate.postForEntity("https://api.assemblyai.com/v2/transcript", transcriptionEntity, Map.class);
        String transcriptId = (String) transcriptionResponse.getBody().get("id");

        // 3. Poll for result (basic, blocking method — can improve later)
        String status = "processing";
        String transcriptText = "";
        while ("processing".equals(status)) {
            Thread.sleep(2000); // Wait 2 seconds

            ResponseEntity<Map> pollingResponse = restTemplate.exchange(
                "https://api.assemblyai.com/v2/transcript/" + transcriptId,
                HttpMethod.GET,
                new HttpEntity<>(transcriptionHeaders),
                Map.class
            );

            Map body = pollingResponse.getBody();
            status = (String) body.get("status");
            if ("completed".equals(status)) {
                transcriptText = (String) body.get("text");
                break;
            } else if ("error".equals(status)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Transcription failed: " + body.get("error"));
            }
        }

        return ResponseEntity.ok(transcriptText);
    }
}
