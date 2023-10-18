package de.loegler.bwinf.eisbudendilemma.main

import jdk.nashorn.internal.ir.annotations.Immutable

class Graph<T>(){
    val vertices = mutableListOf<Vertex<T>>()
    val edges = mutableListOf<Edge<T>>()


    fun addVertex(v : Vertex<T>){
        if(!vertices.contains(v))
            vertices.add(v)
    }

    fun addVertex(content : T) : Vertex<T>{
        var tmp = Vertex(content)
        addVertex(tmp)
        return tmp
    }

    fun addEdge(edge : Edge<T>){
        if(vertices.containsAll(edge.getVertices().toList())){
            var tmp = edges.find { it.sameConnection(edge) }
            if(tmp == null){ //Verbindung zwischen den zwei Knoten noch nicht vorhanden
                edges.add(edge)
            }
        }
    }

    fun addEdge(v1 : Vertex<T>, v2 : Vertex<T>,weight: Double=1.0){
        addEdge(Edge(v1,v2,weight))
    }


    fun removeEdgeBetween(vertex: Vertex<T>, other: Vertex<T>){
        edges.remove(getEdgeBetween(vertex,other))
    }


    //TODO Prüfen: Sideeffects auf alte Liste!
    fun getEdges(vertex : Vertex<T>) : List<Edge<T>> =
        edges.filter { return@filter (it.vertex == vertex || it.other == vertex) }

    fun getEdgeBetween(vertex: Vertex<T>, other: Vertex<T>) : Edge<T>? =
         edges.find { (it.vertex==vertex&&it.other==other)|| (it.vertex==other && it.other==vertex) }

    fun getNeighbours(vertex: Vertex<T>) : List<Vertex<T>> = getEdges(vertex).map {
        if(it.vertex==vertex)
            return@map it.other
        else
            return@map it.vertex
    }





}



@Immutable
data class Edge<T>(val vertex:Vertex<T>,val other:Vertex<T>,val weight : Double = 1.0){

    /**
     * @return true, falls die (ungerichtete) Kante zu den gleichen Knoten zeigt.
     */
    fun sameConnection(other: Edge<T>) : Boolean {
        if(vertex != other.vertex && vertex!=other.other)
            return false
        if(other!=other.vertex && other!=other.other)
            return false
        return true
    }


    fun getVertices() = Pair<Vertex<T>,Vertex<T>>(vertex,other)
}



data class Vertex<T>(val value: T, var marked : Boolean = false){

}


