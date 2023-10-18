package de.loegler.bwinf.tobistunier.main

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.HashMap
import javax.swing.JOptionPane
import kotlin.math.roundToInt
import kotlin.random.Random

var randomLiga = false;

fun main(array: Array<String>) {
    var inputPath = JOptionPane.showInputDialog("Bitte geben Sie die URL zur Beispieldatei ein. Für lokale Dateien ist der Prefix 'file:\\' notwendig.")
    if(inputPath==null || inputPath.isEmpty()){
        inputPath="https://bwinf.de/fileadmin/bundeswettbewerb/39/spielstaerken4.txt"
        JOptionPane.showMessageDialog(null,"Benutze Beispieldatei aus dem Internet")
    }
    var einteilung = JOptionPane.showInputDialog("Bitte geben Sie die gewünschte Einteilung der Turnierplaene ein (random,id,idmixed)")
    if(einteilung==null||einteilung.isEmpty()){
        einteilung="random"
    }

    var players =loadSpielstaerken(URL(inputPath))
    var ligaMap = HashMap<Player , Int >()
    var koMap = HashMap<Player,Int>()
    var ko5Map= HashMap<Player, Int>()
    var maxStrength = players.maxByOrNull { it.spielstaerke }!!.spielstaerke
    var strongestPlayers = mutableListOf<Player>()
    players.forEach { ligaMap[it]=0;ko5Map[it]=0;koMap[it]=0
        if(it.spielstaerke==maxStrength)
            strongestPlayers.add(it)
    }

    var times = 1_000_000
    for(i in 0..times) {
        var liga = Liga(players)
        liga.start()
        liga.printResult()
        var koSystem = KOSystem(players,einteilung)
        koSystem.start(1, players)
        var koSystem5 = KOSystem(players,einteilung)
        koSystem5.start(5, players)
        //Die Anzahl der Siege wird für jeden Gewinner um eins erhöht
        ligaMap[liga.winner!!] = ligaMap[liga.winner]!!+1
        koMap[koSystem.winner!!] = koMap[koSystem.winner]!!+1
        ko5Map[koSystem5.winner!!] = ko5Map[koSystem5.winner]!!+1
    }

    strongestPlayers.forEach { println("$it Liga: ${ligaMap[it]} KO: ${koMap[it]} KO5: ${ko5Map[it]}") }
    strongestPlayers.forEach { println("$it Liga: ${( ligaMap[it]!!.toDouble()/times*100).roundToInt()}%" +
            " KO: ${ (koMap[it]!!.toDouble()/times*100).roundToInt()}% KO5: ${(ko5Map[it]!!.toDouble()/times*100).roundToInt()}%") }


}

/**
 * Implementierung der Ligavariante KO sowie KO5
 */
class KOSystem(players: Array<Player>, turnierEinteilung : String){
    var turnierEinteilung:String = turnierEinteilung
    var playerWins = HashMap<Player,Int>()
    var groups = mutableListOf<Pair<Player,Player>>()
    var winner : Player? = null
    init {
        players.forEach { playerWins.put(it,0) }
    }

    /**
     *Startet die Simulation des Turniers.
    - times - Angabe wie oft pro Duell maximal gespielt werden soll - KO: 1, KO5: 5
     */
    fun start(times:Int, players : Array<Player>){
        //Einteilung der Gruppen
        when {
            turnierEinteilung.toLowerCase()=="id" -> createGroupsPlayerID(players)
            turnierEinteilung.toLowerCase()=="idmixed" -> createGroupsPlayerIDMixed(players)
            else -> createGroups(players)
        }

        var winners = mutableListOf<Player>()

        for(currentGroup in groups){
            var playerOne = currentGroup.first
            var playerTwo = currentGroup.second
            var playerOneWins =0
            var playerTwoWins = 0
            for(i in 0 until times){
                if((playerOne vs playerTwo) == playerOne)
                    playerOneWins++
                else playerTwoWins++

                if(playerOneWins>times/2 || playerTwoWins>times/2){
                    break
                }
            }
            var winner = if(playerOneWins>playerTwoWins) playerOne else playerTwo
            winners.add(winner)
        }


        if(winners.size==1){
           // println("Final winner: ${winners[0]}")
            winner=winners[0]
        }else{
        start(times,winners.toTypedArray())}

    }

