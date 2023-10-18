package de.loegler.bwinf.dreieckspuzzle.main

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.swing.JOptionPane
import kotlin.math.pow

/**
 * In dieser Schrittfolge wird das Dreieckspuzzle gelöst.
 * Format: Zeile;Spalte
 */
var steps = arrayListOf<String>("0;0","1;1","1;0","1;2","2;1","2;3","2;2","2;0","2;4")
var loesungGefunden= false

fun main(args : Array<String>) {

    var path = JOptionPane.showInputDialog("Bitte geben Sie die URL des Beispiels an. Für lokale Dateien ist der Präfix 'file:\\' erforderlich.")
    if(path==null||path.isEmpty())
        path="https://bwinf.de/fileadmin/bundeswettbewerb/39/puzzle3.txt"
    var alleTeile =ladeDreiecke(URL(path))
    var loesung = arrayOfNulls<Array<Puzzleteil?>>(3)
    for(i in 0 until 3)
        loesung[i]=(arrayOfNulls<Puzzleteil>(5))

    solve(alleTeile,loesung.requireNoNulls(),0,0)
    if(!loesungGefunden)
       println("Das Puzzle kann nicht gelöst werden.")
}


fun solve(teileUebrig: MutableList<Puzzleteil>, loesung : Array<Array<Puzzleteil?>>, zeile: Int, spalte:Int){
    println(""+teileUebrig.size + " uebrige bei $zeile Zeile $spalte Spalte bei Lösung ${loesung[2][4]}")
    for(teil in teileUebrig) {
        for (i in 0 until 3) {
            if(loesungGefunden)
                return
            setzeZurueckAb(zeile,spalte,loesung)

            if (isLegbar(zeile, spalte, teil, loesung)) {
                println("Setze Lösung $zeile $spalte auf $teil")
                loesung[zeile][spalte] = teil
                //copyUebirg
                var teileUebrigCopy = mutableListOf<Puzzleteil>()
                teileUebrigCopy.addAll(teileUebrig)
                teileUebrigCopy.remove(teil)
                if(spalte==4|| getNext(zeile,spalte)[0]==-1)
                {
                        println("Lösung gefunden - Das Puzzle kann gelöst werden!")
                        loesung.forEach {
                            it?.forEach{
                                if(it!=null)
                                    println(it.toString())
                            }
                            println("Neue Reihe")
                        }
                        loesungGefunden=true
                        PuzzleGUI().createGUI(loesung)
                    loesungGefunden=true
                    return
                }
                var neueZeile = getNext(zeile,spalte)[0]
                var neueSpalte = getNext(zeile,spalte)[1]
                if (zeile < 3){

                    solve(teileUebrigCopy, loesung, neueZeile, neueSpalte)
                }
            }
            teil.rotate()
        }
    }
}

/**
 * Rückgabe, ob eine Spalte in einer Zeile existiert
 */
fun existiertTeil(zeile:Int, spalte:Int): Boolean{
    if(spalte<0)
        return false
    if(zeile==0)
        return spalte==0

   return spalte <= (2.0.pow(zeile.toDouble()))
}

 fun getNext(zeile: Int, spalte: Int) : IntArray{
    for(i in 0 until steps.size-1){
        var tmp =steps[i].split(";")
        if(Integer.parseInt(tmp[0])==zeile&&Integer.parseInt(tmp[1])==spalte)
            return intArrayOf(  Integer.parseInt(steps[i+1].split(";")[0]),Integer.parseInt(steps[i+1].split(";")[1])     )
    }
    return intArrayOf(-1)
}

fun setzeZurueckAb(zeile: Int, spalte: Int, loesung : Array<Array<Puzzleteil?>>){
    var resetNow = false
    for(i in 0 until steps.size){
        if(resetNow){
            var z = Integer.parseInt(steps[i].split(";")[0])
            var s = Integer.parseInt(steps[i].split(";")[1])
            loesung[z][s]=null
        }
        else
            resetNow= ("$zeile;$spalte") == steps[i]
    }
}

/**
 * Prüft, ob das aktuelle Teil an der gegebenen Stelle eingesetzt werden kann
 */
fun isLegbar(zeile:Int,spalte:Int, aktuellesTeil : Puzzleteil, loesung : Array<Array<Puzzleteil?>>): Boolean{

    if(zeile==0)
        return true //Das erste Teil kann immer gelegt werden
    var linkerNachbar : Puzzleteil? = null
    var rechterNachbar : Puzzleteil? = null
    var oben: Puzzleteil?
    if(existiertTeil(zeile,spalte-1))
        linkerNachbar= loesung[zeile][spalte-1]
    if(existiertTeil(zeile,spalte+1))
        rechterNachbar=loesung[zeile][spalte+1]
    if(spalte%2!=0){
       oben = loesung[zeile-1][spalte-1]
        if (oben != null) {
            if(oben.sides[0]+aktuellesTeil.sides[0]!=0)
                return false
        } //Teil passt nicht mit Teil darüber
    }

    //Entweder aktuelle Teil 'umgedreht', oder Nachbarn
    if(spalte%2==0){
        //Nachbarn umgedreht
        if(linkerNachbar!=null&&linkerNachbar.sides[2]+aktuellesTeil.sides[2]!=0)
            return false
        if(rechterNachbar!=null&&rechterNachbar.sides[1]+aktuellesTeil.sides[1]!=0)
            return false

    }else{
        //Aktuelles Teil umgedreht
        if(linkerNachbar!=null&&linkerNachbar.sides[1]+aktuellesTeil.sides[1]!=0)
            return false
        if(rechterNachbar!=null&&rechterNachbar.sides[2]+aktuellesTeil.sides[2]!=0)
            return false
    }

    return true
}


fun ladeDreiecke(url: URL): MutableList<Puzzleteil> {
    var alleTeile = mutableListOf<Puzzleteil>()

    var reader = BufferedReader(InputStreamReader(url.openStream()))
    var figurenAnzahl  = reader.readLine()
    var teile = reader.readLine()
    var zeile = reader.readLine()
    var i=0
    while(zeile!=null){

        var parts = zeile.split(" ")
        var iArray = intArrayOf(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2]))
        var teil = Puzzleteil(i,iArray)
        alleTeile.add(teil)
        zeile=reader.readLine()
        i++
    }
    alleTeile.forEach { println(it.toString()) }
    return alleTeile
}