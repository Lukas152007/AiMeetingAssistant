import { useState } from 'react';
import type { Conflict, Item, Meeting, Status, SyncResult } from './types';

const API = 'http://localhost:8080/api';
const demos = {
  abc: `00:00:15 Ana: Pozdravljeni iz Podjetje ABC d.o.o. Želimo prenovo spletne strani.
00:01:10 Ana: Pomemben nam je obrazec za povpraševanje.
00:02:20 Ana: Potrebujemo povezavo s CRM-jem, CRM bomo še potrdili.
00:03:05 Ana: Stran mora dobro delovati tudi na telefonu.
00:04:10 Ana: Projekt bi radi zaključili do konca novembra.
00:04:35 Marko: Zaradi obsega je bolj realen december.
00:05:10 Luka: Do 12. 9. 2026 bom pripravil ponudbo.`,
  unclear: `Sestanek je bil kratek. Morda potrebujemo drugačno stran in povezavo z nekim sistemom. O rokih in odgovorni osebi se nismo dogovorili.`,
  zelenaPot: `00:00:10 Maja iz Zelena Pot d.o.o.: Potrebujemo spletne turistične rezervacije.
00:01:05 Maja: Gostje morajo izbrati termin in rezervacijo plačati na spletu.
00:02:10 Maja: Podatke o razpoložljivosti in cenah pridobimo iz sistema TravelDesk.
00:03:05 Maja: Rezervacije potrebujejo slovenski in angleški jezik.
00:04:10 Marko: Do 18. septembra pripravim predlog rezervacijskega toka.
00:04:35 Ana: Do 16. septembra uskladim opise turističnih paketov.
00:05:05 Maja: Pravila odpovedi in končne cene moramo še preveriti.`
};

async function call<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(API + path, { headers: { 'Content-Type': 'application/json' }, ...options });
  if (!response.ok) { const body = await response.json().catch(() => null); throw new Error(body?.message || body?.error || `Napaka ${response.status}`); }
  return response.json();
}

const labels: Record<string, string> = { agreed: 'DOGOVORJENO', ai_suggestion: 'AI PREDLOG', needs_review: 'POTREBEN PREGLED' };
function Badge({ value }: { value: string }) { return <span className={`badge ${value}`}>{labels[value] || value.replace('_', ' ')}</span>; }

function RequirementEditor({ item, onChange }: { item: Item; onChange: (item: Item) => void }) {
  const change = (patch: Partial<Item>) => onChange({ ...item, ...patch });
  return <article className={`item requirement ${item.confidence === 'low' ? 'attention' : ''}`}>
    <label>Opis<input aria-label="Opis zahteve" value={item.description} onChange={event => change({ description: event.target.value })} /></label>
    <div className="requirement-meta"><span className={`confidence ${item.confidence}`}>zaupanje: {item.confidence}</span><label>Status potrditve<select value={item.status} onChange={event => change({ status: event.target.value as Status })}><option value="approved">Potrjeno</option><option value="rejected">Zavrnjeno</option><option value="needs_review">Potreben pregled</option></select></label></div>
    <p className="evidence"><strong>Dokaz:</strong> „{item.source_text}“ {item.source_timestamp && <em>({item.source_timestamp})</em>}</p>
  </article>;
}

function TaskEditor({ item, onChange, employees }: { item: Item; onChange: (item: Item) => void; employees: string[] }) {
  const change = (patch: Partial<Item>) => onChange({ ...item, ...patch });
  return <article className={`item ${item.confidence === 'low' ? 'attention' : ''}`}>
    <div className="itemtop"><Badge value={item.type} /><span className={`confidence ${item.confidence}`}>zaupanje: {item.confidence}</span></div>
    <label>Opis<input aria-label="Opis naloge" value={item.description} onChange={event => change({ description: event.target.value })} /></label>
    <div className="fields"><label>Odgovorna oseba<select value={item.assignee || ''} onChange={event => change({ assignee: event.target.value || null })}><option value="">Ni določeno</option>{employees.map(name => <option key={name}>{name}</option>)}</select></label><label>Rok (YYYY-MM-DD)<input type="text" inputMode="numeric" placeholder="2026-09-12" value={item.due_date || ''} onChange={event => change({ due_date: event.target.value || null })} /></label><label>Odločitev<select value={item.status} onChange={event => change({ status: event.target.value as Status })}><option value="approved">Potrjeno</option><option value="rejected">Zavrnjeno</option><option value="needs_review">Potreben pregled</option></select></label></div>
    <p className="evidence"><strong>Dokaz:</strong> „{item.source_text}“ {item.source_timestamp && <em>({item.source_timestamp})</em>}</p>{item.reason && <p className="reason">Zakaj predlog: {item.reason}</p>}
  </article>;
}

