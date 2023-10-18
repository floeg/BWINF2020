package de.loegler.bwinf.woerter;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class A1Solver {
    /**
     * Gesuchte Wörter
     */
    private List<MissedWord> missedWords = new ArrayList<>();
    /**
     * Gegebene Wörter
     */
    private List<String> givenWords;
    /**
     * Ergebnis-Array
     */
    private MissedWord[] result;
    private String sentenceWithMissedWords;
    /**
     * Zusätzliche Informationen werde in der Konsole ausgebeben, welche nicht Teil der Aufgabenstellung sind.
     * Hierdurch ist die Arbeitsweise des Algorithmus nachvollziehbar.
     */
    private boolean extendedLog = false;


    public A1Solver(URL source) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(source.openStream(),"UTF-8"));
            String firstLine = reader.readLine();
            String secondLine = reader.readLine();
            reader.close();
            this.sentenceWithMissedWords = secondLine;
            String[] split = firstLine.split(" ");
            givenWords = new ArrayList(Arrays.asList(secondLine.split(" ")));
            result = new MissedWord[split.length];
            for (int i = 0; i != split.length; i++) {
                String missedWord = split[i];
                MissedWord w = new MissedWord(missedWord, i);
                this.missedWords.add(w);
                result[i] = w;

                //Erkennung von Groß- und Kleinschreibung
                if (i == 0) {
                    w.couldBeCapital = true;
                    if (extendedLog)
                        System.out.println(w.missedWord + " könnte mit einem Großbuchstaben anfangen");
                } else {
                    char charBefore = split[i - 1].charAt(split[i - 1].length() - 1);
                    if (charBefore == '!' || charBefore == '.' || charBefore == '?') {
                        w.couldBeCapital = true;
                        if (extendedLog)
                            System.out.println(w.missedWord + " könnte mit einem Großbuchstaben anfangen");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        A1Solver solver = null;
        try {
            String input = JOptionPane.showInputDialog("Bitte geben Sie die URL der Raetseldatei an. Für lokale Dateien ist der Prefix file:\\ erforderlich.");
            if (input == null || input.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Verwende Raetsel Datei aus dem Internet");
                solver = new A1Solver(new URL("https://bwinf.de/fileadmin/bundeswettbewerb/39/raetsel4.txt"));
            } else
                solver = new A1Solver(new URL(input));
            solver.solve();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        solver.printResult();
    }

    public void printResult() {
        StringBuilder resultBuilder = new StringBuilder();
        for (MissedWord w : result) {
            resultBuilder.append(w.getResult() + " ");
        }
        System.out.println("Lösung:  ");
        System.out.println(resultBuilder);
    }

    public void solve() {
        if(extendedLog)
            System.out.println("Beginne von vorne");
        List<MissedWord> removedWords = new ArrayList<>();
        for (MissedWord missedWord : missedWords) {
            List<String> possibleWords = missedWord.possibleWords;
            for (int i = 0; i < missedWord.possibleWords.size(); i++) {
                String word = missedWord.possibleWords.get(i);
                if (missedWord.isPossibleWord(word) == false ||
                        !this.givenWords.contains(word)) {
                    missedWord.possibleWords.remove(word);
                }
            }
            Set tmp = new HashSet(); //Jedes Wort soll nur einmal gezählt werden
            possibleWords.forEach(current -> tmp.add(current));
            if (tmp.size() == 1) {
                missedWord.result = possibleWords.get(0);
                removedWords.add(missedWord);//Element gefunden, wird nach dem Durchlauf entfernt
                this.givenWords.remove(missedWord.result);
                if (extendedLog)
                    System.out.println("Gefunden: " + missedWord.result + " für " + missedWord);
            } else {
                if (missedWord.couldBeCapital) {
                    if (tmp.size() == 2) {
                        String[] possWords = possibleWords.toArray(new String[0]);
                        if (possWords[0].equalsIgnoreCase(possWords[1])) {
                            //Beide Wörter unterscheiden sich nur durch ihre Groß/Kleinschreibung
                            String result = Character.isUpperCase(possWords[0].charAt(0)) ? possWords[0] : possWords[1];
                            missedWord.result = result;
                            removedWords.add(missedWord);
                            this.givenWords.remove(result);
                        }
                    }
                }
            }
        }
        this.missedWords.removeAll(removedWords);
        if (this.missedWords.size() != 0) {
            solve();
        }
    }

    /**
     * Repräsentiert ein Lückenwort
     */
    private class MissedWord {
        String missedWord;
        String result;
        int index;
        List<String> possibleWords;
        /**
         * true, wenn sich das Wort am Anfang oder hinter einem Satzzeichen befindet.
         */
        boolean couldBeCapital = false;

        public MissedWord(String missedWord, int index) {
            this.index = index;
            this.missedWord = missedWord;
            this.possibleWords = new ArrayList<>(A1Solver.this.givenWords);
        }


        /**
         * Rückgabe des Wortes ohne Sonderzeichen (außer '_' für einen unbekannten Buchstaben)
         */
        public String getClearMissedWord() {
            String mW = missedWord;
            StringBuilder stringBuilder = new StringBuilder();
            mW.chars().forEachOrdered
                    ((int tmp) -> {
                        char it = (char) tmp;
                        if (Character.isAlphabetic(it) || it == '_')
                            stringBuilder.append(it);

                    });
            return stringBuilder.toString();
        }

        /**
         * Rückgabe, ob sich das MissedWord durch word ersetzen lassen würde
         */
        public boolean isPossibleWord(String word) {
            String clearWord = this.getClearMissedWord();
            if (word.length() != clearWord.length())
                return false;
            for (int i = 0; i != clearWord.length(); i++) {
                if (clearWord.charAt(i) != word.charAt(i) && clearWord.charAt(i) != '_')
                    return false;
            }
            return true;
        }

        public String getResult() {
            String word = this.result;
            //Ersetzt das Lückenwort durch das konkrete Wort
            word= this.missedWord.replace(this.getClearMissedWord(),word);
            return word;
        }

    }

}


