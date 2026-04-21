package io.github.smartdocflow.service.web;

import io.github.smartdocflow.core.model.DocumentProfile;
import io.github.smartdocflow.sdk.SmartDocFlow;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/demo")
public class DemoParseController {
    private final SmartDocFlow smartDocFlow = new SmartDocFlow();

    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DemoParseResponse parse(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "uploaded-file";
        Path tempFile = Files.createTempFile("smartdoc-flow-", "-" + sanitizeFileName(originalFilename));
        try {
            file.transferTo(tempFile);
            DocumentProfile profile = smartDocFlow.profile(tempFile);
            String markdown = smartDocFlow.parseToMarkdown(tempFile);
            String json = smartDocFlow.parseToJson(tempFile);
            return new DemoParseResponse(
                originalFilename,
                profile.sourceType().name(),
                profile.scanned(),
                profile.multiColumn(),
                profile.tableHeavy(),
                profile.imageHeavy(),
                markdown,
                json
            );
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record DemoParseResponse(
        String fileName,
        String sourceType,
        boolean scanned,
        boolean multiColumn,
        boolean tableHeavy,
        boolean imageHeavy,
        String markdown,
        String json
    ) {
    }
}
