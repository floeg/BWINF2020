package de.loegler.bwinf.spiessgesellen.main

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.swing.JOptionPane

var verbose: Boolean = false

fun main(args: Array<String>) {
    verbose = args.any { it.contains("verbose") || it.contains("v") }
    var path =
        JOptionPane.showInputDialog("Bitte geben Sie die URL des Beispiels an. Für lokale Dateien ist der Präfix 'file:\\' erforderlich.")
    if (path == null || path.isEmpty()) {
        path = "https://bwinf.de/fileadmin/bundeswettbewerb/39/spiesse1.txt"
    } else if (path.length == 1) {
        try {
            var nr = path.toInt()
            path = "https://bwinf.de/fileadmin/bundeswettbewerb/39/spiesse$nr.txt"
        } catch (e: Exception) {
        }
    }

    var reader = BufferedReader(InputStreamReader(URL(path).openStream()))

    var maxAmount = reader.readLine().toInt()
    var desiredFruits = reader.readLine().split(" ").toList().filter { it.isNotEmpty() }
    var numberObserved = reader.readLine().toInt()
    var observedList = mutableListOf<Pair<List<Int>, List<String>>>()

    for (i in 0 until numberObserved) {
        var observedBowls = reader.readLine().split(" ").toList().filter { it.isNotEmpty() }.map { it.toInt() }
        var observedFruits = reader.readLine().split(" ").toList().filter { it.isNotEmpty() }
        observedList.add(Pair(observedBowls, observedFruits))
    }

    Spiessgesellen(maxAmount, desiredFruits, observedList)
}

/**
 * @param maxAmount Obergrenze an Obstsorten - maximale Anzahl an Schüsseln
 * @param desiredFruits Liste an Wunschsorten von Donald
 * @param observedPairList Beobachtete Spiesse mit dazugehoerigen Schuesseln
 */
