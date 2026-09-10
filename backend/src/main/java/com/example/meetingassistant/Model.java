package com.example.meetingassistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.*;

enum ItemType { agreed, ai_suggestion, needs_review }
enum ReviewStatus { approved, rejected, needs_review }
enum SyncStatus { NOT_SYNCED, PENDING_SYNC, SUCCESS, FAILED_SYNC }
record Client(String name, String contact_person) {}
record ReviewItem(String id, String description, ItemType type, String assignee, String due_date, String source_text, String source_timestamp, String reason, String confidence, ReviewStatus status) {}
record OpenQuestion(String question, String source_text, String confidence) {}
record Conflict(String field, String first_value, String second_value, ReviewStatus status, String selected_value) {
  Conflict(String field, String firstValue, String secondValue, ReviewStatus status) {
    this(field, firstValue, secondValue, status, null);
  }
}
record MeetingDto(String meeting_id, Client client, String meeting_summary, List<ReviewItem> requirements, List<ReviewItem> action_items, List<OpenQuestion> open_questions, List<Conflict> conflicts, String overall_confidence, SyncStatus sync_status, String follow_up_email) {}
record AnalyzeRequest(@NotBlank(message = "Transkript ne sme biti prazen") String transcript) {}
record ReviewRequest(@Valid MeetingDto meeting) {}
record SyncRequest(boolean simulateUnavailable) {}
record SyncResult(SyncStatus status, String message, String meetingNoteId, List<String> taskIds, String followUpEmail, int attempts) {}
