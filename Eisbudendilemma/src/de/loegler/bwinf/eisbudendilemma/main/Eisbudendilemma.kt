package de.loegler.bwinf.eisbudendilemma.main

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.swing.JOptionPane
import kotlin.math.min

var ausgabeLimit = 3

fun main(args: Array<String>) {
    var path: String? =
        JOptionPane.showInputDialog("Bitte geben Sie die URL des Beispiels an. Für lokale Dateien ist der Präfix 'file:\\' erforderlich.")
    if (path == null || path.isEmpty()) {
        path = "https://bwinf.de/fileadmin/bundeswettbewerb/39/eisbuden1.txt"
    } else if (path.length == 1) {
        try {
            var nr = path.toInt()
            path = "https://bwinf.de/fileadmin/bundeswettbewerb/39/eisbuden$nr.txt"
        } catch (e: Exception) {
        }
    }

    val limitArg = args.find { it.contains("limit") }?.split("limit")?.get(1)
    if (limitArg != null) {
        ausgabeLimit = limitArg.toInt()
    }

    var reader = BufferedReader(InputStreamReader(URL(path).openStream()))

    var infos = reader.readLine().split(" ")
    var umfang = infos[0].toInt()
    var anzahlHaeuser = infos[1].toInt()

    //Die Adressen, Filter zur Sicherheit, falls am Ende der Zeile ein Leerzeichen steht
    var adressenText = reader.readLine()
    var adressen: List<Int> = adressenText.split(" ")
        .filter { it.isNotEmpty() }.map { it.toInt() }

    Eisbudendilemma(umfang, adressen)
}

class Eisbudendilemma(val umfangSee: Int, val adressen: List<Int>) {
    val adressenGraph = Graph<Int>()
    val anzahlHaeuser = adressen.size
    val erstesHausVertex: Vertex<Int> = Vertex(adressen[0])
    val letztesHaus = Vertex(adressen[adressen.lastIndex])
    val adressenZuVertex: HashMap<Int, Vertex<Int>> = HashMap()

    init {
        adressenGraph.addVertex(erstesHausVertex)
        adressenZuVertex.put(erstesHausVertex.value, erstesHausVertex)
        var vorherigeAdresse = erstesHausVertex

        /*
        * Erstelle Verbindung zwischen den Adressen/Häusern
        * Erstes Haus wird außerhalb der Schleife behandelt
        * Zusätzliches vorgehen für die letzte Adresse außerhalb der Schleife
        */
        for (i in 1..adressen.lastIndex) {
            val hausVertex = Vertex(adressen[i])
            adressenGraph.addVertex(hausVertex)
            adressenZuVertex.put(hausVertex.value, hausVertex)
            //Berechne den Abstand zwischen den zwei Häusern
            val abstandAdressen = hausVertex.value - vorherigeAdresse.value
            adressenGraph.addEdge(vorherigeAdresse, hausVertex, abstandAdressen.toDouble())
            vorherigeAdresse = hausVertex
        }
        adressenZuVertex[letztesHaus.value] = letztesHaus

        // Da der See Kreisförmig ist gibt es eine Verbindung zwischen dem ersten und dem letzten Haus
        val abstandErstesLetzesHaus = (umfangSee - letztesHaus.value) + erstesHausVertex.value
        adressenGraph.addEdge(erstesHausVertex, letztesHaus, abstandErstesLetzesHaus.toDouble())
        sucheNachStandorten()
    }

