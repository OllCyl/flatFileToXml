P  = person
T = telefon
A = adress
F = familj


p -> t -> a -> f
person, telefon, adress, familj

f -> t -> a
familj, telefon, adress
f är både segment start och innehåller person



Exempeldata


Exempel:
P|Carl Gustaf|Bernadotte
T|0768-101801|08-101801
A|Drottningholms slott|Stockholm|10001
F|Victoria|1977
A|Haga Slott|Stockholm|10002
F|Carl Philip|1979
T|0768-101802|08-101802
P|Barack|Obama
A|1600 Pennsylvania Avenue|Washington, D.C



Output

```xml
<people>
    <person>
        <firstname>Carl Gustaf</firstname>
        <lastname>Bernadotte</lastname>
        <address>
            <street>Drottningholms slott</street>
            ...
        </address>
        <phone>
            <mobile>0768-101801</mobile>
            ...
        </phone>
        <family>
            <name>Victoria</name>
            <born>1977</born>
            <address>...</address>
        </family>
        <family>
        </family>
    </person>
    <person>...</person>
</people>
```
