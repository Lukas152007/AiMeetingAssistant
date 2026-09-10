# AI Meeting Assistant

Lokalni prototip, ki iz transkripta sestanka pripravi strukturiran predlog. AI pomaga pri razvrščanju informacij, zaposleni pa pred sinhronizacijo vedno pregleda in potrdi poslovno pomembne podatke.

## Glavne funkcionalnosti

- trije demo scenariji: **Podjetje ABC d.o.o.**, nejasen primer z `low` zaupanjem in **Zelena Pot d.o.o.** s turističnimi rezervacijami ter integracijo TravelDesk;
- strukturirane zahteve, naloge, odprta vprašanja, dokazi iz transkripta in stopnja zaupanja;
- ločitev med dogovorjeno nalogo, AI-predlogom in postavko, ki potrebuje pregled;
- urejanje povzetka, zahtev, odgovorne osebe, roka in statusa potrditve;
- jasna izbira za konflikt roka: **konec novembra**, **december** ali **rok ni potrjen**;
- mock interni poslovni API z odjemalci, zaposlenimi, zapisniki in nalogami v H2;
- idempotentna sinhronizacija, retry, `PENDING_SYNC`, `FAILED_SYNC` in kopirljiv ročni fallback;
- follow-up e-mail, ustvarjen izključno iz potrjenih podatkov.

## Arhitektura

```text
React + Vite ── REST ──> Spring Boot
                         ├─ DemoTranscriptAnalysisService
                         ├─ MeetingService: pregled, validacija, retry, e-mail
                         ├─ InternalBusinessService: mock integracija
                         └─ H2 / JPA: predlogi in interni podatki
```

`TranscriptAnalysisService` je razširitvena točka za pravi AI ponudnik. Privzeti `DEMO_MODE=true` je povsem lokalen in ne potrebuje API-ključa.

## Lokalni zagon

Predpogoji: Java 21+, Maven 3.9+ in Node.js 20+.

V prvem terminalu:

```powershell
cd backend
mvn spring-boot:run
```

V drugem terminalu:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Odprite `http://localhost:5173`. Backend deluje na `http://localhost:8080`.

H2 konzola je na `http://localhost:8080/h2-console`:

```text
JDBC URL: jdbc:h2:mem:meetingassistant;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
User: sa
Password: prazno
```

## Demo scenariji

1. **Podjetje ABC d.o.o.** — obrazec za povpraševanje, CRM integracija, mobilna prilagoditev, Luka pripravi ponudbo do `2026-09-12`, AI-predlog in konflikt roka.
2. **Nejasen primer** — varni fallback: neznan klient, `null` vrednosti, `low` zaupanje in status `needs_review`. Sinhronizacija je blokirana do ročnega pregleda.
3. **Zelena Pot d.o.o.** — spletne turistične rezervacije, TravelDesk, večjezični vmesnik, naloge za Marka in Ano ter odprta vprašanja o odpovedih in cenah.

Za ABC najprej potrdite ali zavrnite AI-predlog, nato pri konfliktu izberite eno od treh možnosti roka. Šele nato je sinhronizacija dovoljena.

## Simulacija napake API-ja

1. Potrdite oziroma zavrnite vse postavke, ki potrebujejo pregled.
2. Označite **Simuliraj nedosegljiv interni API** in kliknite **Potrdi in sinhroniziraj**.
3. Potrjena različica se najprej shrani v H2 in prikaže se `PENDING_SYNC`.
4. Kliknite **Poskusi ponovno**. Ob treh neuspešnih poskusih se prikaže `FAILED_SYNC`.
5. Na voljo je **Kopiraj ročni povzetek**. Vsebuje samo potrjeni povzetek, potrjene naloge z odgovornimi osebami in roki ter potrjeni follow-up e-mail.
6. Izklopite simulacijo in ponovno poskusite za uspešno sinhronizacijo.

## Kontrole proti napačnim AI-podatkom

- AI-predlog nikoli ne postane naloga brez izrecnega statusa `approved`.
- `needs_review` in neobravnavan konflikt blokirata sinhronizacijo.
- Potrjena naloga zahteva naslov, obstoječo odgovorno osebo in veljaven ISO datum, če je rok naveden.
- Neznan klient zahteva ročni izbor.
- Dokazi iz transkripta in stopnja zaupanja niso dokaz resničnosti; zaposleni jih mora pregledati.
- Ročni fallback in e-mail ne vključujeta zavrnjenih ali nepotrjenih podatkov.

## Omejitve prototipa

H2 je pomnilniška demo baza in retry je namenoma preprost ter sinhron. Produkcijska različica bi potrebovala prijavo uporabnikov, avtorizacijo, HTTPS, varno hrambo skrivnosti, obstojno vrsto za retryje, audit log, opazljivost in preverjen adapter za pravi interni API oziroma AI ponudnika.
