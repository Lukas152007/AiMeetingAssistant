# AI Meeting Assistant

Lokalno zaganjljiv prototip za pretvorbo transkripta sestanka v pregleden predlog, ki ga zaposleni popravi in potrdi pred zapisom v interni sistem. Vmesnik in primeri so v slovenščini.

## Problem, ki ga rešuje

Po sestankih so dogovori razpršeni po zapiskih, odgovornosti pa se lahko napačno sklepajo. Prototip pokaže varen tok, kjer AI pomaga strukturirati informacije, človek pa ostane nosilec poslovne odločitve.

## Glavni tok

```text
Transkript → demo AI analiza → človeški pregled in potrditev
                                      ↓
                         validacija / mock Internal Business API
                                      ↓
                      zapisnik + potrjene naloge + follow-up osnutek
```

## Funkcionalnosti

- primer transkripta za `Podjetje ABC d.o.o.` z zahtevami, dogovorjeno nalogo, AI-predlogom, odprtim vprašanjem in konfliktom roka;
- ločeni tipi `agreed`, `ai_suggestion` in `needs_review`, izvorni citati ter stopnja zaupanja;
- urejanje povzetka, opisov, odgovorne osebe, roka in statusa vsake postavke;
- H2 trajna (v pomnilniku za demo) hramba pregledane verzije, mock klientov/zaposlenih/zapisnikov/nalog;
- varna sinhronizacija, idempotency ključ (`meeting_id:item_id`), retry in kopirljiv ročni fallback;
- follow-up e-mail, sestavljen samo iz podatkov s statusom `approved`.

## Arhitektura

```text
React + Vite ─REST─> Spring Boot
                       ├─ TranscriptAnalysisService (offline DemoTranscriptAnalysisService)
                       ├─ MeetingService: pregled, validacija, retry, e-mail
                       ├─ InternalBusinessService: integracijska meja in idempotency
                       └─ H2 / JPA: predlogi in mock interni podatki
```

`InternalBusinessService` je namenoma ločen od mock podatkovnega API-ja: v resnični izvedbi bi ga zamenjal adapter za dokumentirani interni API.

## Lokalni zagon

Predpogoji: Java 21+ in Maven 3.9+, Node.js 20+.

```powershell
cd backend
mvn spring-boot:run
```

V drugem terminalu:

```powershell
cd frontend
npm install
npm run dev
```

Odprite `http://localhost:5173`. Backend je na `http://localhost:8080`, H2 konzola pa na `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:meetingassistant`).

## Demo potek

1. Izberite **Naloži primer transkripta** in nato **Analiziraj transkript**.
2. Preglejte citate. AI-predlog zavrnite ali ga izrecno potrdite in mu dodelite zaposlenega.
3. Konflikt glede novembra/decembra označite kot potrjen/popravljen ali zavrnjen.
4. Izberite **Potrdi in sinhroniziraj**. Vidni so ustvarjeni zapisnik in naloge.

## Napaka API-ja in ročni fallback

Označite **Simuliraj nedosegljiv interni API** pred sinhronizacijo. Potrjena različica se najprej shrani v H2 in dobi `PENDING_SYNC`. Gumb **Poskusi ponovno** izvede največ tri poskuse z naraščajočim čakanjem; ob nadaljnji nedosegljivosti se stanje spremeni v `FAILED_SYNC`, podatki pa ostanejo shranjeni. Prikaže se kopirljiv ročni fallback s potrjenim povzetkom, nalogami in e-mailom. Izklopite simulacijo in ponovno poskusite za uspeh.

## Varnost poslovnih podatkov

AI ni avtoriteta. JSON Schema določa strukturo odgovora, ne pa vsebinske pravilnosti. Zato so izvorni citati, validacija in človeška potrditev obvezni. AI-predlog ni dogovorjena obveznost: ne more se sinhronizirati brez statusa `approved`. Neobravnavane nejasnosti/konflikti, neznan klient, neobstoječi zaposleni in neveljavni datumi sinhronizacijo blokirajo.

## Omejitve in produkcijska nadgradnja

`DEMO_MODE=true` ne kliče zunanjega AI-ja; za načrt prave, strogo strukturirane integracije glejte [docs/ai-prompt.md](docs/ai-prompt.md). H2 je demonstracijska baza, retry je zavestno preprost in sinhron. Produkcija bi dodala avtentikacijo, avtorizacijo, HTTPS, šifrirano upravljanje skrivnosti, obstojno čakalno vrsto z background retryji, audit log, omejevanje dostopa in integracijski adapter z opazljivostjo.
