package com.n3d.netlab.i18n

import com.n3d.netlab.core.Ip

/**
 * The Czech course.
 *
 * A translation of [enCourse] page for page, not a second syllabus: the two
 * languages must explain the same method in the same order, or a learner who
 * switches languages mid-chapter lands somewhere else entirely.
 *
 * Vocabulary follows the user's own: hosts are "uzly", never "hosti".
 */
internal val csCourse: List<Chapter> = listOf(

    // -----------------------------------------------------------------------
    Chapter(
        title = "O co tu vlastně jde?",
        summary = "Začni tady — nic se nepředpokládá",
        pages = listOf(
            Page(
                "Adresa a síť, ve které leží",
                listOf(
                    Block.Para("Každé zařízení v síti má IPv4 adresu: čtyři čísla od 0 do 255, oddělená tečkami."),
                    Block.Formula("192.168.1.10"),
                    Block.Para("Samotná adresa toho moc neřekne. Podstatné je, do které skupiny adres patří — do které sítě. Zařízení ve stejné síti spolu mluví přímo; cokoliv mimo ni musí přes směrovač."),
                    Block.Para("Proto k adrese vždy patří druhá informace, která říká, kde její síť začíná a končí. Píše se jako lomítko a číslo:"),
                    Block.Formula("192.168.1.10 /24"),
                    Block.Para("Čti to jako: „tato adresa, v síti, které je pevně daných prvních 24 bitů“. Celá tahle aplikace je o tom, jak z jednoho takového řádku dostat úplný obrázek sítě."),
                ),
            ),
            Page(
                "Šest údajů, které vždycky spočítáš",
                listOf(
                    Block.Para("Ať otázka vypadá jakkoliv, výpočet podsítí vždy končí stejnými šesti údaji o bloku adres. Nauč se, co který znamená, a zbytek je počítání."),
                    Block.Table(listOf(
                        "Maska podsítě" to "dlouhý zápis /24 — 255.255.255.0",
                        "Adresa sítě" to "první adresa; pojmenovává blok",
                        "První uzel" to "adresa sítě + 1",
                        "Poslední uzel" to "broadcast − 1",
                        "Broadcast" to "poslední adresa; osloví všechny",
                        "Použitelné uzly" to "kolik zařízení se opravdu vejde",
                    )),
                    Block.Split(
                        caption = "Jeden blok 256 adres, 192.168.1.0/24",
                        capacity = 256,
                        parts = listOf(
                            SplitPart("síť", 1, "192.168.1.0"),
                            SplitPart("uzly .1 – .254", 254, "254 použitelných"),
                            SplitPart("bc", 1, "192.168.1.255"),
                        ),
                    ),
                    Block.Para("Blok je řada po sobě jdoucích adres. První a poslední jsou rezervované, všechno mezi nimi můžeš rozdat zařízením."),
                ),
            ),
            Page(
                "Proč se sítě vůbec dělí",
                listOf(
                    Block.Para("Firma dostane jeden blok adres a musí v něm provozovat několik oddělených sítí: kancelář, sklad, wifi, spoj mezi dvěma routery. Každá potřebuje vlastní blok."),
                    Block.Para("Rozřezání jednoho bloku na menší se jmenuje dělení na podsítě. Rozřezání na kusy různé velikosti — velký pro kancelář, drobný pro spoj mezi routery — se jmenuje VLSM, masky podsítí proměnné délky."),
                    Block.Split(
                        caption = "Stejných 256 adres, rozdělených na čtyři části",
                        capacity = 256,
                        parts = listOf(
                            SplitPart("A · 64", 64, "60 uzlů"),
                            SplitPart("B · 32", 32, "30 uzlů"),
                            SplitPart("C · 16", 16, "14 uzlů"),
                            SplitPart("D · 16", 16, "7 uzlů"),
                            SplitPart("volné · 128", 128, "zatím nevyužité", free = true),
                        ),
                    ),
                    Block.Para("Aplikace se ptá jenom na tyhle dvě věci. Rozeber jednu adresu, nebo rozděl jednu síť. Záložka Cvičení generuje obojí."),
                    Block.Tip("Na nic z toho nepotřebuješ kalkulačku. Všechno se scvrkne na zdvojnásobování, půlení a odčítání od 256 — a to se vejde do hlavy nebo na okraj papíru."),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "Dvojková soustava, jen to nutné",
        summary = "Osm sloupců a zdvojnásobování",
        pages = listOf(
            Page(
                "Adresa je ve skutečnosti jedno 32bitové číslo",
                listOf(
                    Block.Para("Počítač adresu drží jako 32 bitů — 32 jedniček a nul za sebou. Čtyři čísla s tečkami jsou jenom ty bity nakrájené na čtyři skupiny po osmi. Skupině osmi bitů se říká oktet."),
                    Block.Bits("192.168.1.10 tak, jak ji drží počítač", Ip.bits(Ip.parse("192.168.1.10")!!), 24),
                    Block.Para("Osm bitů je důvod, proč žádná část nemůže být větší než 255: osm jedniček je největší možná hodnota. Tečky jsou jenom interpunkce a do výpočtu nikdy nevstupují."),
                ),
            ),
            Page(
                "Osm sloupců",
                listOf(
                    Block.Para("Uvnitř jednoho oktetu má každý bit dvojnásobnou hodnotu než bit vpravo od něj. Nauč se tuhle řadu a převedeš obojím směrem:"),
                    Block.Formula("128  64  32  16  8  4  2  1"),
                    Block.Para("Z dvojkové do desítkové: sečti sloupce, ve kterých je jednička."),
                    Block.PlaceValue("10101100", 172),
                    Block.Work("sečtení sloupců", listOf(
                        "128 + 32 + 8 + 4  =  172",
                    )),
                    Block.Para("Z desítkové do dvojkové: jdi zleva doprava a ptej se „vejde se?“. Když ano, napiš 1 a odečti; když ne, napiš 0 a jdi dál. Převod čísla 200:"),
                    Block.Work("200 do dvojkové", listOf(
                        "128 se vejde do 200 -> 1  200-128 =  72",
                        " 64 se vejde do  72 -> 1   72- 64 =   8",
                        " 32 je moc velké    -> 0",
                        " 16 je moc velké    -> 0",
                        "  8 se vejde do   8 -> 1    8-  8 =   0",
                        "  4 je moc velké    -> 0",
                        "  2 je moc velké    -> 0",
                        "  1 je moc velké    -> 0",
                        "",
                        "200  =  11001000",
                    )),
                    Block.PlaceValue("200", 200),
                ),
            ),
            Page(
                "Mocniny dvojky",
                listOf(
                    Block.Para("Každý blok adres je mocnina dvojky — blok o velikosti 100 neexistuje. Tuhle tabulku je nejužitečnější umět napsat zpaměti, a stavíš ji zdvojnásobováním."),
                    Block.Table(powersTable(1..16)),
                    Block.Tip("Na papíře si tenhle žebřík napiš do rohu, než začneš. Každá otázka se pak zodpoví tím, že v něm najdeš číslo."),
                    Block.Check(
                        question = "Která mocnina dvojky je nejmenší z těch, které jsou aspoň 300?",
                        answer = "2^9 = 512",
                        why = "256 je 2^8 a na 300 nestačí, takže jdeš o příčku výš. Mezi nimi nic není.",
                    ),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "Maska a prefix",
        summary = "Kde končí síť a začínají uzly",
        pages = listOf(
            Page(
                "Jedna čára vedená 32 bity",
                listOf(
                    Block.Para("Maska rozdělí 32 bitů na dvě části: síťovou vlevo a uzlovou vpravo. Dělí se vždy na jediném místě — nikdy roztroušeně."),
                    Block.Para("Všechny adresy ve stejné síti mají síťové bity totožné. Uzlové bity jsou to, čím se jednotlivá zařízení liší."),
                    Block.Bits("maska pro /26", Ip.bits(Ip.mask(26)), 26),
                    Block.Para("Prefix je prostě počet bitů na síťové straně. /26 znamená 26 síťových bitů, takže zbývá 6 uzlových. Ta dvě čísla dají vždycky dohromady 32."),
                    Block.Formula("síťové bity + uzlové bity = 32"),
                ),
            ),
            Page(
                "Z prefixu na masku s tečkami",
                listOf(
                    Block.Para("Napiš tolik jedniček, kolik je prefix, doplň nulami do 32, rozděl na čtyři oktety a každý převeď. Nic víc maska není."),
                    Block.Work("/26 na masku s tečkami", listOf(
                        "26 jedniček, pak 6 nul:",
                        "11111111 11111111 11111111 11000000",
                        "",
                        "11111111 = 255",
                        "11111111 = 255",
                        "11111111 = 255",
                        "11000000 = 128+64 = 192",
                        "",
                        "/26  =  255.255.255.192",
                    )),
                    Block.Para("V praxi to takhle zdlouhavě nikdy neděláš, protože oktet masky může nabývat jen devíti hodnot. Dvě jedničky jsou vždycky 192, čtyři jedničky vždycky 240:"),
                    Block.Table(maskOctetTable("jedniček")),
                    Block.Note("Maska je vždycky řada jedniček následovaná řadou nul. 255.255.240.0 maska je. 255.0.255.0 není a žádné zařízení ji nepřijme."),
                ),
            ),
            Page(
                "Masky, které se vyplatí umět zpaměti",
                listOf(
                    Block.Para("Tyhle se objevují pořád. Nemusíš je umět hned první den — samy se usadí po pár cvičeních."),
                    Block.Table(prefixTable(24..30, "adr", "uzlů")),
                    Block.Para("A ty větší, které se používají jako síť, kterou dělíš:"),
                    Block.Table(prefixTable(16..23, "adr", "uzlů")),
                    Block.Check(
                        question = "Jaký prefix je 255.255.255.224?",
                        answer = "/27",
                        why = "224 jsou tři jedničky (11100000). 8 + 8 + 8 + 3 = 27.",
                    ),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "Uvnitř jednoho bloku",
        summary = "Velikost bloku, dvě rezervované adresy a to −2",
        pages = listOf(
            Page(
                "Jak je blok velký?",
                listOf(
                    Block.Para("Spočítej uzlové bity a umocni na ně dvojku. Při h uzlových bitech má blok 2^h adres."),
                    Block.Formula("velikost bloku  =  2^h,   h = 32 − prefix"),
                    Block.Work("/26", listOf(
                        "h = 32 - 26 = 6",
                        "blok = 2^6 = 64 adres",
                    )),
                    Block.Para("Je tu ještě druhé pravidlo, stejně důležité, a právě na tohle se zapomíná:"),
                    Block.Formula("blok smí začínat jen na násobku své vlastní velikosti"),
                    Block.Para("Blok 64 adres začíná na 0, 64, 128 nebo 192 — nikdy na 100. To není dohoda, to dělá maska: na začátku bloku musí být všechny uzlové bity nulové, a to nastane právě na těchhle adresách."),
                    Block.Split(
                        caption = "256 adres a místa, kde smí začít /26",
                        capacity = 256,
                        parts = listOf(
                            SplitPart(".0", 64, "začátek 0"),
                            SplitPart(".64", 64, "začátek 64"),
                            SplitPart(".128", 64, "začátek 128"),
                            SplitPart(".192", 64, "začátek 192"),
                        ),
                    ),
                ),
            ),
            Page(
                "Čtyři orientační body",
                listOf(
                    Block.Para("Jakmile víš, kde blok začíná a jak je velký, zbytek je sčítání. Vezmi 192.168.1.64/26 — blok 64 adres začínající na .64:"),
                    Block.Work("odečtení bloku", listOf(
                        "síť       192.168.1.64      <- začátek",
                        "první     192.168.1.65      <- síť + 1",
                        "poslední  192.168.1.126     <- broadcast - 1",
                        "broadcast 192.168.1.127     <- začátek + 64 - 1",
                    )),
                    Block.Para("Broadcast je poslední adresa bloku, proto je to začátek + velikost − 1, a ne začátek + velikost. Když počítáš od 64, šedesátá čtvrtá adresa je 127, ne 128 — 128 už patří dalšímu bloku."),
                    Block.Split(
                        caption = "192.168.1.64/26",
                        capacity = 64,
                        parts = listOf(
                            SplitPart("síť .64", 1, "adresa sítě"),
                            SplitPart("uzly .65 – .126", 62, "62 použitelných"),
                            SplitPart("bc .127", 1, "broadcast"),
                        ),
                    ),
                ),
            ),
            Page(
                "Proč se odečítají dvě",
                listOf(
                    Block.Para("Dvě adresy v každém bloku padnou na blok samotný. Nejnižší pojmenovává síť, nejvyšší je broadcast — ani jednu nesmíš dát zařízení."),
                    Block.Formula("použitelné uzly  =  2^h − 2"),
                    Block.Para("Proto má /26 sice 64 adres, ale jen 62 uzlů. Otoč to a vznikne pravidlo, které při plánování použiješ pořád:"),
                    Block.Formula("potřebné adresy  =  uzly + 2"),
                    Block.Table(prefixTable(26..30, "adr", "uzlů")),
                    Block.Note("/31 a /32 jsou výjimky a obě odpovídají 0 použitelných uzlů. RFC 3021 sice /31 dovoluje pro dva routery na spoji bod-bod, ale pokud to zadání neřekne, odpověz 0."),
                    Block.Check(
                        question = "Oddělení má 62 strojů. Vejde se do /26?",
                        answer = "Ano, přesně — a nic nezbyde.",
                        why = "62 uzlů potřebuje 64 adres. /26 je 64. 63 strojů by už potřebovalo /25.",
                    ),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "Postup: jedna adresa → všechno",
        summary = "Šest kroků a tři vyřešené příklady",
        pages = listOf(
            Page(
                "Šest kroků",
                listOf(
                    Block.Para("Tohle je celý postup. Funguje na libovolnou adresu a libovolný prefix, nepotřebuje kalkulačku a je to ten, který se vyplatí procvičit do automatismu."),
                    Block.Recipe(listOf(
                        RecipeStep("Napiš masku. Prefix říká počet jedniček, tabulka oktetů z toho udělá číslo."),
                        RecipeStep("Najdi zajímavý oktet — ten, kde maska není ani 255, ani 0. Tam padají hranice bloků."),
                        RecipeStep("Magické číslo = 256 − ten oktet masky. Bloky v tom oktetu začínají po magickém čísle."),
                        RecipeStep("Hodnotu adresy v tom oktetu zaokrouhli dolů na násobek magického čísla a všechny oktety vpravo vynuluj. To je adresa sítě."),
                        RecipeStep("Broadcast: k tomu oktetu přičti magické číslo, odečti jedna a všechny oktety vpravo nastav na 255."),
                        RecipeStep("První uzel = síť + 1. Poslední uzel = broadcast − 1. Použitelných = 2^h − 2."),
                    )),
                    Block.Tip("Kroky 4 a 5 sahají vždycky jen na jeden oktet. Všechno vlevo se opíše beze změny, všechno vpravo je 0 v adrese sítě a 255 v broadcastu."),
                ),
            ),
            Page(
                "Vyřešený příklad 1 — 192.168.1.200/26",
                listOf(
                    Block.Work("krok 1 — maska", listOf(
                        "/26 = 26 jedniček",
                        "8 + 8 + 8 + 2 -> poslední oktet má 2 jedničky",
                        "2 jedničky = 192",
                        "maska = 255.255.255.192",
                    )),
                    Block.Work("krok 2 — zajímavý oktet", listOf(
                        "255 . 255 . 255 . 192",
                        "                   ^ není 255 ani 0",
                        "-> 4. oktet",
                    )),
                    Block.Work("krok 3 — magické číslo", listOf(
                        "256 - 192 = 64",
                        "bloky začínají na 0, 64, 128, 192",
                    )),
                    Block.Work("krok 4 — adresa sítě", listOf(
                        "náš 4. oktet je 200",
                        "největší násobek 64, který je <= 200:",
                        "  64 x 3 = 192   (64 x 4 = 256, moc)",
                        "síť = 192.168.1.192",
                    )),
                    Block.Work("krok 5 — broadcast", listOf(
                        "192 + 64 - 1 = 255",
                        "broadcast = 192.168.1.255",
                    )),
                    Block.Work("krok 6 — uzly", listOf(
                        "první    = 192.168.1.193",
                        "poslední = 192.168.1.254",
                        "h = 32 - 26 = 6",
                        "použitelných = 2^6 - 2 = 62",
                    )),
                    Block.Bits("192.168.1.200 — hranice na 26 bitech", Ip.bits(Ip.parse("192.168.1.200")!!), 26),
                ),
            ),
            Page(
                "Vyřešený příklad 2 — 172.16.34.77/20",
                listOf(
                    Block.Para("Stejných šest kroků. Jediný rozdíl je, že hranice teď padne do třetího oktetu, takže čtvrtý oktet je čistě uzlový prostor."),
                    Block.Work("kroky 1 až 3", listOf(
                        "/20 = 8 + 8 + 4",
                        "4 jedničky = 240",
                        "maska = 255.255.240.0",
                        "zajímavý oktet = 3.",
                        "magické číslo = 256 - 240 = 16",
                        "bloky: 0, 16, 32, 48, 64, ...",
                    )),
                    Block.Work("krok 4 — síť", listOf(
                        "3. oktet je 34",
                        "16 x 2 = 32   (16 x 3 = 48, moc)",
                        "-> 32, a všechno vpravo je 0",
                        "síť = 172.16.32.0",
                    )),
                    Block.Work("krok 5 — broadcast", listOf(
                        "32 + 16 - 1 = 47",
                        "všechno vpravo je 255",
                        "broadcast = 172.16.47.255",
                    )),
                    Block.Work("krok 6 — uzly", listOf(
                        "první    = 172.16.32.1",
                        "poslední = 172.16.47.254",
                        "h = 32 - 20 = 12",
                        "použitelných = 2^12 - 2 = 4094",
                    )),
                    Block.Note("Nejčastější chyba je odpovědět 172.16.34.0. Smysl kroku 4 je právě v tom, že 34 není začátek bloku; 32 ano."),
                ),
            ),
            Page(
                "Vyřešený příklad 3 — 10.150.200.30/11",
                listOf(
                    Block.Para("Velký příklad, aby bylo vidět, že se nic nemění. Hranice je ve druhém oktetu, takže třetí a čtvrtý jsou celé uzlový prostor."),
                    Block.Work("všech šest kroků", listOf(
                        "/11 = 8 + 3",
                        "3 jedničky = 224",
                        "maska = 255.224.0.0",
                        "zajímavý oktet = 2.",
                        "magické číslo = 256 - 224 = 32",
                        "",
                        "2. oktet je 150",
                        "32 x 4 = 128  (32 x 5 = 160, moc)",
                        "síť       = 10.128.0.0",
                        "",
                        "128 + 32 - 1 = 159",
                        "broadcast = 10.159.255.255",
                        "",
                        "první    = 10.128.0.1",
                        "poslední = 10.159.255.254",
                        "h = 32 - 11 = 21",
                        "použitelných = 2^21 - 2 = 2097150",
                    )),
                    Block.Check(
                        question = "Ve které síti je 10.150.200.30/11 spolu s 10.130.7.7/11?",
                        answer = "Ve stejné — 10.128.0.0/11.",
                        why = "Oba druhé oktety se zaokrouhlí dolů na 128, a to je jediný oktet, kterým maska prochází.",
                    ),
                ),
            ),
            Page(
                "Kontrola za deset sekund",
                listOf(
                    Block.Para("Tři kontroly odhalí skoro každou chybu. Projeď je, než výsledek napíšeš."),
                    Block.Bullets(listOf(
                        "Adresa sítě musí být v zajímavém oktetu násobkem velikosti bloku.",
                        "Broadcast − síť + 1 se musí přesně rovnat velikosti bloku.",
                        "Zadaná adresa musí ležet mezi nimi. Pokud ne, zaokrouhlil jsi špatným směrem.",
                    )),
                    Block.Work("kontrola příkladu 1", listOf(
                        "192 / 64 = 3 přesně          ok",
                        "255 - 192 + 1 = 64           ok",
                        "192 <= 200 <= 255            ok",
                    )),
                    Block.Tip("Pokud prefix padne přesně na hranici oktetu — /8, /16, /24 — žádný částečný oktet není, oktet masky je 0 a magické číslo je 256, což znamená, že celý oktet je jeden blok."),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "Druhá metoda: binární AND",
        summary = "Co možná učí ve škole — stejný výsledek",
        pages = listOf(
            Page(
                "Dvě metody, jedna odpověď",
                listOf(
                    Block.Para("Setkáš se se dvěma způsoby, jak najít adresu sítě, a nejsou to soupeři — ten druhý je důkaz, že první funguje."),
                    Block.Bullets(listOf(
                        "Metoda magického čísla z předchozí kapitoly. Jenom desítkové počítání. Používá se, když je potřeba být rychlý, a jsou na ní postavené síťařské zkoušky.",
                        "Metoda binárního AND, níže. Napíšeš adresu i masku v bitech a spojíš je. Pomalejší, ale je na ní přesně vidět, proč výsledek vychází.",
                    )),
                    Block.Para("Obě jsou metody na tužku a papír. Ani jedna není správnější. Pokud ve škole učí tu binární, používej ji — a magické číslo si nech na kontrolu, protože zabere pět sekund."),
                ),
            ),
            Page(
                "Jak AND funguje",
                listOf(
                    Block.Para("AND je pravidlo bit po bitu: 1 AND 1 je 1, všechno ostatní je 0. Protože maska jsou jedničky a pak nuly, AND zachová každý síťový bit adresy a každý uzlový smaže na nulu — a to je přesně definice adresy sítě."),
                    Block.Work("192.168.1.200/26 dlouhou cestou", listOf(
                        "adr   11000000 10101000 00000001 11001000",
                        "maska 11111111 11111111 11111111 11000000",
                        "AND   -------- -------- -------- --------",
                        "      11000000 10101000 00000001 11000000",
                        "",
                        "        192  .  168  .    1   .   192",
                    )),
                    Block.Para("Pro broadcast síťové bity necháš a všechny uzlové místo toho nastavíš na 1:"),
                    Block.Work("broadcast dlouhou cestou", listOf(
                        "síť   11000000 10101000 00000001 11000000",
                        "uzlové bity samé 1:",
                        "      11000000 10101000 00000001 11111111",
                        "",
                        "        192  .  168  .    1   .   255",
                    )),
                    Block.Para("Stejné 192.168.1.192 a 192.168.1.255, jaké dalo magické číslo, jenom s osmkrát větším psaním."),
                ),
            ),
            Page(
                "Kterou kdy použít",
                listOf(
                    Block.Table(listOf(
                        "Když tlačí čas" to "magické číslo",
                        "Domácí úkol s postupem" to "binární AND",
                        "Kontrola výsledku" to "ta druhá",
                        "Pochopení proč" to "binární AND",
                    )),
                    Block.Para("Užitečný kompromis: převeď do dvojkové jen zajímavý oktet. Ostatní tři se buď opisují beze změny, nebo nulují, takže je psát v bitech nic nedokazuje."),
                    Block.Work("jen ten oktet, o který jde", listOf(
                        "200   = 11001000",
                        "192 m = 11000000   (oktet masky)",
                        "AND   = 11000000 = 192",
                    )),
                    Block.Tip("Řešení krok za krokem v téhle aplikaci používají metodu magického čísla a vedle ní kreslí pás bitů, takže vidíš obojí najednou."),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "VLSM: rozdělení jedné sítě",
        summary = "Různě velké podsítě z jednoho bloku",
        pages = listOf(
            Page(
                "Jak zadání vypadá",
                listOf(
                    Block.Para("Zadání VLSM ti dá jednu síť a seznam oddělení s počtem zařízení:"),
                    Block.Work("typické zadání", listOf(
                        "Rozděl 10.0.0.0/24 na:",
                        "  A - 60 uzlů",
                        "  B - 30 uzlů",
                        "  C - 14 uzlů",
                        "  D -  7 uzlů",
                    )),
                    Block.Para("Každému musíš dát dost velký blok a plýtvat co nejméně. Dát všem čtyřem /26 by početně vyšlo a bylo by to špatně — vyplýtvá to většinu sítě a nikdo to neuzná."),
                    Block.Para("Odpověď je tabulka: pro každou podsíť adresa sítě, prefix, rozsah uzlů a broadcast."),
                ),
            ),
            Page(
                "Pět kroků",
                listOf(
                    Block.Recipe(listOf(
                        RecipeStep("Seřaď podsítě podle počtu uzlů, největší první. Tohle udělej dřív než cokoliv jiného."),
                        RecipeStep("Vezmi největší. Přičti 2 k počtu uzlů a zaokrouhli nahoru na nejbližší mocninu dvojky. To je velikost bloku."),
                        RecipeStep("Uzlové bity h je exponent, který jsi právě použil; prefix je 32 − h."),
                        RecipeStep("Umísti blok na první volnou adresu. Odečti adresu sítě, první a poslední uzel a broadcast přesně jako v předchozí kapitole."),
                        RecipeStep("Posuň se na adresu hned za tím broadcastem a opakuj s další podsítí v pořadí."),
                    )),
                    Block.Formula("uzly + 2  →  zaokrouhli nahoru na 2^h  →  prefix = 32 − h"),
                    Block.Note("Zaokrouhluje se nahoru, nikdy na nejbližší. 7 uzlů potřebuje 9 adres a 8 nestačí — je potřeba blok 16."),
                ),
            ),
            Page(
                "Proč opravdu největší první",
                listOf(
                    Block.Para("Tenhle krok se přeskakuje a přitom je to on, kdo celou metodu drží pohromadě. Blok smí začínat jen na násobku své velikosti, takže velký blok potřebuje počáteční adresu, kterou mu malý blok snadno zablokuje."),
                    Block.Para("Udělej to ve špatném pořadí — nejdřív 16 adres pro D, pak 64 pro A:"),
                    Block.Work("nejmenší první, a je zle", listOf(
                        "D (/28, 16) na 10.0.0.0 -> končí .15",
                        "kurzor je teď na 10.0.0.16",
                        "",
                        "A potřebuje blok 64.",
                        "smí začít jen na 0, 64, 128, 192.",
                        "16 není násobek 64. Ani 32, ani 48.",
                        "-> A musí skočit na 10.0.0.64",
                        "",
                        ".16 až .63 = 48 adres v pasti",
                    )),
                    Block.Split(
                        caption = "Nejmenší první — díra, kterou nic nevyužije",
                        capacity = 256,
                        parts = listOf(
                            SplitPart("D · 16", 16, "umístěná první"),
                            SplitPart("v pasti · 48", 48, "vyplýtváno", free = true),
                            SplitPart("A · 64", 64, "vytlačená sem"),
                            SplitPart("zbytek", 128, "", free = true),
                        ),
                    ),
                    Block.Para("Když jdeš od největší, tohle nastat nemůže: po umístění bloku 64 je kurzor na násobku 64, což je automaticky i násobek 32, 16, 8 a 4. Každý menší blok, který přijde po něm, padne na povolenou počáteční adresu, aniž bys na to musel myslet."),
                    Block.Tip("Přesně proto tě první fáze cvičení nutí podsítě přetáhnout do pořadí, než ti dovolí cokoliv počítat."),
                ),
            ),
            Page(
                "Vyřešený příklad — 10.0.0.0/24",
                listOf(
                    Block.Work("krok 1 — seřaď", listOf(
                        "A 60, B 30, C 14, D 7   (už v pořadí)",
                        "kontrola celkové potřeby:",
                        "64 + 32 + 16 + 16 = 128 <= 256   vejde se",
                    )),
                    Block.Work("A - 60 uzlů", listOf(
                        "60 + 2 = 62",
                        "2^5 = 32 málo, 2^6 = 64 ok",
                        "h = 6, prefix = 32 - 6 = /26",
                        "začíná na 10.0.0.0",
                        "síť       10.0.0.0/26",
                        "první     10.0.0.1",
                        "poslední  10.0.0.62",
                        "broadcast 10.0.0.63",
                        "volno od  10.0.0.64",
                    )),
                    Block.Work("B - 30 uzlů", listOf(
                        "30 + 2 = 32",
                        "2^5 = 32 přesně, ok",
                        "h = 5, prefix = /27",
                        "64 je násobek 32, takže B začíná tam",
                        "síť       10.0.0.64/27",
                        "první     10.0.0.65",
                        "poslední  10.0.0.94",
                        "broadcast 10.0.0.95",
                        "volno od  10.0.0.96",
                    )),
                    Block.Work("C - 14 uzlů", listOf(
                        "14 + 2 = 16 -> 2^4, h = 4, /28",
                        "síť       10.0.0.96/28",
                        "první     10.0.0.97",
                        "poslední  10.0.0.110",
                        "broadcast 10.0.0.111",
                        "volno od  10.0.0.112",
                    )),
                    Block.Work("D - 7 uzlů", listOf(
                        "7 + 2 = 9",
                        "2^3 = 8 málo, 2^4 = 16 ok",
                        "h = 4, /28  (stejně jako C)",
                        "síť       10.0.0.112/28",
                        "první     10.0.0.113",
                        "poslední  10.0.0.126",
                        "broadcast 10.0.0.127",
                        "volno od  10.0.0.128",
                    )),
                    Block.Split(
                        caption = "Hotový plán",
                        capacity = 256,
                        parts = listOf(
                            SplitPart("A /26", 64, "60 uzlů"),
                            SplitPart("B /27", 32, "30 uzlů"),
                            SplitPart("C /28", 16, "14 uzlů"),
                            SplitPart("D /28", 16, "7 uzlů"),
                            SplitPart("volné", 128, "10.0.0.128 – .255", free = true),
                        ),
                    ),
                    Block.Note("Zajímavá je D: 7 uzlů potřebuje 9 adres, 8 je o jednu málo, takže D dostane /28 přesně jako C. Zaokrouhlování nahoru stojí adresy a nedá se obejít."),
                ),
            ),
            Page(
                "Vyřešený příklad — přes hranice oktetů",
                listOf(
                    Block.Para("Stejná metoda na větší síti, kde jsou bloky delší než jeden oktet. Rozděl 192.168.8.0/22 — 1024 adres, 192.168.8.0 až 192.168.11.255."),
                    Block.Work("seřazeno a umístěno", listOf(
                        "Sklad 500 | Provoz 200 | Lab 100 | Wifi 60 | Spoj 2",
                        "",
                        "Sklad  500+2=502 -> 512 = 2^9  -> /23",
                        "  192.168.8.0/23",
                        "  uzly 192.168.8.1 - 192.168.9.254",
                        "  bc   192.168.9.255",
                        "",
                        "Provoz 200+2=202 -> 256 = 2^8  -> /24",
                        "  192.168.10.0/24",
                        "  uzly .1 - .254   bc 192.168.10.255",
                        "",
                        "Lab    100+2=102 -> 128 = 2^7  -> /25",
                        "  192.168.11.0/25",
                        "  uzly .1 - .126   bc 192.168.11.127",
                        "",
                        "Wifi    60+2= 62 ->  64 = 2^6  -> /26",
                        "  192.168.11.128/26",
                        "  uzly .129 - .190  bc 192.168.11.191",
                        "",
                        "Spoj     2+2=  4 ->   4 = 2^2  -> /30",
                        "  192.168.11.192/30",
                        "  uzly .193 - .194  bc 192.168.11.195",
                        "",
                        "využito 512+256+128+64+4 = 964 z 1024",
                        "volné 192.168.11.196 - .255  (zbývá 60)",
                    )),
                    Block.Para("Blok 512 adres jsou dva plné rozsahy po 256 adresách, takže rozsah uzlů Skladu jde přes hranici z .8 do .9. Na metodě se nemění nic — počítá se pořád v zajímavém oktetu, kterým je u /23 ten třetí: maska je 255.255.254.0, magické číslo je 2 a bloky začínají na každé sudé hodnotě třetího oktetu."),
                    Block.Tip("/30 se dvěma použitelnými uzly je standardní velikost spoje mezi dvěma routery. Objeví se skoro v každém zadání."),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "Pasti a závěrečná kontrola",
        summary = "Chyby, které stojí body",
        pages = listOf(
            Page(
                "Sedm způsobů, jak to zkazit",
                listOf(
                    Block.Bullets(listOf(
                        "Zapomenuté +2. 62 uzlů se do /26 vejde, ale 62 adres ne — potřebuješ 64.",
                        "Zaokrouhlení na nejbližší mocninu dvojky místo nahoru. 9 adres potřebuje 16, ne 8.",
                        "Řazení od nejmenší, takže velký blok nemá kam legálně padnout.",
                        "Začátek bloku na adrese, která není násobkem jeho velikosti — /28 smí začít na .96 nebo .112, nikdy na .100.",
                        "Počítání dvěstěpadesátpětek místo jedniček. 255.255.255.192 je /26, ne /24.",
                        "Předpoklad, že broadcast končí na 255. V /26 je to .63, .127, .191 nebo .255.",
                        "Odpověď 2 použitelné uzly u /31. V konfiguraci routeru správně, u skoro každé zkoušky špatně.",
                    )),
                ),
            ),
            Page(
                "Kontrola hotového plánu",
                listOf(
                    Block.Para("Dvě kontroly odhalí rozbitý plán VLSM okamžitě:"),
                    Block.Bullets(listOf(
                        "Každá adresa sítě je násobkem své vlastní velikosti bloku.",
                        "Broadcast každé podsítě je přesně o jedna níž než adresa sítě té následující — žádné díry, žádné překryvy.",
                    )),
                    Block.Work("kontrola plánu 10.0.0.0/24", listOf(
                        "  0 / 64 = 0 přesně   bc  63 -> další  64  ok",
                        " 64 / 32 = 2 přesně   bc  95 -> další  96  ok",
                        " 96 / 16 = 6 přesně   bc 111 -> další 112  ok",
                        "112 / 16 = 7 přesně   bc 127 -> volno 128  ok",
                    )),
                    Block.Check(
                        question = "Plán dává /27 na 10.0.0.80. Je to legální?",
                        answer = "Ne.",
                        why = "/27 je blok 32 adres a musí začínat na násobku 32: 64 nebo 96. 80 je jen násobek 16.",
                    ),
                ),
            ),
        ),
    ),

    // -----------------------------------------------------------------------
    Chapter(
        title = "Tahák",
        summary = "Všechno na jedné stránce",
        pages = listOf(
            Page(
                "Co si napsat na okraj",
                listOf(
                    Block.Heading("Mocniny dvojky"),
                    Block.Formula("2 4 8 16 32 64 128 256 512 1024"),
                    Block.Heading("Hodnoty oktetu masky"),
                    Block.Formula("0 128 192 224 240 248 252 254 255"),
                    Block.Heading("Jedna adresa → všechno"),
                    Block.Recipe(listOf(
                        RecipeStep("maska z prefixu"),
                        RecipeStep("zajímavý oktet = ten, který není 255 ani 0"),
                        RecipeStep("magické číslo = 256 − ten oktet"),
                        RecipeStep("adresu zaokrouhli dolů na násobek magického čísla → síť"),
                        RecipeStep("síť + magické číslo − 1 v tom oktetu, vpravo 255 → broadcast"),
                        RecipeStep("první = síť + 1, poslední = bc − 1, použitelných = 2^h − 2"),
                    )),
                    Block.Heading("Rozdělení sítě"),
                    Block.Recipe(listOf(
                        RecipeStep("seřaď od největší"),
                        RecipeStep("uzly + 2, zaokrouhli nahoru na 2^h"),
                        RecipeStep("prefix = 32 − h"),
                        RecipeStep("umísti na první volnou adresu a odečti blok"),
                        RecipeStep("kurzor = broadcast + 1, opakuj"),
                    )),
                    Block.Table(prefixTable(24..30, "adr", "uzlů")),
                ),
            ),
        ),
    ),
)
