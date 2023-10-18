package de.loegler.bwinf20.a5.main;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class A5Solver {
    public List<Person> persons = new ArrayList<>();
    List<PossibleResult> toRemoveListe;
    private PossibleResult currentBest;

    public A5Solver(URL url) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String amountStudents = reader.readLine();
        int amount = Integer.parseInt(amountStudents.trim());
        for (int i = 0; i != amount; i++) {
            String line = reader.readLine();
            String[] fragments = line.split(" ");
            int tmp = 0;
            Person person = new Person(i);
            for (String s : fragments) {
                if (!s.isEmpty() && s.matches("[0-9]*")) {
                    person.wish[tmp++] = Integer.parseInt(s);
                }
            }
            persons.add(person);


        }
        reader.close();
        solve(persons);
    }

    public static void main(String[] args) throws IOException {
        String path = JOptionPane.showInputDialog("Bitte geben Sie die URL des Beispiels ein. Für lokale Dateien ist der Präfix 'file\\' erforderlich.");
        if(path==null||path.isEmpty())
            path="https://bwinf.de/fileadmin/bundeswettbewerb/39/wichteln7.txt";
        A5Solver a5Solver = new A5Solver(new URL(path));
    }

    public void solve(List<Person> persons) {
        PossibleResult startResult = new PossibleResult();
        startResult.uncheckedPersons = new ArrayList<>(persons);
        LinkedList<PossibleResult> possibleResults = new LinkedList<>();
        toRemoveListe = possibleResults;
        possibleResults.add(startResult);
        while (!possibleResults.isEmpty()) {
            PossibleResult current = possibleResults.pollLast();
            solve(current, current.step, possibleResults);
            if (currentBest == null || compareDesc(currentBest, current) > 0) {
                currentBest = current;
            }
        }
        System.out.print("Lösung: ");
        currentBest.printResult();
    }

    /**
     * @param possibleResult
     * @param pass
     * @param pResults       Alle Möglichkeiten, welche in Schritt 1-3( intern 0-2) entstanden sind
     */
    public void solve(PossibleResult possibleResult, int pass, List<PossibleResult> pResults) {
        /*
        Wenn bereits bei den vorherigen Schritten (an welchen sich in dieser Möglichkeit nichts ändern wird) eine
        bessere Lösung gibt, dann kann die komplette Möglichkeit verworfen werden.
        Gilt nicht, wenn sie gleich groß sind - dann könnten die pass+1-Level Wünsche entscheidend sein!
         */
        if (currentBest != null && pass > 0 && compareBest(currentBest, possibleResult) < 0) {
            return;
        }
        while (possibleResult.uncheckedPersons.size() > 0) {
            /*Wenn die Wünsche aller ungeprüften Personen erfüllt werden würden, und trotzdem weniger
            Wünsche erfüllt werden würden, als beim aktuell besten, kann abgebrochen werden
             */
            if (currentBest != null &&
                    possibleResult.wishesFulfilled[pass] + possibleResult.uncheckedPersons.size() < currentBest.wishesFulfilled[pass]) {
                return;
            }
            Person uncheckedPerson = possibleResult.uncheckedPersons.get(0);
            possibleResult.uncheckedPersons.remove(uncheckedPerson);
            int wish = uncheckedPerson.wish[pass];
            if (possibleResult.dontChangeWishNumber.contains(wish)) {
                possibleResult.saveForNextStep.add(uncheckedPerson);
                continue;
            }

            /*
             * Sind die anderen Wünsche ("schwächeren") schon vergeben hat es keinen
             * Nachteil, wenn der pass+1-Level Wunsch erfüllt wird
             */
            boolean otherWishesAlreadyTaken = pass != 3;
            for (int i = pass + 1; i != 3; i++) {
                int lowerWish = uncheckedPerson.wish[i];
                otherWishesAlreadyTaken = otherWishesAlreadyTaken && (possibleResult.giftPersonMap.get(lowerWish) != null);
            }

            Person oldPerson = possibleResult.giftPersonMap.get(wish); //kann null sein!
            //Hatte eine andere Person bereits den Wunsch nach diesem Geschenk?
            if (possibleResult.giftPersonMap.get(wish) == null || otherWishesAlreadyTaken) {
                possibleResult.giftPersonMap.put(wish, uncheckedPerson);
                possibleResult.personGiftMap.put(uncheckedPerson, wish);
                possibleResult.uncheckedPersons.remove(uncheckedPerson); //Person hat nun einen Wunsch erfüllt bekommen
                if (oldPerson != null) { //Andere Wünsche der Person waren nicht erfüllbar
                    possibleResult.personGiftMap.remove(oldPerson);
                    possibleResult.saveForNextStep.add(oldPerson);
                    possibleResult.dontChangeWishNumber.add(wish); //Keine Nachteile durch erfüllen des Wunsches
                } else
                    possibleResult.wishesFulfilled[pass]++; //Ein weiterer Wunsch der Stufe "pass" wurde erfüllt
            } else {
                int oldWishLevel = -1;
                for (int tmp = 0; tmp != 3; tmp++) {
                    if (oldPerson.wish[tmp] == wish) {
                        oldWishLevel = tmp;
                        break;
                    }
                }
                if (oldWishLevel < pass) {
                    possibleResult.saveForNextStep.add(uncheckedPerson);
                    possibleResult.uncheckedPersons.remove(uncheckedPerson);
                } else {
                    boolean oldWishesEqual = true;
                    boolean oldPersonNoNegative = true;
                    for (int i = pass + 1; i != 3; i++) {
                        oldWishesEqual = oldWishesEqual && (oldPerson.wish[i] == uncheckedPerson.wish[i]
                                || (possibleResult.giftPersonMap.get(oldPerson.wish[i]) != null));
                        oldPersonNoNegative = oldPersonNoNegative &&
                                possibleResult.giftPersonMap.get(oldPerson.wish[i]) != null;
                    }
                    if (oldPersonNoNegative)
                        possibleResult.dontChangeWishNumber.add(wish);
                    if (oldPersonNoNegative || oldWishesEqual) {
                        //Keine Wünsche der alten Person erfüllbar -> Keine 'Nachteile'
                        possibleResult.saveForNextStep.add(uncheckedPerson);
                    } else {
                        PossibleResult clone = possibleResult.clone();
                        // Neue Verteilung: Wunsch wird für die aktuelle Person erfüllt
                        possibleResult.saveForNextStep.add(uncheckedPerson);
                        clone.uncheckedPersons.remove(uncheckedPerson);
                        clone.saveForNextStep.add(oldPerson); //Person im nächsten Schritt
                        //Anpassung der Maps an neue Verteilung
                        clone.personGiftMap.remove(oldPerson);
                        clone.personGiftMap.put(uncheckedPerson, wish);
                        clone.giftPersonMap.put(wish, uncheckedPerson);
                        //Die Anzahl der erfüllten Wünsche bleibt gleich, da ein "pass"-Wunsch nicht mehr erfüllt wird
                        clone.dontChangeWishNumber.add(wish);
                        pResults.add(clone);

                    }
                }
            }
        }
        possibleResult.uncheckedPersons.addAll(possibleResult.saveForNextStep);
        if (++possibleResult.step != 3) {
            possibleResult.saveForNextStep.clear();
            possibleResult.dontChangeWishNumber.clear();
            solve(possibleResult, possibleResult.step, pResults);
        }
    }


    public int compareDesc(PossibleResult current, PossibleResult other) {
        int tmp = compareAsc(current, other);
        if (tmp == 0)
            return 0;
        if (tmp < 0)
            return 1;
        else
            return -1;


    }

    /**
     * Wie viele Wünsche werden momentan gleichzeitig erfüllt?
     *
     * @param other
     */
    public int compareAsc(PossibleResult current, PossibleResult other) {
        for (int i = 0; i != 3; i++) {
            int intComp = Integer.compare(current.wishesFulfilled[i], other.wishesFulfilled[i]);
            if (intComp != 0)
                return intComp;

            //if(current.wishesFulfilled[i]<other.wishesFulfilled[i])
            //  return -1;
            //else if(current.wishesFulfilled[i]>other.wishesFulfilled[i])
            //  return 1;
        }
        return 0;

    }

    /**
     * Vergleicht ein neues PossibleResult mit der derzeitig besten Wahl
     *
     * @return 0, wenn bis newPossibleResult.step+1 gleich viele Wünsche erfüllt werden, <0, wenn oldBest weniger Wünsche erfüllt als newPossibleResult
     */
    public int compareBest(PossibleResult oldBest, PossibleResult newPossibleResult) {
        for (int i = 0; i != newPossibleResult.step; i++) {
            int tmp = Integer.compare(oldBest.wishesFulfilled[i], newPossibleResult.wishesFulfilled[i]);
            if (tmp != 0)
                return tmp;
        }
        return 0;
    }

    public int compareAsc(Person p1, Person p2) {
        return Integer.compare(p1.personID, p2.personID);
    }


    public class PossibleResult implements Comparable {
        private Map<Integer, Person> giftPersonMap;
        private Map<Person, Integer> personGiftMap;
        private List<Person> uncheckedPersons;
        private int[] wishesFulfilled = new int[3];
        private List<Person> saveForNextStep;
        private List<Integer> dontChangeWishNumber;
        /**
         * Der aktuelle Schritt - entspricht dem Schritt des PossibleResults, aus welchem diese
         * Verteilung entstanden ist
         */
        private int step = 0;

        public PossibleResult() {
            this.init();
        }

        public PossibleResult(int step) {
            this.step = step;
        }

        public PossibleResult clone() {
            PossibleResult clone = new PossibleResult(this.step);
            clone.uncheckedPersons = new ArrayList<>(uncheckedPersons);
            clone.giftPersonMap = new HashMap<>(giftPersonMap);
            clone.personGiftMap = new HashMap<>(personGiftMap);
            clone.wishesFulfilled = Arrays.copyOf(wishesFulfilled, 3);
            clone.saveForNextStep = new ArrayList<>(saveForNextStep);
            clone.dontChangeWishNumber = new ArrayList<>(dontChangeWishNumber);
            return clone;
        }

        public void init() {
            giftPersonMap = new HashMap<>();
            personGiftMap = new HashMap<>();
            uncheckedPersons = new ArrayList<>();
            saveForNextStep = new ArrayList<>();
            dontChangeWishNumber = new ArrayList<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PossibleResult that = (PossibleResult) o;

            if (!giftPersonMap.equals(that.giftPersonMap)) return false;
            return personGiftMap.equals(that.personGiftMap);
        }

        @Override
        public int hashCode() {
            int result = giftPersonMap.hashCode();
            result = 31 * result + personGiftMap.hashCode();
            return result;
        }

        public void printResult() {
            System.out.print("Die Verteilung erfüllt ");
            for (int i = 0; i != 3; i++)
                System.out.print(this.wishesFulfilled[i] + "* " + (i + 1) + "-Wünsche; ");
            System.out.println();
            System.out.print("Übrige Geschenke:");
            List<Integer> wishesLeft = new ArrayList<>();
            for (int i = 1; i <= persons.size(); i++) {
                if (giftPersonMap.get(i) == null)
                    wishesLeft.add(i);
            }


            System.out.print(Arrays.toString(wishesLeft.toArray(new Integer[0])));
            System.out.println();
            System.out.print("Übrige Personen: ");
            System.out.print(Arrays.toString(uncheckedPersons.toArray(new Person[0])));
            System.out.println();


            this.personGiftMap.forEach((p, g) -> System.out.println(p.toString() + " bekommt: " + g));

        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof PossibleResult)
                return compareAsc(this, (PossibleResult) o);
            else
                return 0;
        }
    }

    private class Person implements Comparable {
        final int[] wish = new int[3];
        final int personID;

        public Person(int personID) {
            this.personID = personID;
        }

        @Override
        public String toString() {
            return "Person(" +
                    personID +
                    ')';
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof Person)
                return Integer.compare(this.personID, ((Person) o).personID);
            return 0;
        }

    }
}