class Spiessgesellen(
    val maxAmount: Int,
    val desiredFruits: List<String>,
    val observedPairList: List<Pair<List<Int>, List<String>>>
) {

    var graph = Graph<VertexWrapper>()

    /**
     * Alle Obstsorten, welche beobachtet wurden
     */
    val observedFruitSet: Set<String>
    /**
     * Alle erwähnten Obstsorten (inklusive Wunschsorten)
     */
    val allFruitSet: Set<String>
    val observedBowlIDs: Set<Int>
    /**
     * Liste mit Früchten, welche noch (eindeutig) zugeordnet werden müssen / können.
     */
    val fruitVerticesToMatch: MutableList<Vertex<VertexWrapper>> = mutableListOf()

    /**
     * Liste mit IDs, welche noch (eindeutig) zugeordnet werden müssen / können.
     */
    val bowlsVToMatch: MutableList<Vertex<VertexWrapper>> = mutableListOf()

    /**
     * Liste mit IDs der Schüsseln sowie zugehörigen Obstsorten.
     * Ist first -1, so konnte kein Partner gefunden werden.
     */
    val fruitsMatched: List<Pair<Int, String>> = mutableListOf()


    init {
        val fruitListFirst: List<String> = observedPairList.map { it.second }.flatten()
        val allFruits = mutableListOf<String>()
        allFruits.addAll(desiredFruits)
        allFruits.addAll(fruitListFirst)

        //Alle Obstsorten, welche auf einem der Spieße waren
        observedFruitSet = fruitListFirst.toSet()

        //Alle Obstsorten welche in der Datei erwähnt wurden
        allFruitSet = allFruits.toSet()

        observedBowlIDs = observedPairList.flatMap { it.first }.toSet()

        //Füge für jede beobachtete Obstsorte und Schüssel einen Knoten zum Graphen hinzu
        observedFruitSet.forEach {
            var wrapper = VertexWrapper(it, false)
            var tmpVertex = graph.addVertex(wrapper)
            fruitVerticesToMatch.add(tmpVertex)
        }
        observedBowlIDs.forEach {
            var wrapper = VertexWrapper("$it", true)
            var tmpVertex = graph.addVertex(wrapper)
            bowlsVToMatch.add(tmpVertex)
        }

        //Teil 1. des Algorithmus - Verarbeitung der Beobachtungen
        matchFruits()
        //Teil 2. des Algorithmus - Entfernen von weiteren Kanten (Schleife)
        match2()

        //Teil 3. des Algorithmus - Hinzufügen von nicht beobachteten Schüsseln und Obstsorten
        addUnobserved()
        //Teil 4. des Algorithmus - Herausfinden, ob eindeutige Lösung vorhanden & Ausgabe
        printResult()
    }


    fun matchFruits() {
        for (currentObservedPair in observedPairList) {
            var currentIDList = currentObservedPair.first
            var currentFruitList = currentObservedPair.second

            for (singleFruitName in currentFruitList) {
                /*Suche nach dem Knoten in der Liste, welcher die aktuelle Frucht repräsentiert
                  Ist null, falls die Frucht bereits zugeordnet wurde - in diesem Fall passiert nichts.
                 */
                var fruitVertex: Vertex<VertexWrapper>? =
                    fruitVerticesToMatch.find { it.value.name == singleFruitName }
                if (fruitVertex != null) {
                    /*
                    Zwei Fälle (aufgrund der Optimierung):
                    Keine Kanten: Erstes mal, dass die Frucht beobachtet wurde
                                  Erstellen von Kanten
                    Bereits Kanten vorhanden: Entfernen von allen Kanten, welche auf Schüsseln zeigen,
                                              Welche hier nicht beobachtet wurden
                     */
                    var neighbours = graph.getNeighbours(fruitVertex)

                    //Nur Knoten der Partitionsklasse A, welche im aktuellen Durchlauf beobachtet wurden
                    var currentIDsToMatch: List<Vertex<VertexWrapper>> =
                        bowlsVToMatch.filter { currentIDList.contains(it.value.name.toInt()) }

                    //Keine Nachbarn: Fall 1: Erstmalig Kanten hinzufügen
                    if (neighbours.isEmpty()) {
                        for (id in currentIDsToMatch) {
                            graph.addEdge(fruitVertex, id)
                        }
                    }
                    //Fall 2: ggf. Kanten entfernen
                    else {
                        for (neighbour in neighbours) {
                            //Alle (ausgehenden) Kanten welche auf einen Knoten zeigen, welcher in diesem Durchlauf nicht beobachtet wurde
                            if (!currentIDsToMatch.contains(neighbour)) {
                                graph.removeEdgeBetween(fruitVertex, neighbour)
                            }
                        }
                    }

                    //Ist nur noch eine Kante vorhanden, so liegt eine eindeutige Zuordnung vor
                    if (graph.getEdges(fruitVertex).size <= 1)
                        fruitVerticesToMatch.remove(fruitVertex)
                }
            }
        }
    }

    fun match2() {
        var changed = true

        var debugCounter = 0
        while (changed) {
            changed = false
            //Verknüpft jede Schüssel mit jeder Frucht, zu der noch eine Verbindung besteht
            var bowlVToFruitVList =
                graph.vertices.filter { it.value.number }.associateWith { graph.getNeighbours(it) }

            for ((currentBowl, currentFruits) in bowlVToFruitVList) {
                //Hat currentBowl nur einen Nachbarn?
                if (currentFruits.size == 1) {
                    //Schüssel und Frucht wurden zugeordnet - können aus der Arbeitsliste entfernt werden
                    bowlsVToMatch.remove(currentBowl)
                    fruitVerticesToMatch.remove(currentFruits.first())
                    //Das Element in currentFruits hat ggf. noch andere Nachbarn, welche entfernt werden können
                    for (neighbour in graph.getNeighbours(currentFruits.first())) {
                        if (neighbour != currentBowl) {
                            graph.removeEdgeBetween(neighbour, currentFruits.first())
                            changed = true
                        }
                    }
                }
                //Mehrere Obstsorten könnten in der Schüssel sein
                else {
                    //Suche nach einer Frucht, welche nur die aktuelle Schüssel als mögliche Schüssel hat
                    for (fruitVertex in currentFruits) {
                        val fruitPossibleBowls = graph.getNeighbours(fruitVertex)

                        if (fruitPossibleBowls.size == 1) {
                            //Nur die aktuelle Schüssel kommt für die Frucht in Frage
                            if (fruitPossibleBowls.first() == currentBowl) {
                                //Eindeutig bestimmt - Entferne aus Arbeitsliste
                                bowlsVToMatch.remove(currentBowl)
                                fruitVerticesToMatch.remove(fruitVertex)
                                //currentBowl kann dementsprechend keine weiteren Früchte enthalten - entferne daher andere Kanten
                                for (fruit in graph.getNeighbours(currentBowl)) {
                                    if (fruit != fruitVertex) {
                                        changed = true
                                        graph.removeEdgeBetween(fruit, currentBowl)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Füge nicht beobachtete Schüsseln und Früchte hinzu
     */
    fun addUnobserved() {
        val observedFruitAmount = observedFruitSet.size
        val maxFruits = maxAmount
        /*Wenn die Obergrenze an Früchten über der Anzahl aller bekannten Früchte liegt, so sind Früchte ohne Namen vorhanden
        * Beträgt die Differenz 0, so sind keine Unbekannten Früchte vorhanden.
        */
        val unknownFruitsAmount = maxFruits - allFruitSet.size
        var notObservedFruits = allFruitSet.filterNot { observedFruitSet.contains(it) }
        if (unknownFruitsAmount != 0) {
            notObservedFruits += "Früchte ohne Namen ($unknownFruitsAmount *)"
        }
        var notObservedFruitVList = mutableListOf<Vertex<VertexWrapper>>()

        for (notObservedFruit in notObservedFruits) {
            var notObservedVertex = Vertex(VertexWrapper(notObservedFruit, false))
            notObservedFruitVList.add(notObservedVertex)
            graph.addVertex(notObservedVertex)
        }

        for (bowlNR in 1..maxFruits) {
            val bowlObserved: Boolean = observedBowlIDs.find { it == bowlNR } != null
            if (!bowlObserved) {
                var notObservedNumberVertex = Vertex(VertexWrapper(bowlNR.toString(), true))
                graph.addVertex(notObservedNumberVertex)
                //Füge Kanten zu allen nicht beobachteten Früchten hinzu
                notObservedFruitVList.forEach {
                    graph.addEdge(notObservedNumberVertex, it)
                }

            }
        }
    }

    fun printResult() {
        if (verbose) {
            println("Der Verbose-Modus ist aktiviert. Es werden nun alle Zuordnungen ausgegeben.")
            printVerbose()
            println("Nun folgt die eigentliche Ausgabe des Programms")
        }

        //Filtert alle Knoten, welche Wunschsorten sind
        val desiredFruitsV: List<Vertex<VertexWrapper>> =
            graph.vertices.filter { desiredFruits.contains(it.value.name) }
        val bowls = mutableSetOf<Int>()
        val multipleToPrint = mutableListOf<String>()

        for (desiredFruitV in desiredFruitsV) {
            val canBeInBowls = graph.getNeighbours(desiredFruitV)
            if (canBeInBowls.size == 1) {
                bowls.add(canBeInBowls.first().value.name.toInt())
            } else {
                //desiredFruitV kann in mehren Schüsseln sein
                for (possibleBowl in canBeInBowls) {
                    val neighboursOfPBowl = graph.getNeighbours(possibleBowl)
                    //Prüft, ob alle Früchte, welche in possibleBowl sind Wunschsorten von Donald sind
                    if (neighboursOfPBowl.all { desiredFruitsV.contains(it) }) {
                        bowls.add(possibleBowl.value.name.toInt())
                    } else {
                        multipleToPrint.add("${desiredFruitV.value.name} könnte in ${possibleBowl.value.name} sein")
                        multipleToPrint.add(
                            "Unerwünschte Früchte: ${
                                neighboursOfPBowl.filterNot {
                                    desiredFruitsV.contains(
                                        it
                                    )
                                }.joinToString { it.value.name }
                            }"
                        )
                    }
                }
            }
        }

        if (multipleToPrint.size != 0) {
            println("Die Menge konnte nicht eindeutig bestimmt werden. Es folgen weitere Hinweise.")
        }
        println(bowls.joinToString { it.toString() })

        for (line in multipleToPrint)
            println(line)
    }


    fun printVerbose() {
        var notMentioned = allFruitSet.filterNot { observedFruitSet.contains(it) }

        if (notMentioned.isNotEmpty()) {
            println("Nicht erwähnte Früchte: ${notMentioned.joinToString { it }}")
        }

        //Gehe alle Schüsseln durch
        for (singleID in 1..maxAmount) {
            var numberVertex: Vertex<VertexWrapper>? =
                graph.vertices.find { it.value.number && it.value.name.toInt() == singleID }
            var notMentionedFruits = allFruitSet.filterNot { observedFruitSet.contains(it) }

            if (numberVertex == null) {
                println("$singleID kann sein: ${notMentionedFruits.toList().joinToString { it }}")

            } else {
                println("$singleID kann sein: ${graph.getNeighbours(numberVertex).joinToString { it.value.name }}")
            }
        }
    }
}

data class VertexWrapper(val name: String, val number: Boolean)