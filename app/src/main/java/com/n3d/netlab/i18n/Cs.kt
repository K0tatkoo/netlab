package com.n3d.netlab.i18n

import com.n3d.netlab.core.AnalyzeStep
import com.n3d.netlab.core.Ip
import com.n3d.netlab.core.VlsmStep

object Cs : Strings {

    override val lang = Lang.Cs

    // ---- formatting ----------------------------------------------------------

    override fun num(value: Long): String {
        val s = value.toString()
        if (s.length <= 4) return s
        // Czech groups thousands with a non-breaking space, not a comma — a
        // comma here would read as a decimal point.
        return s.reversed().chunked(3).joinToString(" ").reversed()
    }

    /**
     * Czech has three plural forms, and picking the wrong one is the single
     * loudest sign of a machine translation. 1 → singular, 2–4 → nominative
     * plural, everything else (including 0 and 5+) → genitive plural.
     */
    private fun plural(n: Long, one: String, few: String, many: String) = when {
        n == 1L -> one
        n in 2L..4L -> few
        else -> many
    }

    override fun hosts(count: Long) = "${num(count)} ${plural(count, "uzel", "uzly", "uzlů")}"
    override fun addresses(count: Long) = "${num(count)} ${plural(count, "adresa", "adresy", "adres")}"
    override fun subnets(count: Int) =
        "$count ${plural(count.toLong(), "podsíť", "podsítě", "podsítí")}"
    override fun bits(count: Int) = "$count ${plural(count.toLong(), "bit", "bity", "bitů")}"

    /** Locative: "ve třetím oktetu". */
    private fun ordinalLoc(n: Int) = when (n) {
        1 -> "prvním"; 2 -> "druhém"; 3 -> "třetím"; else -> "čtvrtém"
    }

    /** Nominative: "třetí oktet je zajímavý". */
    private fun ordinalNom(n: Int) = when (n) {
        1 -> "první"; 2 -> "druhý"; 3 -> "třetí"; else -> "čtvrtý"
    }

    // ---- navigation ----------------------------------------------------------

    override val tabPractice = "Procvičování"
    override val tabCalculator = "Kalkulačka"
    override val tabLearn = "Teorie"
    override val tabSettings = "Nastavení"

    // ---- shared vocabulary ---------------------------------------------------

    override val actionCheck = "Zkontrolovat"
    override val actionSolution = "Krok za krokem"
    override val actionHideSolution = "Skrýt řešení"
    override val actionNewExercise = "Nové cvičení"
    override val actionNext = "Další"
    override val actionBack = "Zpět"
    override val actionReset = "Vymazat"
    override val actionClose = "Zavřít"
    override val actionAdd = "Přidat"
    override val actionShowAll = "Všechny kroky"
    override val actionOneByOne = "Po krocích"
    override val actionFillCorrect = "Doplnit odpovědi"
    override val actionClear = "Vymazat"
    override val actionConfirm = "Vynulovat"
    override val actionCancel = "Zrušit"

    override val labelCorrect = "Správně"
    override val labelWrong = "Špatně"
    override val labelExpected = "Správná odpověď"
    override val labelSolved = "Vyřešeno"
    override val labelStreak = "Série"
    override val labelBest = "Rekord"
    override val labelDifficulty = "Obtížnost"
    override val diffEasy = "Lehká"
    override val diffMedium = "Střední"
    override val diffHard = "Těžká"

    override val fieldNetwork = "Adresa sítě"
    override val fieldPrefix = "Prefix"
    override val fieldMask = "Maska podsítě"
    override val fieldFirstHost = "První uzel"
    override val fieldLastHost = "Poslední uzel"
    override val fieldBroadcast = "Broadcast"
    override val fieldUsableHosts = "Použitelné uzly"
    override val fieldWildcard = "Wildcard maska"
    override val fieldBlockSize = "Velikost bloku"
    override val fieldHostBits = "Bity uzlů"
    override val fieldNetworkBits = "Bity sítě"
    override val fieldRange = "Rozsah"
    override val fieldTotalAddresses = "Celkem adres"
    override val fieldClass = "Třída"
    override val fieldScope = "Typ"
    override val fieldBinary = "Binárně"
    override val fieldSpare = "Rezerva"
    override val fieldAddress = "Adresa"

    override val classA = "Třída A"
    override val classB = "Třída B"
    override val classC = "Třída C"
    override val classD = "Třída D (multicast)"
    override val classE = "Třída E (rezervováno)"
    override val classLoopback = "Loopback"
    override val classThisNetwork = "Tato síť"

