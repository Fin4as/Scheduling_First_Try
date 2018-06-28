
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author robin
 */
public class Functions {

    Schedule s;

    public Functions(Schedule s) {
        this.s = s;
    }

    public double wait(int w) {
        double result = 0.0;
        result = ((double) 1 / 6) * w;
        return result;
    }

    public double late(int l) {
        double result = 0.0;
        result = 0.125 * l;
        return result;
    }

    public double fO(List<Patient> sequence) {
        double result = 0;

        Test t = new Test(sequence, s);
        t.addTask();

        result = t.calculateMakespan();

        result += wait(t.getTotalWaitingTime());
        result += late(t.getLateness());
        // System.out.println(wait(t.totalWaitingTime));
//        for(int i =0; i<sequence.size(); i++){
//            System.out.print(sequence.get(i).getPatientID());
//            System.out.println(Arrays.toString(sequence.get(i).getSchedule()));
//        }
//        System.out.println("");
//        
//        for(int j =0; j<t.getListResource().size(); j++){
//            System.out.print(t.getListResource().get(j).getResourceID());
//            System.out.println(Arrays.toString(t.getListResource().get(j).getTime()));
//        }

        return result;
    }

    public List<Patient> annealingMin(double temperature, int itermax, List<Patient> scur) {
        List<Patient> sold;
        double tempmin = 0.1;
        int numiter = 0;
        double coolingRate = 0.01;
        double dif;
        double rd;
        List<Patient> minb = scur;
//        System.out.println("scur init : " + fO(scur));
//        System.out.println("minb init : " + fO(minb));
        while (temperature >= tempmin) {
            //System.out.println("hey");
            if (numiter < itermax) {
                sold = scur;
                scur = SwappableSequence.deterministicSwap(scur, numiter % (scur.size()), (numiter + 1) % (scur.size()));
                //System.out.println(snew.toString());
//                System.out.println("scur - sold" + fO(scur) + "    " + fO(sold) + " = dif : " + (fO(scur) - fO(sold)));
                dif = fO(scur) - fO(sold);
                //System.out.println(f(snew)+ "-" + f(scur)+" = "+dif);

                if (dif <= 0) {
                    sold = scur;
                    double dif2 = fO(sold) - fO(minb);
                    if (dif2 <= 0) {
                        minb = sold;
//                        System.out.println(fO(minb));

                    }

                } else {
                    rd = Math.random();
                    if (rd < exp(-dif / (1.38064852 * pow(10, -23)) * temperature)) {
                        sold = scur;
                    }
                }
                numiter++;
//                    System.out.println("");
//                    System.out.println(temperature);
//                    System.out.println("iteration n°"+numiter);
//                    System.out.println("Current x min : "+ minb);
//                    System.out.println("Current minimum:"+ f(minb));
//                    System.out.println("Position of the scur: "+ scur);
//                    System.out.println("Outcome of the f(scur): "+ f(scur));
//                    System.out.println("Position of snew: "+snew);
//                    System.out.println("Outcome of the f(snew):" + f(snew));

            } else {
                temperature = coolingRate * temperature;
                numiter = 0;
            }

        }
        System.out.print("The minimum is located  ");
        return minb;
    }

