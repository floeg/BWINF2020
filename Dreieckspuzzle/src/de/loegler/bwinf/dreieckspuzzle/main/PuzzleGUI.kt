package de.loegler.bwinf.dreieckspuzzle.main

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.border.LineBorder

class PuzzleGUI {


    fun createGUI(loesung: Array<Array<Puzzleteil?>>) {
        var frame = JFrame()
        var panels = arrayOfNulls<Array<JPanel?>>(3)
        for (i in 0..2)
            panels[i] = arrayOfNulls<JPanel>(5)
        frame.size = Dimension(1000, 1000)
        frame.setLocationRelativeTo(null)
        frame.contentPane.layout = null
        var base = 500
        var baseY = 100
        var compWidth = 150
        var compHeight = 150
        var firstPanel = JPanel()
        firstPanel.setBounds(base - compWidth / 2, baseY, compWidth, compHeight)
        panels[0]!![0] = firstPanel
        panels[1]!![1] = createDown(panels[0]!![0])
        panels[1]!![0] = createLeft(panels[1]!![1])
        panels[1]!![2] = createRight(panels[1]!![1])

        panels[2]!![1] = createDown(panels[1]!![0])
        panels[2]!![0] = createLeft(panels[2]!![1])
        panels[2]!![2] = createRight(panels[2]!![1])

        panels[2]!![3] = createRight(panels[2]!![2])
        panels[2]!![4] = createRight(panels[2]!![3])
        for (i in 0..2) {
            for (z in panels[i]!!.indices){
                var current = panels[i]?.get(z)
                var currentTeil = loesung[i][z]
                if(current!=null&&currentTeil!=null){
                    current.layout = BorderLayout()

                    var nullButton = JButton(""+currentTeil.sides[0])
                    nullButton.setSize(15,15)
                    var einsButton = JButton(""+currentTeil.sides[1])
                    einsButton.setSize(15,15)
                    var zweiButton = JButton(""+currentTeil.sides[2])
                    zweiButton.setSize(15,15)

                    if(z%2==0){
                        current.add(nullButton,BorderLayout.SOUTH)
                        current.add(einsButton,BorderLayout.EAST)
                        current.add(zweiButton,BorderLayout.WEST)
                    }else{

                        current.add(nullButton,BorderLayout.NORTH)
                        current.add(einsButton,BorderLayout.WEST)
                        current.add(zweiButton,BorderLayout.EAST)


                    }
                    current.add(JButton(""+currentTeil.teilID),BorderLayout.CENTER)
                    current.border=LineBorder(Color.BLACK)
                    frame.contentPane.add(current)
                }
            }
        }
        frame.isVisible = true
    }

    fun createDown(parent: JPanel?): JPanel {
        var tmp = JPanel()
        if (parent != null) {
            tmp.setBounds(parent.x, parent.y + parent.height + 5, parent.width, parent.height)
        }
        return tmp

    }

    fun createLeft(parent: JPanel?): JPanel {
        var tmp = JPanel()
        if (parent != null) {
            tmp.setBounds(parent.x - parent.width - 5, parent.y, parent.width, parent.height)
        }
        return tmp
    }

    fun createRight(parent: JPanel?): JPanel {
        var tmp = JPanel()
        if (parent != null) {
            tmp.setBounds(parent.x + parent.width + 5, parent.y, parent.width, parent.height)
        }
        return tmp
    }
}