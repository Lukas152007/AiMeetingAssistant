package com.example.meetingassistant;

interface TranscriptAnalysisService { MeetingDto analyze(String transcript); }

/** Extension seam for a real provider. Keep it behind a profile until an audited API adapter is supplied. */
@org.springframework.context.annotation.Profile("ai")
@org.springframework.stereotype.Service
class AiTranscriptAnalysisService implements TranscriptAnalysisService {
  public MeetingDto analyze(String transcript) {
    throw new UnsupportedOperationException("AI adapter ni konfiguriran; uporabite DEMO_MODE=true.");
  }
}
