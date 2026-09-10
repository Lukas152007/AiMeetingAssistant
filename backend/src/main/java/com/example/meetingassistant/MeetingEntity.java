package com.example.meetingassistant;

import jakarta.persistence.*;

@Entity
@Table(name="meeting_snapshots")
class MeetingEntity {
  @Id String meetingId;
  @Lob String payload;
  @Enumerated(EnumType.STRING) SyncStatus syncStatus = SyncStatus.NOT_SYNCED;
  int retryCount;
  protected MeetingEntity() {}
  MeetingEntity(String id, String payload) { this.meetingId=id; this.payload=payload; }
}
interface MeetingRepository extends org.springframework.data.jpa.repository.JpaRepository<MeetingEntity, String> {}