    override val scopePrivate = "Privátní (RFC 1918)"
    override val scopePublic = "Veřejná"
    override val scopeLoopback = "Loopback"
    override val scopeLinkLocal = "Link-local"
    override val scopeCgnat = "Carrier-grade NAT"
    override val scopeMulticast = "Multicast"
    override val scopeReserved = "Rezervovaná"

    // ---- practice ------------------------------------------------------------

    override val practiceTitle = "Procvičování"
    override val kindVlsm = "Rozdělení sítě"
    override val kindAnalyze = "Rozbor adresy"
    override val kindVlsmHint = "Rozděl jednu síť na podsítě různých velikostí"
    override val kindAnalyzeHint = "Jedna adresa a prefix — dopočítej zbytek"

    override val assignment = "Zadání"
    override val baseNetwork = "Výchozí síť"
    override val givenAddress = "Zadáno"
    override val vlsmPrompt = "Rozděl výchozí síť tak, aby každá podsíť níže dostala aspoň tolik uzlů, kolik si žádá, a aby se plýtvalo co nejméně."
    override val analyzePrompt = "Dopočítej všechno o síti, do které tahle adresa patří."
    override val yourAnswer = "Tvoje odpověď"

    override fun requirement(name: String, hostCount: Int) = "$name — ${hosts(hostCount.toLong())}"
    override fun subnetTitle(name: String) = "Podsíť $name"

    override val verdictPerfect = "Všechno správně."
    override fun verdictWrong(wrong: Int, total: Int) = "Špatně: $wrong z $total polí."
    override val verdictIncomplete = "Některá pole jsou ještě prázdná."
    override val verdictNothing = "Nejdřív něco vyplň."
    override val notCheckedYet = "Zatím nezkontrolováno"

    // ---- walkthrough ---------------------------------------------------------

    override val solutionTitle = "Krok za krokem"
    override fun stepOf(index: Int, total: Int) = "Krok $index z $total"

    override val stepIntroTitle = "Co máš zadáno"

    override fun stepIntroBody(step: VlsmStep.Intro): List<String> {
        val last = step.base + step.capacity - 1
        val verdict = if (step.demand <= step.capacity) {
            "${num(step.demand)} ≤ ${num(step.capacity)}, zadání se tedy vejde — a ještě zbyde ${addresses(step.capacity - step.demand)}."
        } else {
            "${num(step.demand)} > ${num(step.capacity)}, zadání se tedy nevejde. Něco bude muset zůstat stranou."
        }
        return listOf(
            "Výchozí síť je ${Ip.cidr(step.base, step.basePrefix)}. Její maska je ${Ip.format(Ip.mask(step.basePrefix))} a pokrývá ${addresses(step.capacity)} — od ${Ip.format(step.base)} do ${Ip.format(last)}.",
            "Úplně první z nich je adresa sítě a úplně poslední je broadcast, uzel tedy může dostat ${num(step.usable)} z nich.",
            "Musíš do ní vměstnat ${subnets(step.subnetCount)}. Dohromady si žádají ${hosts(step.requestedHosts.toLong())} — jenže každá podsíť si ještě musí zaplatit vlastní adresu sítě a broadcast a pak zaokrouhlit nahoru na mocninu dvojky, takže skutečná cena je ${addresses(step.demand)}.",
            verdict,
        )
    }

    override val stepOrderTitle = "Seřaď je od největší"

    override fun stepOrderBody(step: VlsmStep.Order): List<String> = listOf(
        "Seřaď podsítě podle velikosti, od největší: ${step.ordered.joinToString(", ") { "${it.name} (${num(it.hosts.toLong())})" }}.",
        "Není to úklid, ale samotná metoda. Blok o 2^n adresách smí začínat jen na adrese, která je násobkem 2^n — /26 na 0, 64, 128 nebo 192, nikdy na 16.",
        "Když přidělíš malou podsíť první, ta velká pak nemá kam zarovnaně začít, musíš přeskočit dopředu a všechno přeskočené je ztracené. Když jdeš od největší, další volná adresa je pokaždé už zarovnaná na to, co má přijít.",
    )

    override fun stepSizeTitle(step: VlsmStep.Size) = "${subnetTitle(step.alloc.requirement.name)} — jak velká?"