    //Methoden zur Gruppeneinteilung

    /**
     * Teilt die Gruppen basierend auf ihrer Spielerid auf -> 1 mit 2, 3 mit 4...
     */
    private fun createGroupsPlayerID(tmpPlayers: Array<Player>){
        var playerList = mutableListOf<Player>()
        groups.clear()
        playerList.addAll(tmpPlayers)
        while(playerList.isNotEmpty()){
            var firstPlayer= playerList[0]
            var secondPlayer = playerList[1]
            playerList.removeAt(0)
            playerList.removeAt(0)
            groups.add(Pair(firstPlayer,secondPlayer))
        }
    }

    /**
     * Teilt die Gruppen basierend auf ihrer SpielerID auf -> 1 mit 8/16, 2 mit 7/15 ...
     */
    private fun createGroupsPlayerIDMixed(tmpPlayers: Array<Player>){
        var playerList = mutableListOf<Player>()
        groups.clear()
        playerList.addAll(tmpPlayers)
        while(playerList.isNotEmpty()){
            var firstPlayer= playerList[0]
            var secondPlayer = playerList[playerList.size-1]
            playerList.removeAt(0)
            playerList.removeAt(playerList.size-1)
            groups.add(Pair(firstPlayer,secondPlayer))
        }
    }


    private fun createGroups(tmpPlayers:Array<Player>){
        var playersList = mutableListOf<Player>()
        groups.clear()
        playersList.addAll(tmpPlayers)
        while(playersList.isNotEmpty()){
            var firstIndex = Random.nextInt(playersList.size)
            var firstPlayer = playersList[firstIndex]
            playersList.removeAt(firstIndex)
            var secondIndex = Random.nextInt(playersList.size)
            var secondPlayer = playersList[secondIndex]
            playersList.removeAt(secondIndex)
            groups.add(Pair(firstPlayer,secondPlayer))

        }
    }
}


class Liga(players:Array<Player>){
    var players=players
    var playerWins = HashMap<Player,Int>()
    var winner : Player? = null
    init {
        players.forEach { playerWins[it] = 0 }
    }
     fun start(){
        for(i in players.indices)
        {
            var firstPlayer = players[i]
            for(z in i+1 until players.size){ //Spieler bis i. haben schon gegen jeden anderen gespielt
                var secondPlayer = players[z]
                var winningPlayer = firstPlayer vs secondPlayer
                playerWins[winningPlayer]=playerWins[winningPlayer]!!+1
            }
        }
    }

     fun printResult(){
        var maxWins = playerWins.maxByOrNull { it.value }!!.value
        var playersWithMaxWins = mutableListOf<Player>()
         for(i in players.indices){
             if(playerWins[players[i]] == maxWins){
                 playersWithMaxWins.add(players[i])
                 //Liste ist nach Spielnummern sortiert
                 if(!randomLiga){
                 winner=players[i]
                 break
                 }
             }
         }
         if(randomLiga){
             winner = playersWithMaxWins[Random.nextInt(playersWithMaxWins.size)]
         }

     }
}



fun loadSpielstaerken( url: URL) : Array<Player>{

    val reader : BufferedReader = BufferedReader(InputStreamReader(url.openStream()))
    var playerCount = Integer.parseInt(reader.readLine())
    var players = arrayOfNulls<Player>(playerCount)
    for(i in 0 until playerCount){
        var line = reader.readLine()
        println(line + " gelesen")
        var spielstaerke = Integer.parseInt(line)
        players[i] = Player(i,spielstaerke)
    }
    return players.requireNoNulls()

}
