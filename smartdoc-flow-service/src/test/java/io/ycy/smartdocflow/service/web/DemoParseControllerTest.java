package io.ycy.smartdocflow.service.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DemoParseControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void parsesUploadedFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "sample.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "财务摘要\n\n整体稳定。".getBytes()
        );

        mockMvc.perform(multipart("/api/demo/parse").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fileName").value("sample.txt"))
            .andExpect(jsonPath("$.sourceType").value("UNKNOWN"))
            .andExpect(jsonPath("$.markdown").isString())
            .andExpect(jsonPath("$.json").isString());
    }

    @Test
    void rejectsEmptyUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "empty.txt",
            MediaType.TEXT_PLAIN_VALUE,
            new byte[0]
        );

        mockMvc.perform(multipart("/api/demo/parse").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("上传文件不能为空"));
    }
}