    override fun stepSizeBody(step: VlsmStep.Size): List<String> {
        val a = step.alloc
        val needed = a.requirement.hosts + 2
        val smaller = 1L shl (a.hostBits - 1)
        return listOf(
            "${a.requirement.name} si žádá ${hosts(a.requirement.hosts.toLong())}.",
            "Přičti dvě: jedna adresa padne na adresu sítě a jedna na broadcast a ani jednu nemůže dostat stroj. Podsíť tedy musí pojmout ${a.requirement.hosts} + 2 = $needed adres.",
            "Teď vezmi nejmenší mocninu dvojky, která je aspoň $needed. 2^${a.hostBits - 1} = ${num(smaller)} je málo; 2^${a.hostBits} = ${num(a.blockSize)} stačí. To je ${bits(a.hostBits)} prostoru pro uzly.",
            "Prefix je to, co zbyde: 32 − ${a.hostBits} = /${a.prefix}, maska ${Ip.format(a.mask)}. Blok je široký ${addresses(a.blockSize)}, z toho ${num(a.usable)} použitelných — ${num(a.spare)} v rezervě.",
        )
    }

    override fun stepPlaceTitle(step: VlsmStep.Place) = "${subnetTitle(step.alloc.requirement.name)} — kam?"

    override fun stepPlaceBody(step: VlsmStep.Place): List<String> {
        val a = step.alloc
        val (octetIndex, magic) = Ip.magicNumber(a.prefix)
        val octetValue = Ip.octet(a.network, octetIndex - 1)
        val boundaries = generateSequence(0L) { it + magic }.takeWhile { it < 256 }.toList()
        val boundaryText = if (boundaries.size <= 5) {
            boundaries.joinToString(", ")
        } else {
            boundaries.take(4).joinToString(", ") + ", …"
        }
        return listOf(
            "První volná adresa je ${Ip.format(step.cursorBefore)}.",
            "/${a.prefix} je široký ${addresses(a.blockSize)}, smí tedy začít jen tam, kde je ${ordinalNom(octetIndex)} oktet násobkem ${num(magic)} — $boundaryText — a všechny oktety za ním jsou nulové. První volná adresa to už splňuje, protože jdeš od největší podsítě dolů.",
            "${a.requirement.name} je tedy ${Ip.cidr(a.network, a.prefix)}, její ${ordinalNom(octetIndex)} oktet je $octetValue.",
            "Broadcast je poslední adresa v bloku: ${Ip.format(a.network)} + ${num(a.blockSize)} − 1 = ${Ip.format(a.broadcast)}. Všechno mezi oběma konci je použitelné, uzly tedy běží ${Ip.format(a.firstHost)} – ${Ip.format(a.lastHost)}.",
            "Další volná adresa je ${Ip.format(step.cursorAfter)}. Tam začíná další podsíť.",
        )
    }

    override fun stepOverflowTitle(step: VlsmStep.Overflow) =
        "${subnetTitle(step.requirement.name)} se nevejde"

    override fun stepOverflowBody(step: VlsmStep.Overflow): List<String> = listOf(
        "${step.requirement.name} si žádá ${hosts(step.requirement.hosts.toLong())}, což se zaokrouhlí na blok o ${addresses(step.needed)}.",
        "Ve výchozí síti zbývá jen ${addresses(step.remaining)} a blok nelze rozdělit přes její konec.",
        "Buď musí být výchozí síť větší, nebo si tahle podsíť musí říct o méně.",
    )

    override val stepVerifyTitle = "Zkontroluj plán"

    override fun stepVerifyBody(step: VlsmStep.Verify): List<String> {
        val p = step.plan
        val lines = mutableListOf(
            "Sečti bloky: rozdáno je ${num(p.used)} z ${num(p.capacity)} adres.",
        )
        lines += if (p.free > 0) {
            "Nahoře v rozsahu zbývá ${addresses(p.free)} nevyužitých, ${Ip.format(p.nextFree)} – ${Ip.format(p.endExclusive - 1)}. Tam by šla pátá podsíť nebo pozdější rozšíření."
        } else {
            "Nezbylo nic — výchozí síť je využitá do poslední adresy."
        }
        lines += "Uvnitř podsítí je ${num(p.wasted)} použitelných adres, které si nikdo nenárokuje. To je cena za zaokrouhlování každé podsítě nahoru na mocninu dvojky a je to normální."
        lines += "Správnost plánu drží dvě věci a obě se vyplatí zkontrolovat ručně: každá adresa sítě je násobkem své vlastní velikosti bloku a žádné dva rozsahy se nepřekrývají. Když čteš seznam shora dolů, broadcast každé podsítě je přesně o jedna menší než adresa sítě té následující."
        return lines
    }

