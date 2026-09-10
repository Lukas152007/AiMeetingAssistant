export type ItemType = 'agreed' | 'ai_suggestion' | 'needs_review';
export type Status = 'approved' | 'rejected' | 'needs_review';
export interface Client { name: string | null; contact_person: string | null }
export interface Item { id:string; description:string; type:ItemType; assignee:string|null; due_date:string|null; source_text:string; source_timestamp:string|null; reason:string|null; confidence:'high'|'medium'|'low'; status:Status }
export interface Conflict { field:string; first_value:string; second_value:string; status:Status }
export interface Meeting { meeting_id:string; client:Client; meeting_summary:string; requirements:Item[]; action_items:Item[]; open_questions:{question:string;source_text:string;confidence:string}[]; conflicts:Conflict[]; overall_confidence:string; sync_status:string; follow_up_email:string }
export interface SyncResult { status:string; message:string; meetingNoteId:string|null; taskIds:string[]; followUpEmail:string; attempts:number }
