## 2023-10-27 - Screen Reader Language Pronunciation
**Learning:** Adding French strings to the default `values/strings.xml` in an English-first application causes screen readers (TalkBack) to read the French text using an English Text-To-Speech engine, resulting in unintelligible pronunciation that defeats the accessibility purpose.
**Action:** Always put English strings in the default `values/strings.xml` and create localized files like `values-fr/strings.xml` for translations to ensure proper screen reader pronunciation.
