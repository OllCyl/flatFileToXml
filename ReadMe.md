# Flatfile → XML

Konverterar en pipe-separerad flatfil till ett XML-dokument.

Flatfilen består av rader på formen `TYP|fält|fält|...`:

| Prefix | Betydelse | Fält | XML-element |
|--------|-----------|------|-------------|
| `P` | Person — startar nytt segment | förnamn, efternamn | `<person>` |
| `T` | Telefon | mobil, fastnät | `<phone>` |
| `A` | Adress | gata, stad, postnummer | `<address>` |
| `F` | Familjemedlem — startar nytt segment | namn, födelseår | `<family>` |

`T` och `A` som följer en `P`-rad hör till personen. `T` och `A` som följer en
`F`-rad hör till den familjemedlemmen. Exempeldata finns i `src/main/resources/flatIn.txt`
och resultatet blir `<people>` med nästlade `<person>`- och `<family>`-element.

## Användning

### Paketerad version (kräver inget Java)

Distribuera mappen `dist/datamapping/`:

1. Lägg `flatIn.txt` i samma mapp som `datamapping.exe`
2. Kör `datamapping.exe` — `xmlOut.xml` och `logs/datamapping.log` skapas i samma mapp

Egna sökvägar kan ges som argument:

```
datamapping.exe --flatfile.input=file:C:\data\minfil.txt --flatfile.output=file:resultat.xml
```

### Kör från källkod (kräver JDK 25)

```
mvnw spring-boot:run
```

Läser då `flatIn.txt` från `src/main/resources/` och skriver `xmlOut.xml` i
projektets rotmapp.

### Konfiguration (`application.yaml`)

```yaml
flatfile:
  input: flatIn.txt      # classpath:flatIn.txt, file:flatIn.txt eller absolut sökväg
  output: xmlOut.xml     # relativ eller absolut sökväg
logging:
  file:
    name: logs/datamapping.log
```

`input` utan prefix tolkas som en classpath-resurs. Använd `file:`-prefix eller
en absolut sökväg för filer utanför jar-filen.

## Bygga paketet

```
mvnw package
jpackage --name datamapping --input target/jpackage-input --main-jar datamapping-0.0.1-SNAPSHOT.jar --type app-image --dest dist --arguments "--flatfile.input=file:flatIn.txt"
```

Kopiera först jar-filen till `target/jpackage-input/` så att bara den följer med i imagen.

## Tester

Testerna är datadrivna: varje mapp under `src/test/resources/testdata/` innehåller
en `input.txt` och en `expected.xml`. Saknas `expected.xml` förväntas testet kasta
`IllegalArgumentException`. Kör med `mvnw test`.

---

## Om uppgiften

Arbetsuppgiften gick ut på att skapa en applikation som kan konvertera en flatfil
till ett XML-dokument.
Jag byggde den med Java 25, Maven och Spring Boot 4.2 då det är de tekniker jag senast har arbetat med och redan hade verktyg installerat.

### Antaganden

- `T`-radens fasta nummer mappas till XML-noden `<office>`
- Flatfilens encoding antas vara UTF-8
- Flatfilens radbrytningstecken antas vara CRLF
- Skickas tomma värden ska elementet fortfarande skapas i xml filen utan värde
- Ingen validering av fältvärden, datasetet är för litet och det finns ingen specifikation att validera mot (radordning och radtyper valideras dock då det brukar vara viktigt)

### Design före utveckling

Ursprunglig designskiss över relationer och kardinalitet:

| Relation | Kardinalitet | Beskrivning |
|----------|--------------|-------------|
| PEOPLE → PERSON | 1:N | En samling innehåller flera personer |
| PERSON → ADDRESS | 1:0..1 | En person kan ha en adress |
| PERSON → PHONE | 1:0..1 | En person kan ha två telefonnummer, ett för mobil och ett för jobb |
| PERSON → FAMILY | 1:0..N | En person kan ha flera familjemedlemmar |
| FAMILY → ADDRESS | 1:0..1 | En familjemedlem kan ha en adress |
| FAMILY → PHONE | 1:0..1 | En familjemedlem kan ha ett telefonnummer, ett för mobil och ett för jobb |

``` mermaid
erDiagram
    PEOPLE ||--|{ PERSON : contains

    PERSON ||--o| ADDRESS : has
    PERSON ||--o| PHONE : has
    PERSON ||--o{ FAMILY : has

    FAMILY ||--o| ADDRESS : has
    FAMILY ||--o| PHONE : has

    PEOPLE {
        int[] positions
    }

    PERSON {
        string firstname
        string lastname
    }

    ADDRESS {
        string street
        string city
        string postalcode
    }

    PHONE {
        string mobile
        string work
    }

    FAMILY {
        string name
        int born
    }

```

## AI-utvärdering av mig

### Det som talar för dig

- **Du har suttit i förarsätet.** Du bad om förslag först, inte färdig kod — och du fattade alla domän-beslut själv (xmlOut som källa, sedan korrigeringen att `T` hör till familjen).
- **Du verifierar istället för att lita.** Du fångade att mitt kodstycke inte gick att kompilera ("det innehåller en del fel"), och ifrågasatte var jag fick telefon-regeln ifrån — det visade sig vara din egen `xmlOut.xml` som var felritad.
- **Du granskade även AI-förslaget.** Reviewn du klistrade in hade en reell motsättning (`findSegments` som kastar vs ledande segment) som vi löste, inte kopierade blint.
- **Du driver arkitekturfrågor.** Frågan om var familjsegmenten borde ligga visade att du tänker på design, inte bara output.

### Det som kan sticka i ögonen

- **Vi gick fram och tillbaka** på telefonplacering och elementordning (`leafOrder` tillkom och togs bort). Det berodde på att `xmlOut.xml` var felritad från början — när du ändrade den fick du AI:n att skifta riktning, utan att markera att det var ett fel i specen du åtgärdade.
- **Devin-attributionen** togs bort ur den första committen eftersom den bara pushade befintlig kod — du tyckte det tog för mycket kred. Attributionen föll ändå bort i senare commits, så det spelade ingen större roll.

### Sammanfattningsvis

Du har använt AI som det borde användas: för mekanik (struktur, verifiering, packaging) medan du ägt krav, design och granskning. Koden är bättre efter dina invändningar än den hade varit utan.
