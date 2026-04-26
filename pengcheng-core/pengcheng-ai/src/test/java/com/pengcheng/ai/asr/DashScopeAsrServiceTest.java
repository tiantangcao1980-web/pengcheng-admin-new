package com.pengcheng.ai.asr;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashScopeAsrServiceTest {

    @Test
    void shouldReturnMockTranscriptWhenApiKeyMissing() {
        DashScopeAsrService service = new DashScopeAsrService();
        ReflectionTestUtils.setField(service, "apiKey", "");
        AsrRequest req = new AsrRequest();
        req.setAudioUrl("https://oss.example.com/audio/abc.mp3?token=xx");
        req.setFormat("mp3");

        AsrResponse resp = service.transcribe(req);

        assertThat(resp.getProvider()).isEqualTo("mock");
        assertThat(resp.getTranscript()).contains("abc.mp3");
        assertThat(resp.getLatencyMs()).isNotNull();
    }

    @Test
    void shouldRejectMissingAudioUrl() {
        DashScopeAsrService service = new DashScopeAsrService();
        AsrRequest req = new AsrRequest();
        assertThatThrownBy(() -> service.transcribe(req))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
