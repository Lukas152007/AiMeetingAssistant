package com.example.meetingassistant;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import java.util.List;

/** Deterministic, offline analyzer. It deliberately distinguishes evidence from suggestions. */
@Service @Primary
class DemoTranscriptAnalysisService implements TranscriptAnalysisService {
  public MeetingDto analyze(String transcript) {
    String id = "meeting-" + java.time.LocalDate.now() + "-abc-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    boolean knownExample = transcript.toLowerCase().contains("podjetje abc");
    Client client = knownExample ? new Client("Podjetje ABC d.o.o.", "Ana Novak") : new Client(null, null);
    String summary = knownExample ? "Stranka želi prenovo spletne strani z obrazcem, CRM integracijo in mobilno prilagoditvijo. Končni rok projekta ni dokončno potrjen." : "Demo analiza: pred sinhronizacijo ročno preverite klienta, odgovorne osebe in predloge.";
    List<ReviewItem> requirements = knownExample ? List.of(
      item("req-1", "Obrazec za povpraševanje", ItemType.agreed, null, null, "Pomemben nam je obrazec za povpraševanje.", "00:01:10–00:01:18", null, "high", ReviewStatus.approved),
      item("req-2", "CRM integracija", ItemType.agreed, null, null, "Potrebujemo povezavo s CRM-jem.", "00:02:20–00:02:28", null, "high", ReviewStatus.approved),
      item("req-3", "Mobilna prilagoditev", ItemType.agreed, null, null, "Stran mora dobro delovati tudi na telefonu.", "00:03:05–00:03:12", null, "medium", ReviewStatus.approved)
    ) : List.of(item("req-demo", "Ročno preverite zahteve iz transkripta", ItemType.needs_review, null, null, transcript.substring(0, Math.min(150, transcript.length())), null, "Demo način ne interpretira poljubnega besedila kot dejstvo.", "low", ReviewStatus.needs_review));
    List<ReviewItem> tasks = knownExample ? List.of(
      item("task-1", "Priprava ponudbe", ItemType.agreed, "Luka", java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.FRIDAY)).toString(), "Luka bo do petka pripravil ponudbo.", "00:05:10–00:05:18", null, "high", ReviewStatus.approved),
      item("task-2", "Preveriti tehnične zahteve za CRM integracijo", ItemType.ai_suggestion, null, null, "Potrebujemo povezavo s CRM-jem.", "00:02:20–00:02:28", "CRM integracija je zahteva, vendar konkreten naslednji korak ni bil dogovorjen.", "low", ReviewStatus.needs_review)
    ) : List.of();
    List<OpenQuestion> questions = knownExample ? List.of(new OpenQuestion("Kateri CRM-sistem uporablja stranka?", "CRM bomo še potrdili.", "medium")) : List.of();
    List<Conflict> conflicts = knownExample ? List.of(new Conflict("project_deadline", "konec novembra", "december", ReviewStatus.needs_review)) : List.of();
    MeetingDto draft = new MeetingDto(id, client, summary, requirements, tasks, questions, conflicts, "medium", SyncStatus.NOT_SYNCED, "");
    return withEmail(draft);
  }
  private ReviewItem item(String id,String description,ItemType type,String assignee,String due,String source,String timestamp,String reason,String confidence,ReviewStatus status) { return new ReviewItem(id,description,type,assignee,due,source,timestamp,reason,confidence,status); }
  static MeetingDto withEmail(MeetingDto meeting) { return MeetingService.composeEmail(meeting); }
}
