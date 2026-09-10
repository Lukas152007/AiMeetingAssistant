package com.example.meetingassistant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
class InternalBusinessService {
  private final InternalClientRepository clients; private final EmployeeRepository employees; private final InternalMeetingNoteRepository notes; private final InternalTaskRepository tasks;
  InternalBusinessService(InternalClientRepository c, EmployeeRepository e, InternalMeetingNoteRepository n, InternalTaskRepository t){clients=c;employees=e;notes=n;tasks=t;}
  boolean clientExists(String name){ return name != null && clients.existsByName(name); }
  boolean employeeExists(String name){ return name != null && employees.existsByName(name); }
  @Transactional SyncResult createApprovedRecords(MeetingDto m) {
    InternalMeetingNote note = notes.findByMeetingId(m.meeting_id()).orElseGet(() -> notes.save(new InternalMeetingNote(m.meeting_id(),m.client().name(),m.meeting_summary())));
    List<String> ids=new ArrayList<>();
    for (ReviewItem task: m.action_items()) if(task.status()==ReviewStatus.approved) {
      String key=m.meeting_id()+":"+task.id();
      InternalTask saved=tasks.findByIdempotencyKey(key).orElseGet(() -> tasks.save(new InternalTask(key,m.meeting_id(),task.description(),task.assignee(),task.due_date())));
      ids.add("task-"+saved.getId());
    }
    return new SyncResult(SyncStatus.SUCCESS,"Sinhronizacija je uspela.","note-"+note.getId(),ids,m.follow_up_email(),1);
  }
}