    fun sucheNachStandorten() {
        // Zu beginn werden die Eisbuden auf die ersten drei Häuser verteilt
        val erstePositionen = Triple(adressen.component1(), adressen.component2(), adressen.component3())
        val ersteAbstaende = berechneAbstaende(erstePositionen)
        val startLoesung = MoeglicheLoesung(erstePositionen, ersteAbstaende)
        val nichtAussortierte = mutableListOf(startLoesung)
        // Aktuelle Kombination an Standorten, mindestens eine Eisbude wird in jedem Durchlauf verschoben
        var aktuelleLoesung = startLoesung.verschiebeUmEins()
        while (aktuelleLoesung != null) {
            // Annahme: nichtAussortierte.first() sind die aktuellen Positionen der Eisbuden
            val ergebnisAlteGegenAktuell = simuliereAbstimmung(nichtAussortierte.first(), aktuelleLoesung)
            // Die neue Kombination gewinnt mehr Ja-Stimmen
            if (ergebnisAlteGegenAktuell) {
                // Entfernen der alten Kombination, hinzufügen der neuen
                nichtAussortierte.add(aktuelleLoesung)
                nichtAussortierte.removeFirst()
            }
            // Kombination erreichte nicht mehr Ja Stimmen
            else {
                // Annahme: aktuelleLoesung sind Positionen von Eisbuden
                val ergebnisAktuellGegenAlt = simuliereAbstimmung(aktuelleLoesung, nichtAussortierte.first())
                // Wenn man nicht zur first() wechseln würde, könnte aktuell auch stabil sein
                if (!ergebnisAktuellGegenAlt) {
                    nichtAussortierte.add(aktuelleLoesung)
                }
            }
            // Wird null, falls alle Kombinationen überprüft wurden
            aktuelleLoesung = aktuelleLoesung.verschiebeUmEins()
        }

        // 2. Durchlauf: Sortiere alle möglichen stabilen Lösungen aus, welche in Wirklichkeit nicht stabil sind
        val stabileLoesungen = mutableListOf<MoeglicheLoesung>()
        for (moegliche in nichtAussortierte) {
            var istStabil = true
            var aktuelleZweiterDurchlauf: MoeglicheLoesung? = startLoesung
            /*
            * Wenn es eine Kombination aus Standorten gibt, welche gegen moegliche
            * mehr Ja-Stimmen erhalten würde, so ist moegliche nicht stabil
             */
            while (aktuelleZweiterDurchlauf != null) {
                val nichtStabil = simuliereAbstimmung(moegliche, aktuelleZweiterDurchlauf)
                istStabil = istStabil && !nichtStabil
                aktuelleZweiterDurchlauf = aktuelleZweiterDurchlauf.verschiebeUmEins()
            }
            // Ist er nach der Überprüfung aller Elemente immer noch als stabil markiert, so ist er es auch in wirklichkeit
            if (istStabil)
                stabileLoesungen.add(moegliche)
        }
        // Ausgabe - Fallunterscheidung, ob Lösung vorhanden
        if (stabileLoesungen.isEmpty()) {
            println("Es konnte kein stabiler Standort gefunden werden.")
        } else {
            for (i in 1..min(ausgabeLimit, stabileLoesungen.size)) {
                var positionen = stabileLoesungen[i].eisbudenPositionen
                println("Adressen der Eisbuden: $positionen")
            }
        }
    }

    /**
     * Gibt true zurueck, falls [neue] mehr Ja als Neinstimmen erhalten wuerde, falls [alte] die derzeitigen Positionen ist
     */
    fun simuliereAbstimmung(alte: MoeglicheLoesung, neue: MoeglicheLoesung): Boolean {
        var anzahlJa = neue.abstaendeHaeuser.count { neuesEntry ->
            val neuerAbstand = neuesEntry.value
            //Die Häuser sind immer konstant. Daher ist der Key auf jeden fall vorhanden
            val alterAbstand = alte.abstaendeHaeuser[neuesEntry.key]!!
            neuerAbstand < alterAbstand
        }

        return anzahlJa > (anzahlHaeuser / 2)
    }

