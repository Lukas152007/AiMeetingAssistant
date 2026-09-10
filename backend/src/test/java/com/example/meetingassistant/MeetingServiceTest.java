package com.example.meetingassistant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MeetingServiceTest {
  @Autowired MeetingService service;
  @Test void unapprovedAiSuggestionIsNotSynced() {
    MeetingDto draft=service.analyze("Podjetje ABC d.o.o. želi prenovo");
    MeetingDto reviewed = new MeetingDto(draft.meeting_id(), draft.client(), draft.meeting_summary(), draft.requirements(), draft.action_items().stream().map(i -> new ReviewItem(i.id(),i.description(),i.type(),i.assignee(),i.due_date(),i.source_text(),i.source_timestamp(),i.reason(),i.confidence(),i.type()==ItemType.ai_suggestion?ReviewStatus.rejected:i.status())).toList(), draft.open_questions(),
      draft.conflicts().stream().map(c -> new Conflict(c.field(),c.first_value(),c.second_value(),ReviewStatus.rejected)).toList(), draft.overall_confidence(), draft.sync_status(), draft.follow_up_email());
    service.review(draft.meeting_id(), reviewed);
    assertDoesNotThrow(() -> service.sync(draft.meeting_id(),false));
  }
  @Test void unresolvedConflictBlocksSync() {
    MeetingDto draft=service.analyze("Podjetje ABC d.o.o.");
    assertThrows(ResponseStatusException.class, () -> service.sync(draft.meeting_id(),false));
  }
}
