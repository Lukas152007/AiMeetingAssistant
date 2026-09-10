package com.example.meetingassistant;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api") @CrossOrigin(origins="http://localhost:5173")
class ApiController {
  private final MeetingService meetings; private final InternalClientRepository clients; private final EmployeeRepository employees; private final InternalMeetingNoteRepository notes; private final InternalTaskRepository tasks;
  ApiController(MeetingService m,InternalClientRepository c,EmployeeRepository e,InternalMeetingNoteRepository n,InternalTaskRepository t){meetings=m;clients=c;employees=e;notes=n;tasks=t;}
  @PostMapping("/meetings/analyze") ResponseEntity<MeetingDto> analyze(@Valid @RequestBody AnalyzeRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(meetings.analyze(r.transcript()));}
  @GetMapping("/meetings/{id}") MeetingDto meeting(@PathVariable String id){return meetings.get(id);}
  @PutMapping("/meetings/{id}/review") MeetingDto review(@PathVariable String id,@Valid @RequestBody ReviewRequest r){return meetings.review(id,r.meeting());}
  @PostMapping("/meetings/{id}/sync") SyncResult sync(@PathVariable String id,@RequestBody(required=false) SyncRequest r){return meetings.sync(id,r!=null&&r.simulateUnavailable());}
  @PostMapping("/meetings/{id}/retry") SyncResult retry(@PathVariable String id,@RequestBody(required=false) SyncRequest r){return meetings.retry(id,r!=null&&r.simulateUnavailable());}
  @GetMapping(value="/meetings/{id}/manual-fallback",produces=MediaType.TEXT_PLAIN_VALUE) String fallback(@PathVariable String id){return meetings.fallback(id);}
  @GetMapping("/internal/clients") List<InternalClient> clients(@RequestParam(required=false,defaultValue="") String name){return clients.findByNameContainingIgnoreCase(name);}
  @GetMapping("/internal/employees") List<Employee> employees(){return employees.findAll();}
  @PostMapping("/internal/meeting-notes") ResponseEntity<?> createNote(@RequestParam(defaultValue="false") boolean simulateUnavailable){return simulateUnavailable?ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message","Simuliran nedosegljiv interni API.")):ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("message","Mock zapisnik ustvarja integracijski modul prek sinhronizacije."));}
  @PostMapping("/internal/tasks") ResponseEntity<?> createTask(@RequestParam(defaultValue="false") boolean simulateUnavailable){return simulateUnavailable?ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message","Simuliran nedosegljiv interni API.")):ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("message","Mock naloge ustvarja integracijski modul prek sinhronizacije."));}
  @GetMapping("/internal/meeting-notes") List<InternalMeetingNote> notes(){return notes.findAll();}
  @GetMapping("/internal/tasks") List<InternalTask> tasks(){return tasks.findAll();}
}
