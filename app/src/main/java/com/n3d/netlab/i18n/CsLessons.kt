package com.n3d.netlab.i18n

import com.n3d.netlab.core.Ip

internal val csLessons: List<Lesson> = listOf(

    Lesson(
        title = "Adresa je jedno 32bitové číslo",
        summary = "Proč jsou tečky jen interpunkce",
        body = listOf(
            Block.Para("IPv4 adresa je jediné 32bitové číslo. Čtyři čísla oddělená tečkami nejsou nic víc než čitelný způsob, jak ho zapsat: každé z nich je osm z těch bitů, a proto žádné nemůže přesáhnout 255."),
            Block.Bits("192.168.1.10 binárně", Ip.bits(Ip.parse("192.168.1.10")!!), 24),
            Block.Para("Uvnitř jednoho oktetu má každý bit dvojnásobnou hodnotu než ten napravo od něj:"),
            Block.Formula("128  64  32  16  8  4  2  1"),
            Block.Para("Takže 192 je 128 + 64, což je 11000000. Součet všech bitů dá 255 a to je nejvíc, co oktet zvládne. Umět převést jeden oktet z hlavy je většina toho, proč jde dělení sítí některým lidem tak rychle."),
            Block.Note("Všechno ostatní v téhle aplikaci je počítání s tím jedním 32bitovým číslem. Tečky se výpočtu nikdy neúčastní."),
        ),
    ),

    Lesson(
        title = "Maska určuje, kde končí síť",
        summary = "Prefix, tečkovaná maska a co vlastně říkají",
        body = listOf(
            Block.Para("Samotná adresa neříká, do jaké sítě patří. To říká maska. Rozdělí těch 32 bitů na síťovou část vlevo a uzlovou část vpravo — a dělí se vždycky na jednom místě, nikdy roztroušeně."),
            Block.Para("Prefix je jen počet bitů na straně sítě. /24 znamená 24 bitů sítě a 8 bitů uzlu. Napiš 24 jedniček a za ně 8 nul a máš tečkovanou masku:"),
            Block.Bits("maska pro /24", Ip.bits(Ip.mask(24)), 24),
            Block.Formula("11111111.11111111.11111111.00000000  =  255.255.255.0"),
            Block.Para("Všechno ostatní plyne z bitů uzlu. Při h bitech uzlu pojme blok 2^h adres a smí začínat jen na adrese, která je násobkem 2^h."),
            Block.Para("Masky, které se vyplatí umět zpaměti:"),
            Block.Table(prefixTable(24..30, "adres", "uzlů")),
            Block.Para("A ty větší, pro výchozí sítě:"),
            Block.Table(prefixTable(16..23, "adres", "uzlů")),
            Block.Note("Maska je vždycky řada jedniček následovaná řadou nul. 255.255.240.0 je maska; 255.0.255.0 není a žádné zařízení ji nepřijme."),
        ),
    ),

    Lesson(
        title = "Adresa sítě, broadcast a to −2",
        summary = "Dvě adresy, které nikdy nedostaneš",
        body = listOf(
            Block.Para("Každý blok utratí dvě ze svých adres sám za sebe. Ta nejnižší — všechny bity uzlu nulové — pojmenovává síť. Ta nejvyšší — všechny bity uzlu jedničky — je broadcast, tedy všesměrová adresa pro všechno uvnitř. Ani jednu nelze nastavit stroji."),
            Block.Formula("použitelné uzly  =  2^h − 2"),
            Block.Para("Proto /26 obsahuje 64 adres, ale jen 62 uzlů, a proto se oddělení s 62 stroji do /26 vejde, kdežto oddělení s 63 už ne a potřebuje /25."),
            Block.Para("Blok se čte popořadě takhle:"),
            Block.Bullets(listOf(
                "Adresa sítě — vlastní adresa bloku, bity uzlu samé 0",
                "První použitelná — adresa sítě + 1",
                "Poslední použitelná — broadcast − 1",
                "Broadcast — bity uzlu samé 1, tedy také adresa sítě + velikost bloku − 1",
            )),
            Block.Note("/31 a /32 jsou výjimky. Do /31 se obojí nevejde a /32 je jediná adresa; obojí má 0 použitelných uzlů. RFC 3021 sice dovoluje, aby /31 nesl dva routery na point-to-point spoji, ale pokud to zadání neřekne, odpověz nula."),
        ),
    ),

    Lesson(
        title = "Magické číslo",
        summary = "Jak najít adresu sítě za pár vteřin a bez binárky",
        body = listOf(
            Block.Para("Binárku skoro nikdy psát nemusíš. Najdi ten jeden oktet, kde maska není ani 255, ani 0 — říkejme mu zajímavý oktet — a odečti ho od 256. To je magické číslo a je to velikost bloku měřená v tom oktetu."),
            Block.Formula("magické číslo  =  256 − oktet masky"),
            Block.Para("Ukázka: do jaké sítě patří 172.16.34.77/20?"),
            Block.Bullets(listOf(
                "/20 → maska 255.255.240.0, zajímavý je tedy třetí oktet.",
                "256 − 240 = 16. Bloky začínají po 16 ve třetím oktetu: 0, 16, 32, 48, 64…",
                "Adresa má v tom oktetu 34. Zaokrouhli dolů na nejbližší násobek 16 → 32.",
                "Všechno za ním vynuluj: síť je 172.16.32.0.",
                "Broadcast je o blok výš minus jedna: 32 + 16 − 1 = 47 a zbytek jde na 255 → 172.16.47.255.",
                "Uzly běží od 172.16.32.1 do 172.16.47.254 — je jich 4094.",
            )),
            Block.Para("Celý trik je zaokrouhlení dolů na násobek magického čísla. Všechno napravo od zajímavého oktetu je prostor pro uzly a jednoduše běží od 0 do 255."),
            Block.Note("Když prefix padne přesně na hranici oktetu — /8, /16, /24 — žádný částečný oktet není. Bloky se pak posouvají po 1 v oktetu za poslední plnou 255."),
        ),
    ),

    Lesson(
        title = "VLSM — podsítě různých velikostí",
        summary = "Metoda a příklad, na kterém se vždycky učí",
        body = listOf(
            Block.Para("Rozdělit síť na stejně velké kusy je snadné, ale plýtvavé. Když má jedno oddělení 60 strojů a druhé 7, dát oběma /26 zahodí 55 adres. Masky proměnné délky (VLSM) dají každé podsíti jen ten blok, který doopravdy potřebuje."),
            Block.Para("Metoda má čtyři kroky a záleží na jejich pořadí:"),
            Block.Bullets(listOf(
                "Seřaď podsítě podle velikosti, od největší.",
                "U každé: uzly + 2, zaokrouhli nahoru na nejbližší mocninu dvojky a ten exponent je počet bitů uzlu. Prefix = 32 − bity uzlu.",
                "Umísti ji na první volnou adresu. Když jdeš odshora dolů, ta adresa je vždycky už násobkem velikosti bloku.",
                "Posuň kurzor o velikost bloku a opakuj.",
            )),
            Block.Para("Vezmi 10.0.0.0/24 rozdělenou na A: 60 uzlů, B: 30, C: 14, D: 7."),
            Block.Para("A potřebuje 60 + 2 = 62 adres. 2^5 = 32 je málo, 2^6 = 64 stačí, takže 6 bitů uzlu a /26 — blok o 64 adresách začínající na 10.0.0.0."),
            Block.Para("B potřebuje 32. 2^5 = 32 přesně, tedy /27. Kurzor je na 10.0.0.64, což je násobek 32, takže B přistane tam."),
            Block.Para("C potřebuje 16 → /28 na 10.0.0.96. D potřebuje 9, což se stejně zaokrouhlí na 16 → /28 na 10.0.0.112."),
            Block.Table(listOf(
                "A  ·  60 uzlů" to "10.0.0.0/26 · .1 – .62 · bc .63",
                "B  ·  30 uzlů" to "10.0.0.64/27 · .65 – .94 · bc .95",
                "C  ·  14 uzlů" to "10.0.0.96/28 · .97 – .110 · bc .111",
                "D  ·  7 uzlů" to "10.0.0.112/28 · .113 – .126 · bc .127",
            )),
            Block.Para("Využito je 128 z 256 adres a 10.0.0.128 – 10.0.0.255 zůstává volných — místo na další /25, nebo na to, aby se všechna čtyři oddělení zdvojnásobila."),
            Block.Note("Zajímavá je D. Sedm uzlů potřebuje 9 adres a 2^3 = 8 je o jednu málo, takže D dostane /28 přesně jako C. Právě zaokrouhlování nahoru stojí ty adresy a nejde se mu vyhnout."),
        ),
    ),

    Lesson(
        title = "Kde to obvykle drhne",
        summary = "Šest chyb, které se vyplatí poznat",
        body = listOf(
            Block.Bullets(listOf(
                "Zapomenuté +2. Do /26 se vejde 62 uzlů, ale 62 adres ne — potřebuješ jich 64.",
                "Řazení od nejmenší. Velký blok pak nemá kam zarovnaně přistát a místo mezi tím přijde vniveč.",
                "Začátek bloku na adrese, která není násobkem jeho velikosti. /28 smí začínat na .96 nebo .112, nikdy na .100.",
                "Čtení masky místo prefixu. 255.255.255.192 je /26, ne /24 — počítej jedničky, ne dvěstěpadesátpětky.",
                "Předpoklad, že broadcast vždycky končí na 255. V /26 je to .63, .127, .191 nebo .255.",
                "Odpověď 2 použitelné uzly u /31. V konfiguraci routeru správně, u zkoušky skoro vždycky špatně.",
            )),
            Block.Note("Když plán vypadá správně, ověř ho dvěma způsoby: každá adresa sítě má být násobkem své vlastní velikosti bloku a broadcast každé podsítě má být přesně o jedna menší než adresa sítě té následující."),
        ),
    ),
)
