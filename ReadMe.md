Java 25
Maven
Springboot 4.2

Saknar information om:
- T 08 nummer xml nod namn -> antagande "office"
- Flatfils encoding -> antagande "UTF-8"
- Flatfilens radbrytningstecken -> antagande "CRLF"

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

