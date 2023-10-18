package de.loegler.bwinf.tobistunier.main

import kotlin.random.Random

data class Player(val playerID:Int, val spielstaerke:Int){
    infix fun vs(other:Player):Player {
        //Zahl von 0-beide Spielstärken zusammen-1
        var rand = Random.nextInt(spielstaerke+other.spielstaerke)
        if(rand<spielstaerke)
            return this
        else return other
    }


}