export default function App() {
  const [transcript, setTranscript] = useState(''); const [meeting, setMeeting] = useState<Meeting | null>(null); const [employees, setEmployees] = useState<string[]>(['Luka', 'Ana', 'Marko']); const [clients, setClients] = useState<{ name: string; contact: string }[]>([]); const [busy, setBusy] = useState(false); const [simulate, setSimulate] = useState(false); const [result, setResult] = useState<SyncResult | null>(null); const [error, setError] = useState(''); const [fallback, setFallback] = useState(''); const [copied, setCopied] = useState(false);
  const loadDemo = (value: string) => { setTranscript(value); setMeeting(null); setResult(null); setFallback(''); setError(''); };
  const analyze = async () => { try { setBusy(true); setError(''); setResult(null); setFallback(''); const analyzed = await call<Meeting>('/meetings/analyze', { method: 'POST', body: JSON.stringify({ transcript }) }); setMeeting(analyzed); const [employeeData, clientData] = await Promise.all([call<{ name: string }[]>('/internal/employees'), call<{ name: string; contact: string }[]>('/internal/clients')]); setEmployees(employeeData.map(employee => employee.name)); setClients(clientData); } catch (exception) { setError(exception instanceof Error ? exception.message : 'Analiza ni uspela'); } finally { setBusy(false); } };
  const updateItem = (kind: 'requirements' | 'action_items', updated: Item) => setMeeting(current => current && ({ ...current, [kind]: current[kind].map(item => item.id === updated.id ? updated : item) }));
  const saveReview = async () => { if (!meeting) return; const saved = await call<Meeting>(`/meetings/${meeting.meeting_id}/review`, { method: 'PUT', body: JSON.stringify({ meeting }) }); setMeeting(saved); return saved; };
  const sync = async (retry = false) => { if (!meeting) return; try { setBusy(true); setError(''); await saveReview(); const synced = await call<SyncResult>(`/meetings/${meeting.meeting_id}/${retry ? 'retry' : 'sync'}`, { method: 'POST', body: JSON.stringify({ simulateUnavailable: simulate }) }); setResult(synced); if (synced.status !== 'SUCCESS') setFallback(await fetch(`${API}/meetings/${meeting.meeting_id}/manual-fallback`).then(response => response.text())); } catch (exception) { setError(exception instanceof Error ? exception.message : 'Sinhronizacija ni uspela'); } finally { setBusy(false); } };
  const selectDeadline = (index: number, selectedValue: string) => setMeeting(current => current && ({ ...current, conflicts: current.conflicts.map((conflict, conflictIndex) => conflictIndex === index ? { ...conflict, selected_value: selectedValue || null, status: selectedValue ? 'approved' : 'needs_review' } : conflict) }));
  const copyFallback = async () => { if (!fallback) return; await navigator.clipboard.writeText(fallback); setCopied(true); window.setTimeout(() => setCopied(false), 1800); };
  return <main><header><div><p className="eyebrow">AI MEETING ASSISTANT</p><h1>Od transkripta do varnega naslednjega koraka</h1><p>AI pripravi predlog. Zaposleni potrdi poslovno pomembne podatke pred sinhronizacijo.</p></div><span className="shield">Človeška kontrola</span></header>
    <section className="card step"><h2><span>1</span> Vnos transkripta</h2><p>Izberite demo scenarij ali prilepite svoj transkript. Pred potrditvijo se v poslovni sistem ne zapiše nič.</p><div className="demo-actions"><button className="secondary" onClick={() => loadDemo(demos.abc)}>Naloži: Podjetje ABC</button><button className="secondary" onClick={() => loadDemo(demos.unclear)}>Naloži: nejasen primer</button><button className="secondary" onClick={() => loadDemo(demos.zelenaPot)}>Naloži: Zelena Pot</button></div><textarea value={transcript} onChange={event => setTranscript(event.target.value)} placeholder="Prilepite transkript sestanka ..." /><div className="actions"><button disabled={!transcript.trim() || busy} onClick={analyze}>{busy ? 'Analiziram ...' : 'Analiziraj transkript'}</button></div></section>
    {error && <div className="alert error">{error}</div>}
    {meeting && <><section className="card step"><h2><span>2</span> Pregled AI-rezultata <Badge value={meeting.overall_confidence} /></h2><div className="grid"><div><h3>Podatki o stranki</h3><label>Izbrani klient<select value={meeting.client.name || ''} onChange={event => { const client = clients.find(item => item.name === event.target.value); setMeeting({ ...meeting, client: { name: event.target.value || null, contact_person: client?.contact || null } }); }}><option value="">Ročno izberite klienta</option>{clients.map(client => <option key={client.name} value={client.name}>{client.name}</option>)}</select></label><p>{meeting.client.contact_person || 'Kontakt ni določen'}</p></div><div><h3>Povzetek sestanka</h3><textarea className="summary" value={meeting.meeting_summary} onChange={event => setMeeting({ ...meeting, meeting_summary: event.target.value })} /></div></div>
      <h3>Zahteve stranke</h3>{meeting.requirements.map(item => <RequirementEditor key={item.id} item={item} onChange={updated => updateItem('requirements', updated)} />)}
      <h3>Dogovorjene naloge in AI-predlogi</h3>{meeting.action_items.map(item => <TaskEditor key={item.id} item={item} employees={employees} onChange={updated => updateItem('action_items', updated)} />)}
      <h3>Odprta vprašanja</h3>{meeting.open_questions.map(question => <blockquote key={question.question}>“{question.question}” <small>Dokaz: {question.source_text}; zaupanje: {question.confidence}</small></blockquote>)}
      {meeting.conflicts.length > 0 && <><h3>Konflikt glede roka</h3>{meeting.conflicts.map((conflict: Conflict, index) => <div className="conflict" key={conflict.field}><strong>Omenjeni roki:</strong> „{conflict.first_value}“ ↔ „{conflict.second_value}“<label>Potrjeni rok<select value={conflict.selected_value || ''} onChange={event => selectDeadline(index, event.target.value)}><option value="">Izberite možnost</option><option value="konec novembra">konec novembra</option><option value="december">december</option><option value="rok ni potrjen">rok ni potrjen</option></select></label></div>)}</>}
      <h3>Osnutek follow-up e-maila</h3><textarea className="email" readOnly value={meeting.follow_up_email} /></section>
      <section className="card step"><h2><span>3</span> Potrditev in sinhronizacija</h2><label className="check"><input type="checkbox" checked={simulate} onChange={event => setSimulate(event.target.checked)} /> Simuliraj nedosegljiv interni API</label><p>V sinhronizacijo se pošljejo le potrjene postavke. AI-predlog se nikoli ne spremeni v nalogo brez vaše potrditve.</p><div className="actions"><button onClick={() => sync(false)} disabled={busy}>Potrdi in sinhroniziraj</button>{result && result.status !== 'SUCCESS' && <button className="secondary" onClick={() => sync(true)} disabled={busy}>Poskusi ponovno</button>}{fallback && <button className="secondary" onClick={copyFallback}>Kopiraj ročni povzetek</button>}</div>{copied && <p className="copy-confirm">Ročni povzetek je kopiran.</p>}{result && <div className={`alert ${result.status === 'SUCCESS' ? 'success' : 'warning'}`}><strong>{result.status}</strong> — {result.message}{result.meetingNoteId && <div>Ustvarjen zapisnik: {result.meetingNoteId}; naloge: {result.taskIds.join(', ') || 'brez novih nalog'}</div>}</div>}{fallback && <div className="fallback"><h3>Ročni fallback</h3><p>Vsebuje samo potrjeni povzetek, potrjene naloge, odgovorne osebe, roke in potrjeni e-mail.</p><textarea readOnly value={fallback} /></div>}</section></>}
  </main>;
}
