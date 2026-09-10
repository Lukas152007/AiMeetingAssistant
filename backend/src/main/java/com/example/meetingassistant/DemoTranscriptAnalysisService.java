package com.example.meetingassistant;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import java.util.List;

/** Deterministic, offline analyzer. It deliberately distinguishes evidence from suggestions. */
@Service @Primary
class DemoTranscriptAnalysisService implements TranscriptAnalysisService {
  public MeetingDto analyze(String transcript) {
    String id = "meeting-" + java.time.LocalDate.now() + "-demo-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    String normalized = transcript.toLowerCase();
    if (normalized.contains("zelena pot")) return zelenaPot(id);
    if (!normalized.contains("podjetje abc")) return unclear(id, transcript);
    return abc(id);
  }
  private MeetingDto abc(String id) {
    Client client = new Client("Podjetje ABC d.o.o.", "Ana Novak");
    String summary = "Stranka želi prenovo spletne strani z obrazcem, CRM integracijo in mobilno prilagoditvijo. Končni rok projekta ni dokončno potrjen.";
    List<ReviewItem> requirements = List.of(
      item("req-1", "Obrazec za povpraševanje", ItemType.agreed, null, null, "Pomemben nam je obrazec za povpraševanje.", "00:01:10–00:01:18", null, "high", ReviewStatus.approved),
      item("req-2", "CRM integracija", ItemType.agreed, null, null, "Potrebujemo povezavo s CRM-jem.", "00:02:20–00:02:28", null, "high", ReviewStatus.approved),
      item("req-3", "Mobilna prilagoditev", ItemType.agreed, null, null, "Stran mora dobro delovati tudi na telefonu.", "00:03:05–00:03:12", null, "medium", ReviewStatus.approved)
    );
    List<ReviewItem> tasks = List.of(
      item("task-1", "Priprava ponudbe", ItemType.agreed, "Luka", "2026-09-12", "Luka bo do petka pripravil ponudbo.", "00:05:10–00:05:18", null, "high", ReviewStatus.approved),
      item("task-2", "Preveriti tehnične zahteve za CRM integracijo", ItemType.ai_suggestion, null, null, "Potrebujemo povezavo s CRM-jem.", "00:02:20–00:02:28", "CRM integracija je zahteva, vendar konkreten naslednji korak ni bil dogovorjen.", "low", ReviewStatus.needs_review)
    );
    List<OpenQuestion> questions = List.of(new OpenQuestion("Kateri CRM-sistem uporablja stranka?", "CRM bomo še potrdili.", "medium"));
    List<Conflict> conflicts = List.of(new Conflict("project_deadline", "konec novembra", "december", ReviewStatus.needs_review));
    MeetingDto draft = new MeetingDto(id, client, summary, requirements, tasks, questions, conflicts, "medium", SyncStatus.NOT_SYNCED, "");
    return withEmail(draft);
  }
  private MeetingDto unclear(String id, String transcript) {
    String excerpt = transcript.isBlank() ? "Brez uporabnega besedila." : transcript.substring(0, Math.min(150, transcript.length()));
    MeetingDto draft = new MeetingDto(id, new Client(null, null), "Nejasen demo scenarij: klient, zahteve in naslednji koraki niso dovolj jasno navedeni.",
      List.of(item("req-unclear-1", "Nejasno omenjena možna sprememba", ItemType.needs_review, null, null, excerpt, null, "Ni dovolj dokazov za potrditev zahteve.", "low", ReviewStatus.needs_review)),
      List.of(item("task-unclear-1", "Ročno preveriti zapis sestanka", ItemType.needs_review, null, null, excerpt, null, "Odgovorna oseba in rok nista navedena.", "low", ReviewStatus.needs_review)),
      List.of(new OpenQuestion("Kdo je klient in kaj je bilo dejansko dogovorjeno?", excerpt, "low")), List.of(), "low", SyncStatus.NOT_SYNCED, "");
    return withEmail(draft);
  }
  private MeetingDto zelenaPot(String id) {
    MeetingDto draft = new MeetingDto(id, new Client("Zelena Pot d.o.o.", "Maja Kovač"), "Zelena Pot želi spletne turistične rezervacije s povezavo TravelDesk. Pred objavo je treba potrditi pravila odpovedi in razpoložljivost API-ja.",
      List.of(
        item("req-zp-1", "Spletne turistične rezervacije", ItemType.agreed, null, null, "Gostje morajo izbrati termin in rezervacijo plačati na spletu.", "00:01:05–00:01:16", null, "high", ReviewStatus.approved),
        item("req-zp-2", "Integracija TravelDesk", ItemType.agreed, null, null, "Podatke o razpoložljivosti in cenah pridobimo iz sistema TravelDesk.", "00:02:10–00:02:23", null, "high", ReviewStatus.approved),
        item("req-zp-3", "Slovenski in angleški vmesnik", ItemType.agreed, null, null, "Rezervacije potrebujejo slovenski in angleški jezik.", "00:03:05–00:03:13", null, "medium", ReviewStatus.approved)
      ),
      List.of(
        item("task-zp-1", "Pripraviti predlog rezervacijskega toka", ItemType.agreed, "Marko", "2026-09-18", "Marko do 18. septembra pripravi predlog rezervacijskega toka.", "00:04:10–00:04:20", null, "high", ReviewStatus.approved),
        item("task-zp-2", "Uskladiti turistične vsebine", ItemType.agreed, "Ana", "2026-09-16", "Ana do 16. septembra uskladi opise turističnih paketov.", "00:04:35–00:04:45", null, "high", ReviewStatus.approved),
        item("task-zp-3", "Preveriti omejitve TravelDesk API-ja", ItemType.ai_suggestion, null, null, "Podatke o razpoložljivosti in cenah pridobimo iz sistema TravelDesk.", "00:02:10–00:02:23", "Integracija je potrjena, tehnično preverjanje pa ni bilo izrecno dogovorjeno.", "low", ReviewStatus.needs_review)
      ),
      List.of(new OpenQuestion("Ali TravelDesk podpira odpoved rezervacije in vračila kupnine?", "Pravila odpovedi še preverimo.", "medium"), new OpenQuestion("Kdo potrdi končne cene turističnih paketov?", "Cene moramo interno še uskladiti.", "medium")),
      List.of(), "medium", SyncStatus.NOT_SYNCED, "");
    return withEmail(draft);
  }
  private ReviewItem item(String id,String description,ItemType type,String assignee,String due,String source,String timestamp,String reason,String confidence,ReviewStatus status) { return new ReviewItem(id,description,type,assignee,due,source,timestamp,reason,confidence,status); }
  static MeetingDto withEmail(MeetingDto meeting) { return MeetingService.composeEmail(meeting); }
}
