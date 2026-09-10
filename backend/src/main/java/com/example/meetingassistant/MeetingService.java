package com.example.meetingassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
class MeetingService {
  private final MeetingRepository meetings; private final TranscriptAnalysisService analyzer; private final ObjectMapper mapper; private final InternalBusinessService internal;
  MeetingService(MeetingRepository r, TranscriptAnalysisService a, ObjectMapper m, InternalBusinessService i){meetings=r;analyzer=a;mapper=m;internal=i;}
  @Transactional MeetingDto analyze(String transcript){ MeetingDto result=analyzer.analyze(transcript); save(result,0); return result; }
  MeetingDto get(String id){ return read(id); }
  @Transactional MeetingDto review(String id, MeetingDto incoming){
    if(incoming==null || !id.equals(incoming.meeting_id())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ID sestanka se ne ujema.");
    MeetingEntity e=entity(id); e.payload=write(DemoTranscriptAnalysisService.withEmail(incoming)); e.syncStatus=SyncStatus.NOT_SYNCED; e.retryCount=0; return readPayload(e);
  }
  @Transactional SyncResult sync(String id, boolean unavailable){
    MeetingEntity e=entity(id); MeetingDto m=readPayload(e); validateForSync(m); e.payload=write(DemoTranscriptAnalysisService.withEmail(m));
    if(unavailable) { e.syncStatus=SyncStatus.PENDING_SYNC; e.payload=write(withSyncStatus(readPayload(e),SyncStatus.PENDING_SYNC)); e.retryCount=1; return new SyncResult(SyncStatus.PENDING_SYNC,"Interni API ni dosegljiv. Potrjena različica je varno shranjena; uporabite Poskusi ponovno.",null,List.of(),readPayload(e).follow_up_email(),1); }
    SyncResult r=internal.createApprovedRecords(readPayload(e)); e.syncStatus=SyncStatus.SUCCESS; e.payload=write(withSyncStatus(readPayload(e),SyncStatus.SUCCESS)); e.retryCount=0; return r;
  }
  @Transactional SyncResult retry(String id, boolean unavailable){
    MeetingEntity e=entity(id); MeetingDto m=readPayload(e); validateForSync(m);
    if(!unavailable) { SyncResult r=internal.createApprovedRecords(m); e.syncStatus=SyncStatus.SUCCESS; e.payload=write(withSyncStatus(m,SyncStatus.SUCCESS)); e.retryCount=0; return r; }
    e.syncStatus=SyncStatus.PENDING_SYNC; e.payload=write(withSyncStatus(m,SyncStatus.PENDING_SYNC));
    for(int attempt=1;attempt<=3;attempt++){ e.retryCount=attempt; try { Thread.sleep(attempt*80L); } catch(InterruptedException x){Thread.currentThread().interrupt();} }
    e.syncStatus=SyncStatus.FAILED_SYNC; e.payload=write(withSyncStatus(readPayload(e),SyncStatus.FAILED_SYNC));
    return new SyncResult(SyncStatus.FAILED_SYNC,"Interni API po treh poskusih ni dosegljiv. Podatki ostanejo shranjeni.",null,List.of(),m.follow_up_email(),3);
  }
  String fallback(String id){ MeetingDto m=read(id); return "POTRJEN POVZETEK\n"+m.meeting_summary()+"\n\nPOTRJENE NALOGE\n"+m.action_items().stream().filter(x->x.status()==ReviewStatus.approved).map(x->"- "+x.description()+" | odgovorna oseba: "+x.assignee()+(x.due_date()!=null?" | rok: "+x.due_date():"")).reduce("",(a,b)->a+b+"\n")+"\nE-MAIL\n"+m.follow_up_email(); }
  private void validateForSync(MeetingDto m){
    if(m.client()==null || m.client().name()==null || !internal.clientExists(m.client().name())) fail("Izberite obstoječega klienta.");
    if(m.conflicts().stream().anyMatch(c->c.status()==ReviewStatus.needs_review || c.selected_value()==null || c.selected_value().isBlank()) || m.requirements().stream().anyMatch(i->i.status()==ReviewStatus.needs_review) || m.action_items().stream().anyMatch(i->i.status()==ReviewStatus.needs_review)) fail("Vse nejasne ali konfliktne postavke morajo biti obravnavane.");
    for(ReviewItem item:m.action_items()) if(item.status()==ReviewStatus.approved){
      if(item.description()==null||item.description().isBlank()) fail("Potrjena naloga potrebuje naslov.");
      if(!internal.employeeExists(item.assignee())) fail("Potrjena naloga mora imeti obstoječo odgovorno osebo.");
      if(item.due_date()!=null&&!item.due_date().isBlank()) try{LocalDate.parse(item.due_date());}catch(DateTimeParseException ex){fail("Rok mora biti veljaven datum YYYY-MM-DD.");}
    }
  }
  private void fail(String m){throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,m);}
  private MeetingEntity entity(String id){return meetings.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Sestanek ni najden."));}
  private MeetingDto read(String id){return readPayload(entity(id));}
  private MeetingDto readPayload(MeetingEntity e){try{return mapper.readValue(e.payload,MeetingDto.class);}catch(Exception x){throw new IllegalStateException("Poškodovan shranjen predlog",x);}}
  private void save(MeetingDto m,int retries){ meetings.save(new MeetingEntity(m.meeting_id(),write(m))); }
  private String write(MeetingDto m){try{return mapper.writeValueAsString(m);}catch(Exception x){throw new IllegalStateException(x);}}
  private MeetingDto withSyncStatus(MeetingDto m, SyncStatus status){return new MeetingDto(m.meeting_id(),m.client(),m.meeting_summary(),m.requirements(),m.action_items(),m.open_questions(),m.conflicts(),m.overall_confidence(),status,m.follow_up_email());}
  static MeetingDto composeEmail(MeetingDto m){
    String requirements=m.requirements().stream().filter(i->i.status()==ReviewStatus.approved).map(i->"- "+i.description()).reduce("",(a,b)->a+b+"\n");
    String tasks=m.action_items().stream().filter(i->i.status()==ReviewStatus.approved).map(i->"- "+i.description()+(i.assignee()!=null?" — "+i.assignee():"")+(i.due_date()!=null?", rok "+i.due_date():"")).reduce("",(a,b)->a+b+"\n");
    String email="Zadeva: Povzetek sestanka in naslednji koraki\n\nPozdravljeni,\n\nhvala za današnji sestanek. V nadaljevanju pošiljamo kratek povzetek dogovorjenega.\n\nKljučne zahteve:\n"+requirements+"\nNaslednji koraki:\n"+tasks+"\nProsimo, sporočite nam, če smo kateri del dogovora napačno razumeli.\n\nLep pozdrav,\nEkipa";
    return new MeetingDto(m.meeting_id(),m.client(),m.meeting_summary(),m.requirements(),m.action_items(),m.open_questions(),m.conflicts(),m.overall_confidence(),m.sync_status(),email);
  }
}