    fun MoeglicheLoesung.verschiebeUmEins(): MoeglicheLoesung? {
        var aktuellDrittes = findeVertex(this.eisbudenPositionen.third)
        var aktuellZweites = findeVertex(this.eisbudenPositionen.second)
        var aktuellErstes = findeVertex(this.eisbudenPositionen.first)
        var nachfolgerDrittes: Vertex<Int> = adressenGraph.getNeighbours(aktuellDrittes).maxByOrNull { it.value }!!
        var nachfolgerErstes = aktuellErstes
        var nachfolgerZweites = aktuellZweites

        // Wahr, falls einer der Nachbarn das erste Haus ist - Alle Knoten wurden besucht
        if (aktuellDrittes.value > nachfolgerDrittes.value) {
            nachfolgerZweites = adressenGraph.getNeighbours(aktuellZweites).maxByOrNull { it.value }!!
            //Mit dem zweiten auch ans Ende angekommen
            if (aktuellZweites.value > nachfolgerZweites.value) {
                if (aktuellErstes == erstesHausVertex)
                    nachfolgerErstes = adressenGraph.getNeighbours(aktuellErstes).minByOrNull { it.value }!!
                else
                    nachfolgerErstes = adressenGraph.getNeighbours(aktuellErstes).maxByOrNull { it.value }!!
                //Alle Positionen bereits ausprobiert
                if (aktuellErstes.value > nachfolgerErstes.value) {
                    return null
                }
                aktuellErstes = nachfolgerErstes
                aktuellZweites = adressenGraph.getNeighbours(aktuellErstes).maxByOrNull { it.value }!!
            }
            aktuellDrittes = adressenGraph.getNeighbours(aktuellZweites).maxByOrNull { it.value }!!
            nachfolgerDrittes = aktuellDrittes
        }
        var positionen = Triple(nachfolgerErstes.value, nachfolgerZweites.value, nachfolgerDrittes.value)
        var abstaende = berechneAbstaende(positionen)
        val loesungRotiert = MoeglicheLoesung(positionen, abstaende)
        // Prüfe, ob zwei Eisbuden an der selben Adresse wären
        val neuePos = loesungRotiert.eisbudenPositionen
        val tmp = intArrayOf(neuePos.first, neuePos.second, neuePos.third)
        // Falls ja, gehe um eine Position weiter
        return if (tmp.toSet().size != 3)
            loesungRotiert.verschiebeUmEins()
        else
            loesungRotiert
    }

    fun berechneAbstaende(positionenEisbuden: Triple<Int, Int, Int>): HausZuAbstand {
        val abstaende = HausZuAbstand()

        val positionenArray = intArrayOf(positionenEisbuden.first, positionenEisbuden.second, positionenEisbuden.third)
        for (position in positionenArray) {
            // Alle Elemente in zuTesten wurden im aktuellen Durchgang bereits berechnet
            var zuTesten = mutableListOf<Vertex<Int>>()
            zuTesten.add(findeVertex(position))
            abstaende[zuTesten.first().value] = 0
            while (zuTesten.isNotEmpty()) {
                // Betrachte das aktuelle Element und entferne es aus der Liste
                val aktuellerVertex = zuTesten.removeFirst()
                val nachbarn: List<Vertex<Int>> = adressenGraph.getNeighbours(aktuellerVertex)
                for (nachbar in nachbarn) {
                    /*
                     * Berechne den Abstand zur nächsten Eisbude für die Nachbarn, falls der Weg zu ihr über aktuellerVertex verläuft
                     */
                    val kanteZuNachbar = adressenGraph.getEdgeBetween(nachbar, aktuellerVertex)!!
                    val neuerWegNachbar = abstaende[aktuellerVertex.value]!! + kanteZuNachbar.weight
                    val alterWegNachbar = abstaende.getOrDefault(nachbar.value, Int.MAX_VALUE)
                    // Falls der alte Weg besser sein sollte handelt es sich nicht um die nächste Eisbude
                    // Ansonsten wird der Abstand verändert. Weiterhin müssen die Nachbarn des Nachbarn betrachtet werden
                    if (neuerWegNachbar < alterWegNachbar) {
                        zuTesten.add(nachbar)
                        abstaende[nachbar.value] = neuerWegNachbar.toInt()
                    }
                }
            }
        }
        return abstaende
    }

    private fun findeVertex(adresse: Int) = adressenZuVertex[adresse]!!

    data class MoeglicheLoesung(val eisbudenPositionen: Triple<Int, Int, Int>, val abstaendeHaeuser: HausZuAbstand) {
    }

}

typealias HausZuAbstand = HashMap<Int, Int>