    /**
     * Genetic Algorithm
     *
     * @param sizePopulation size of the population studied
     * @param nbrGeneration number of generation done before finding the best
     * sequence
     * @return the sequence with the best fitness
     */
    public List<Patient> genetic(int sizePopulation, int nbrGeneration, List<Patient> scur) {
        // List of Sequences considered as a population
        List<List<Patient>> population = new ArrayList();
        // Declaration of the initial sequence 
        List<Patient> bestPosition = scur;

        //Initialization of the two lists used for the parents
        List<Patient> bestPopulation1;
        List<Patient> bestPopulation2;

        //Filling of the population by random sequences(replace by Quentin)
        Random rd = new Random();
        List<Patient> randomPatients;
        List<Patient> possiblePatient = new ArrayList();
        Patient randomPatient;
        int iteratorCheck;

        population.add(scur);
        while (population.size() < sizePopulation) {
            for (int i = 0; i < scur.size(); i++) {
                possiblePatient.add(scur.get(i));
            }
            randomPatients = new ArrayList();
            iteratorCheck = 0;
            while (randomPatients.size() < scur.size()) {
                randomPatient = possiblePatient.get(rd.nextInt(possiblePatient.size()));
                randomPatients.add(randomPatient);
                possiblePatient.remove(randomPatient);

            }
            while (iteratorCheck < population.size()) {
                if (population.contains(randomPatients)) {
                    break;
                } else {
                    iteratorCheck++;
                }
                if (iteratorCheck == population.size()) {
                    population.add(randomPatients);
                }
            }
        }
        //End of the part of Quentin

        //Comparison of fitness of the two first sequences of the population to set -the first two parents
        if (fO(population.get(0)) < fO(population.get(1))) {
            bestPopulation1 = population.get(0);
            bestPopulation2 = population.get(1);
        } else {
            bestPopulation1 = population.get(1);
            bestPopulation2 = population.get(0);
        }

        /*Evolution of the population to find the sequence with the best fitness
        after a fixed number of iterations*/
        int n = 0;
        while (n < nbrGeneration) {

            //Examination of the population to find the two fittest sequences
            bestPopulation2 = population.get(0);
            for (int j = 0; j < sizePopulation; j++) {
                List<Patient> read = population.get(j);
                if (fO(read) < fO(bestPopulation2)) {
                    if (fO(read) < fO(bestPopulation1)) {
                        bestPopulation2 = bestPopulation1;
                        bestPopulation1 = read;
                    }
                    bestPopulation2 = read;
                }
            }

            //Realisation of the crossing over to create an offspring supposedly better than its two parents
            List<Patient> child = SwappableSequence.makeACrossingOver(bestPopulation1, bestPopulation2, 4);
//                //This offspring is added in the population 
            population.add(child);
            //The list fit parent in taken out of the population 
            System.out.println(population.indexOf(bestPopulation2));
            population.remove(population.indexOf(bestPopulation2));

            //A Generation pass
            n++;
        }

        //Find the best sequence at the end of the evolution
        bestPosition = population.get(0);
        for (int m = 1; m < sizePopulation; m++) {
            if (fO(population.get(m)) < fO(bestPosition)) {
                bestPosition = population.get(m);
            }
        }
        return bestPosition;
    }

    public List<Patient> tabuSearch(int itermax, int sizeTabuList, List<Patient> scur) {
        List<Patient> bestPosition = scur;
        List<Comparison> tabuList = new ArrayList<>();
        int i = 0;
        List<Patient> curSpot = bestPosition;

        while (i < itermax) {

            List<Patient> test = SwappableSequence.deterministicSwap(curSpot, i % (scur.size()), (i + 1) % (scur.size()));
            List<Patient> pairTest = new ArrayList<>();
            Patient e1 = null;
            Patient e2 = null;

            for (int k = 0; k < test.size(); k++) {
                if (!test.get(k).equals(curSpot.get(i))) {
                    e1 = test.get(k);
                    e2 = curSpot.get(i);
                }
            }
            pairTest.add(e1);
            pairTest.add(e2);

            Comparison pair = new Comparison(pairTest);

            boolean tabu = false;
            int m = 0;
            while (m < tabuList.size() && !tabu) {
                if ((pairTest.get(0).equals(tabuList.get(m).paire.get(0))) || (pairTest.get(0).equals(tabuList.get(m).paire.get(1)))) {
                    if ((pairTest.get(1).equals(tabuList.get(m).paire.get(0))) || (pairTest.get(1).equals(tabuList.get(m).paire.get(1)))) {
                        tabu = true;
                    }
                }
                m++;
            }
            if (!tabu && m < tabuList.size()) {
                curSpot = test;
                if (fO(curSpot) < fO(bestPosition)) {
                    bestPosition = curSpot;
                }
                tabuList.add(sizeTabuList, pair);
                if (tabuList.size() > sizeTabuList) {
                    tabuList.remove(0);
                }
            }
            i++;
        }

        return bestPosition;
    }

}