    override fun analyzeStepTitle(step: AnalyzeStep): String = when (step) {
        is AnalyzeStep.Mask -> "Napiš masku"
        is AnalyzeStep.Magic -> "Najdi magické číslo"
        is AnalyzeStep.Network -> "Zaokrouhli dolů na adresu sítě"
        is AnalyzeStep.Broadcast -> "Najdi broadcast"
        is AnalyzeStep.Hosts -> "První a poslední uzel"
        is AnalyzeStep.Count -> "Kolik uzlů se vejde"
    }

    override fun analyzeStepBody(step: AnalyzeStep): List<String> = when (step) {
        is AnalyzeStep.Mask -> {
            val t = step.task
            listOf(
                "/${t.prefix} znamená, že prvních ${bits(t.prefix)} adresy určuje síť a zbývajících ${bits(t.hostBits)} určuje uzel v ní.",
                "Napiš tedy ${t.prefix} jedniček a za ně ${t.hostBits} nul a rozděl těch 32 bitů na čtyři oktety po osmi: ${Ip.toBinary(t.mask)}.",
                "Každý oktet převeď zpátky do desítkové soustavy a maska je ${Ip.format(t.mask)}.",
            )
        }

        is AnalyzeStep.Magic -> {
            val t = step.task
            val (octetIndex, magic) = t.magic
            val maskOctet = Ip.octet(t.mask, octetIndex - 1)
            if (t.hostBits % 8 == 0) {
                listOf(
                    "/${t.prefix} padne přesně na hranici oktetu, takže každý oktet masky je buď 255, nebo 0 a žádný částečný oktet tu není.",
                    "To je ten snadný případ: bloky se posouvají po 1 v ${ordinalLoc(octetIndex)} oktetu a všechny oktety za ním běží celé od 0 do 255.",
                )
            } else {
                listOf(
                    "Najdi oktet, kde maska není ani 255, ani 0 — zajímavý oktet. Tady je to ${ordinalNom(octetIndex)} a je v něm $maskOctet.",
                    "Magické číslo je 256 − $maskOctet = ${num(magic)}. Je to velikost bloku měřená v tomhle oktetu a bloky můžou začínat jen na jeho násobcích: ${generateSequence(0L) { it + magic }.takeWhile { it < 256 }.take(5).joinToString(", ")}${if (256 / magic > 5) ", …" else ""}.",
                    "Všechno za zajímavým oktetem je prostor pro uzly a běží celé od 0 do 255.",
                )
            }
        }

        is AnalyzeStep.Network -> {
            val t = step.task
            val (octetIndex, magic) = t.magic
            val octetValue = Ip.octet(t.address, octetIndex - 1)
            listOf(
                "Adresa je ${Ip.format(t.address)}. Její ${ordinalNom(octetIndex)} oktet je $octetValue.",
                "Zaokrouhli ho dolů na nejbližší násobek ${num(magic)}: $octetValue ÷ ${num(magic)} = ${octetValue / magic} zbytek ${octetValue % magic}, takže ${octetValue / magic} × ${num(magic)} = ${num(step.multiple)}. Všechny oktety za ním nastav na 0.",
                "Adresa sítě = ${Ip.format(t.network)}.",
                "Stejný výsledek delší cestou, když chceš vidět proč: postav adresu proti masce a udělej bitový součin AND — bit přežije jen tam, kde má maska jedničku.",
            )
        }

        is AnalyzeStep.Broadcast -> {
            val t = step.task
            val (octetIndex, magic) = t.magic
            val netOctet = Ip.octet(t.network, octetIndex - 1)
            listOf(
                "Broadcast je prostě poslední adresa v bloku: adresa sítě + ${num(t.blockSize)} − 1.",
                "Ručně je rychlejší zkratka: přičti k ${ordinalLoc(octetIndex)} oktetu ${num(magic)} − 1 = ${num(magic - 1)}, vyjde ${netOctet + magic - 1}, a všechny oktety za ním nastav na 255.",
                "Broadcast = ${Ip.format(t.broadcast)}.",
            )
        }

        is AnalyzeStep.Hosts -> {
            val t = step.task
            if (t.prefix >= 31) {
                listOf(
                    "/${t.prefix} nemá v klasickém pojetí žádný použitelný rozsah: /31 obsahuje jen adresu sítě a broadcast, /32 je jediná adresa.",
                    "V praxi se /31 používá na point-to-point spojích, kde RFC 3021 dovoluje, aby obě adresy nesly router. Zkoušky ale skoro vždy chtějí klasickou odpověď nula.",
                )
            } else {
                listOf(
                    "Samotnou adresu sítě nelze přidělit stroji, první uzel je tedy o jednu výš: ${Ip.format(t.network)} + 1 = ${Ip.format(t.firstHost!!)}.",
                    "Broadcast taky ne, poslední uzel je tedy o jednu níž: ${Ip.format(t.broadcast)} − 1 = ${Ip.format(t.lastHost!!)}.",
                    "Použitelný rozsah: ${Ip.format(t.firstHost!!)} – ${Ip.format(t.lastHost!!)}.",
                )
            }
        }

        is AnalyzeStep.Count -> {
            val t = step.task
            listOf(
                "Prostor pro uzly má ${bits(t.hostBits)}, blok tedy pojme 2^${t.hostBits} = ${addresses(t.blockSize)}.",
                "Odečti adresu sítě a broadcast: ${num(t.blockSize)} − 2 = ${num(t.usable)} použitelných uzlů.",
                "Ověř si to z druhé strany — od ${Ip.format(t.firstHost ?: t.network)} do ${Ip.format(t.lastHost ?: t.broadcast)} vyjde stejných ${num(t.usable)}.",
            )
        }
    }

