package de.loegler.bwinf.dreieckspuzzle.main

class Puzzleteil (teilID:Int,sides : IntArray){
    var teilID=teilID
    var sides= sides
         private set
    /**
     *
     */
     fun rotate(){
         var rotatedArray = intArrayOf(sides[1],sides[2],sides[0])
        sides=rotatedArray
     }

    override fun toString(): String {
        return "Puzzleteil(teilID=$teilID, sides=${sides.contentToString()})"
    }
}