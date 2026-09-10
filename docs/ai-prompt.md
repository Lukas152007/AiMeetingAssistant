# Predlog prompta za pravo AI-integracijo

Sistemskemu modelu bi poslali transkript skupaj z JSON Schema za `MeetingDto` in naslednja pravila:

> Uporabi izključno izrecno navedene informacije iz transkripta. Za vsak podatek navedi kratek dobesedni `source_text`; če ga ni, vrni `null`. Nalogo označi kot `agreed` samo, če je v transkriptu izrecno dogovorjena naloga, odgovorna oseba ali rok. Smiseln, a nedogovorjen naslednji korak označi izključno kot `ai_suggestion` in `needs_review`. Ne ugibaj klienta, rokov, odgovorne osebe ali CRM sistema. Zaznaj nasprotujoče si trditve kot `conflicts`. JSON Schema zagotavlja obliko odgovora, ne pa njegove resničnosti; citati so obvezni.

V produkciji bi adapter `AiTranscriptAnalysisService` uporabil `OPENAI_API_KEY`, zahteval strogi JSON Schema output, validiral odgovor in ga šele nato posredoval istemu postopku človeškega pregleda.