    override fun bitsCaption(networkBits: Int) = when {
        networkBits == 1 -> "první bit = síť, zbytek = uzel"
        networkBits in 2..4 -> "první $networkBits bity = síť, zbytek = uzel"
        else -> "prvních $networkBits bitů = síť, zbytek = uzel"
    }

    override val bitsLegendNetwork = "síť"
    override val bitsLegendHost = "uzel"

    // ---- rows ----------------------------------------------------------------

    override val rowSubnetsRequired = "Počet podsítí"
    override val rowHostsRequested = "Žádané uzly"
    override val rowAddressesNeeded = "Potřeba adres"
    override val rowHostsRequired = "Požadované uzly"
    override val rowPlusTwo = "+ síť a broadcast"
    override val rowFirstFree = "První volná adresa"
    override val rowNextFree = "Další volná adresa"
    override val rowUsed = "Rozděleno"
    override val rowFree = "Zbývá"
    override val rowWasted = "Nevyužito v podsítích"
    override val rowMagicOctet = "Zajímavý oktet"
    override val rowMagicNumber = "Magické číslo"
    override val rowRoundedDown = "Zaokrouhleno na"

    // ---- calculator ----------------------------------------------------------

    override val calculatorTitle = "Kalkulačka"
    override val calcModeSubnet = "Podsíť"
    override val calcModeDesigner = "Plán VLSM"
    override val calcInvalidAddress = "Neplatná IPv4 adresa"
    override val calcEnterAddress = "Zadej adresu a uvidíš čísla"
    override val designerBase = "Výchozí síť"
    override val designerSubnets = "Podsítě"
    override val designerAddSubnet = "Přidat podsíť"
    override val designerRemove = "Odebrat"
    override val designerEmpty = "Přidej podsíť a vznikne plán."
    override fun designerFits(used: Long, capacity: Long) =
        "Využito ${num(used)} z ${num(capacity)} adres"
    override fun designerOverflow(count: Int) = "Nevejde se: ${subnets(count)}"
    override val designerPlan = "Plán"
    override val designerHostsFor = "Uzly"

    // ---- learn ---------------------------------------------------------------

    override val learnTitle = "Teorie"
    override val lessons = csLessons

    // ---- settings ------------------------------------------------------------

    override val settingsTitle = "Nastavení"
    override val settingLanguage = "Jazyk"
    override val settingTheme = "Vzhled"
    override val themeSystem = "Systém"
    override val themeLight = "Světlý"
    override val themeDark = "Tmavý"
    override val settingDefaults = "Procvičování"
    override val settingDefaultKind = "Typ cvičení"
    override val settingDefaultDifficulty = "Obtížnost"
    override val settingShortFields = "Krátké odpovědi"
    override val settingShortFieldsHint = "Ptát se jen na adresu sítě a prefix, ne na celý rozsah."
    override val settingStats = "Statistika"
    override val settingResetStats = "Vynulovat statistiku"
    override val settingResetStatsHint = "Smaže počet vyřešených cvičení i obě série."
    override val settingAbout = "O aplikaci"
    override val aboutBody = "NetLab — procvičování dělení IPv4 sítí a VLSM. Všechno se počítá přímo v telefonu; aplikace nemá oprávnění k síti a nikam nic neposílá."
    override val languageEnglish = "English"
    override val languageCzech = "Čeština"
}
