
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Class used to define the objective function and write the different
 * metaheuristic algorithms
 *
 * @author Robin Blanchard. Contact info : robin.blanchard@wanadoo.fr
 */
public class Functions {

    private int totaliterGrasp;
    private int totaliterBest;
    Data s;

    /**
     * Constructor of the class Initialize the data Functions will be using
     *
     * @param s the Copy of the data base
     */
    public Functions(Data s) {
        this.s = s;
    }

    /**
     * Function allowing to round double values by choosing how many number of
     * decimals kept
     *
     * @param value the double valut to round
     * @param places number of decimals to keep
     * @return rounded number
     * @see BigDecimal#setScale(int, java.math.RoundingMode)
     * @see BigDecimal#doubleValue()
     */
    public static double round(double value, int places) {
        // if there number behind the comma is negative then throw an exception
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        //Creation of a type of variable that can be rounded
        BigDecimal bd = new BigDecimal(value);
        //The value is rounded the half-up according to the number of places chosen
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        //Conversion to double
        return bd.doubleValue();
    }

    /**
     * Calculus of the waiting penalization
     *
     * @param w waiting time
     * @return waiting penalization
     */
    public double waiting(int w) {
        // see the documentation for further explanation about the coefficients
        
        //creation of the variable that will take the value of the wainting penalization
        double result;
        //If the value of waiting is inferior to 60
        if (w < 60) {
            result = (1 / 15) * w;
        //If the value of waiting is superior to 60
        } else {
            result = (-40 + w) / 5;
        }
        return result;
    }

    /**
     * Calculus of the lateness penalization
     *
     * @param l lateness
     * @return lateness penalization
     * @see #round(double, int)
     */
    public double late(int l) {
        // see the documentation for further explanation 
        
        //creation of the variable that will take the value of the lateness penalization
        double result;
        result = round(Math.exp(l / (100 / Math.log(30))), 4);
        return result;
    }

    /**
     * Calculus of the value of the objective function of a Schedule according
     * to a certain sequence of patients
     *
     * @param sequence Sequence of patients to be scheduled in that order
     * @param giveDetails Boolean which determining if the schedule is sent to
     * the Excel document
     * @return the value of the objective function
     *
     * @see Test#Test(java.util.List, Data)
     * @see Patient
     * @see Test#addTask(boolean)
     * @see Test#calculateMakespan()
     * @see Test#getTotalWaitingTime()
     * @see Test#getLateness()
     * @see Test#getListResource()
     * @see #waiting(int)
     * @see #late(int)
     * @see ExcelWriter#write(java.util.List)
     */
    public double fO(List<Patient> sequence, boolean giveDetails) {

        try {
            //Initialize the Test to schedule the sequence entered as a parameter and the infrmation of the Data Base            
            Test t = new Test(sequence, s);
            // Create the schedule 
            t.addTask(giveDetails);

//Calculus of the value of the objective function 
            //Add the value of makespan from the Test t
            double result = t.calculateMakespan();
            //Add the value of waiting penalization according to the value of waiting from the Test t
            result += waiting(t.getTotalWaitingTime());
            //Add the value of lateness penalization according to the value of lateness from the Test t 
            result += late(t.getLateness());

            if (giveDetails == true) {
                ExcelWriter excelWriter = new ExcelWriter();
                excelWriter.write(t.listPatient);
                for (int h = 0; h < t.getListResource().size(); h++) {
                    excelWriter.update(t.getListResource());
                }
                System.out.println("Excel files created on the desktop");
            }

            return result;
        } catch (IllegalArgumentException e) {
            return Double.MAX_VALUE;
        }
    }

    /**
     * Simulated Annealing algorithm used to find the minimum of the objective
     * function
     *
     * @param temperature Degree of acceptance of worst result for the value of
     * objective function will reduce during the run of the algorithm
     * @param tempmin Symbolize the end of the algorithm
     * @param itermax Number of iteration by temperature
     * @param scur Initial sequence of Patients
     * @return Best Sequence found with the minimum value of the objective
     * function
     * @see Patient
     * @see #fO(java.util.List, boolean)
     * @see Math#random()
     * @see BufferedWriter#write(java.lang.String)
     * @see Sequence#deterministicSwap(java.util.List, int, int)
     * @see System#nanoTime()
     */
    public List<Patient> annealingMin(double temperature, double tempmin, int itermax, List<Patient> scur, int swap) {
        //Initialization of the list of patients that will be studied and modified throughout the run of the algorithm 
        List<Patient> sStudied = new ArrayList();
        double startRuntime = System.nanoTime();
        double currentRuntime = System.nanoTime();
        double totalRuntime;
        //factor that will determine how fast the temperature will decrease
        double coolingRate = 0.70;
        int numtemp = 0;
        int numiter = 1;
        int numiterBest = 1;
        //This variable is used to compare the value of sequence according to the objective function
        double dif;
        //This variable is used to pick a random number for the Boltzmann equation
        double rd;
        List<String> sequenceDisplay = new ArrayList();
        for (Patient p : scur) {
            sequenceDisplay.add(p.getPatientID());
        }
        //this variable is used to keep the sequence with the smallest value of the objective function 
        List<Patient> minb = new ArrayList();
        /*Definition of the initial sequence as the best sequence for scheduling 
        Each patient is added one by one*/
        for (Patient p : scur) {
            minb.add(p);
        }

        try (Writer writerAnnealing = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("improvementAnnealing.txt", true)));
                Writer writerAnnealingStatTime = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("improvementAnnealingStatTime.txt", true)));
                Writer writerAnnealingStatSequence = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("improvementAnnealingStatSequence.txt", true)));
                Writer writerAnnealingStatValue = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("improvementAnnealingStatValue.txt", true)))) {
            if (fO(scur, false) == Double.MAX_VALUE) {
                writerAnnealing.write("////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////" + System.getProperty("line.separator"));
                writerAnnealing.write(System.getProperty("line.separator"));
                writerAnnealing.write("NEW RUN" + System.getProperty("line.separator"));
                writerAnnealing.write(System.getProperty("line.separator"));
                writerAnnealing.write("Initial solution S0 : " + sequenceDisplay + System.getProperty("line.separator"));
                writerAnnealing.write("Objective function value of the initial solution : The initial sequence is not schedulable" + System.getProperty("line.separator"));
                writerAnnealing.write("Neighbourhood search method used: Type " + swap + ". Refer to the documentation to know the corresponding method." + System.getProperty("line.separator"));
                writerAnnealing.write(System.getProperty("line.separator"));
                writerAnnealing.write("Temperature : " + temperature + System.getProperty("line.separator"));
                writerAnnealing.write(System.getProperty("line.separator"));
            } else {
                writerAnnealing.write("////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////" + System.getProperty("line.separator"));
                writerAnnealing.write(System.getProperty("line.separator"));
                writerAnnealing.write("NEW RUN" + System.getProperty("line.separator"));
                writerAnnealing.write(System.getProperty("line.separator"));
                writerAnnealing.write("Initial solution S0 : " + sequenceDisplay + System.getProperty("line.separator"));
                writerAnnealing.write("Objective function value of the initial solution : " + fO(scur, false) + System.getProperty("line.separator"));
                writerAnnealing.write("Neighbourhood search method used: Type " + swap + ". Refer to the documentation to know the corresponding method." + System.getProperty("line.separator"));
                writerAnnealing.write(System.getProperty("line.separator"));
                writerAnnealing.write("Temperature : " + temperature + System.getProperty("line.separator"));
                writerAnnealing.write(System.getProperty("line.separator"));
            }

            // while the temperature has not reached the limit continue to search 
            while (temperature >= tempmin) {

                // if the number of iteration has not reached the limit
                if (numiter <= itermax) {
                    //Empty the list studied  
                    sStudied = new ArrayList();
                    // Fill the list studied with the content of the current sequence 
                    for (Patient i : scur) {
                        //Each patient is added one by one
                        sStudied.add(i);
                    }
                    // Modification of the studied sequence to perform a Neighbourhood search according several types of swaps 
                    switch (swap) {
                        case 1:
                            Sequence.simpleSwap(sStudied, (numiter - 1) % (sStudied.size()), numiter % (sStudied.size()));
                            break;
                        case 2:
                            Sequence.simpleSwap(sStudied, (numiter - 1) % (sStudied.size()), (numiter + 1) % (sStudied.size()));
                            break;
                        case 3:
                            Sequence.simpleSwap(sStudied, (numiter - 1) % (sStudied.size()), (numiter + 3) % (sStudied.size()));
                            break;
                        case 4:
                            Sequence.simpleSwap(sStudied, (numiter - 1) % (sStudied.size()), (numiter + 5) % (sStudied.size()));
                            break;
                        case 5:
                            Sequence.simpleSwap(sStudied, (numiter - 1) % (sStudied.size()), (numiter + 7) % (sStudied.size()));
                            break;
                        case 6:
                            Sequence.reverseSubsequence(sStudied, (numiter - 1) % (sStudied.size()), (numiter + 3) % (sStudied.size()));
                            break;
                        case 7:
                            Sequence.reverseSubsequence(sStudied, (numiter - 1) % (sStudied.size()), (numiter + 5) % (sStudied.size()));
                            break;
                        case 8:
                            Sequence.reverseSubsequence(sStudied, (numiter - 1) % (sStudied.size()), (numiter + 7) % (sStudied.size()));
                            break;
                    }
                    //Compare the values of the objective function for the two sequences
                    dif = fO(sStudied, false) - fO(scur, false);
                    // if the value of the studied sequence is smaller than the current sequence
                    if (dif <= 0) {
                        //the studied sequence replace the current sequence 
                        //Empty the current sequence
                        scur = new ArrayList();
                        //The patients of the sequence studied are copied into the current sequence
                        for (Patient p : sStudied) {
                            //Each patient is added one by one
                            scur.add(p);
                        }
                        //Compare the value of the best sequence and the current sequence 
                        double dif2 = fO(scur, false) - fO(minb, false);
                        //if the value of the current sequence is smaller than the best sequence
                        if (dif2 < 0) {
                            //The current sequence replace the best sequence
                            //Empty the best sequence
                            minb = new ArrayList();
                            //The patients of the sequence studied are copied into the best sequence
                            for (Patient d : scur) {
                                //Each patient is added one by one
                                minb.add(d);
                            }
                            numiterBest = itermax * numtemp + numiter;
                            currentRuntime = (System.nanoTime() - startRuntime) / pow(10, 9);
                            writerAnnealing.write("Improved optimum found in the neighbourhood: " + fO(minb, false) + " Current total of generated sequences to get this optimum: " + numiterBest + " Current runtime : " + currentRuntime + " s." + System.getProperty("line.separator"));
                        }

                        //else if the schedule of the studied sequence is feasible but its value is bigger than the current sequence 
                    } else if (dif < pow(10, 9)) {
                        // a random number is selected between 0 and 1  
                        rd = Math.random();
                        // The Boltzman equation determine a value according to the temperature with a Boltzman constant of 1 
                        double choice = exp(-dif / temperature);
                        //if the random value is smaller than the value of the Boltzman equation  
                        if (rd < choice) {
                            //the studied sequence replace the current sequence 
                            //Empty the current sequence
                            scur = new ArrayList();
                             //The patients of the sequence studied are copied into the current sequence
                            for (Patient p : sStudied) {
                                //Each patient is added one by one
                                scur.add(p);
                            }
                        }
                    }

                } else {
                    // the temperature decrease according to the value of the cooling rate 
                    temperature = coolingRate * temperature;
                    //the number of iteration is set back to zero 
                    numiter = 0;
                    numtemp++;
                    if (temperature > tempmin) {
                        writerAnnealing.write(System.getProperty("line.separator"));
                        writerAnnealing.write(System.getProperty("line.separator"));
                        writerAnnealing.write("Temperature : " + temperature + System.getProperty("line.separator"));
                        writerAnnealing.write(System.getProperty("line.separator"));
                    }
                }
                // the number of iterations increases 
                numiter++;
            }
            totalRuntime = (System.nanoTime() - startRuntime) / pow(10, 9);
            sequenceDisplay = new ArrayList();
            for (Patient p : minb) {
                sequenceDisplay.add(p.getPatientID());
            }
            writerAnnealing.write(System.getProperty("line.separator"));
            writerAnnealing.write("Best solution proposed by the algorithm : " + sequenceDisplay + System.getProperty("line.separator"));
            writerAnnealing.write("Objective function value associed : " + fO(minb, false) + System.getProperty("line.separator"));
            writerAnnealing.write("Found in " + currentRuntime + " s. on a total runtime of " + totalRuntime + " s." + System.getProperty("line.separator"));
            writerAnnealing.write("This solution was reached by generating " + numiterBest + " sequences on a fixed total of " + numtemp * itermax + " generated sequences." + System.getProperty("line.separator"));
            writerAnnealing.write(System.getProperty("line.separator"));
            writerAnnealing.close();

            writerAnnealingStatTime.write(currentRuntime + System.getProperty("line.separator"));
            writerAnnealingStatTime.close();
            writerAnnealingStatSequence.write(numiterBest + System.getProperty("line.separator"));
            writerAnnealingStatSequence.close();
            writerAnnealingStatValue.write(fO(minb, false) + System.getProperty("line.separator"));
            writerAnnealingStatValue.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return minb;
    }

    /** 
     * GRASP algorithm used to find the minimum of the objective function 
     * without the use of a Restricted Choice List 
     * 
     * @param nbIteration Number of generation of a new sequence 
     * @param maxNonImprov Number of swap without improvement before the local 
     * search stops 
     * @param scur Initial Sequence of patients 
     * @return Best Sequence found with the minimum value of the objective function 
     *  
     * @see Patient 
     * @see System#natoTime 
     * @see System#getProperty(java.lang.String)   
     * @see Sequence#weightedInitialSequence(java.util.List) 
     * @see #fO(java.util.List, boolean)  
     * @see #randomizedConstruction(java.util.List)  
     * @see #localSearch(java.util.List, int, double, java.io.Writer)  
     * @see BufferedWriter#BufferedWriter(java.io.Writer) 
     * @see Writer#Writer(java.lang.Object) 
     * @see OutputStreamWriter#OutputStreamWriter(java.io.OutputStream) 
     * @see FileOutputStream#FileOutputStream(java.lang.String, boolean)  
     * @see BufferedWriter#write(java.lang.String)  
     * @see BufferedWriter#close()  
     */ 
    public List<Patient> grasp(int nbIteration, int maxNonImprov, List<Patient> scur, int swap) {
        double startRuntime = System.nanoTime();
        double currentRuntime1 = System.nanoTime();
        double currentRuntime2 = System.nanoTime();
        double totalRuntime;
        int totaliterBestGrasp = 0;
        List<String> sequenceDisplay = new ArrayList();
        for (Patient p : scur) {
            sequenceDisplay.add(p.getPatientID());
        }
        //This variable is used to keep the sequence with the smallest value of the objective function 
        List<Patient> bestPosition = new ArrayList();
        //This variable is used to keep the content of the current sequence when needed
        List<Patient> backUp = new ArrayList();
        //Set initial sequence as best sequence 
        for (Patient p : scur) { 
            //Each patient is added one by one
            bestPosition.add(p); 
        } 
        int i = 0;
        totaliterGrasp = 0;
        try (Writer writerGrasp = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("improvementGrasp.txt", true)));
                Writer writerGraspStatTime = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("improvementGraspStatTime.txt", true)));
                Writer writerGraspStatSequence = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("improvementGraspStatSequence.txt", true)));
                Writer writerGraspStatValue = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("improvementGraspStatValue.txt", true)))) {
            if (fO(bestPosition, false) == Double.MAX_VALUE) {
                writerGrasp.write("////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////" + System.getProperty("line.separator"));
                writerGrasp.write(System.getProperty("line.separator"));
                writerGrasp.write("NEW RUN" + System.getProperty("line.separator"));
                writerGrasp.write(System.getProperty("line.separator"));
                writerGrasp.write("Initial solution S0 : " + sequenceDisplay + System.getProperty("line.separator"));
                writerGrasp.write("Objective function value of the initial solution : The initial sequence is not schedulable" + System.getProperty("line.separator"));
                writerGrasp.write("Neighbourhood search method used: Type " + swap + ". Refer to the documentation to know the corresponding method." + System.getProperty("line.separator"));
                writerGrasp.write(System.getProperty("line.separator"));
            } else {
                writerGrasp.write("////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////" + System.getProperty("line.separator"));
                writerGrasp.write(System.getProperty("line.separator"));
                writerGrasp.write("NEW RUN" + System.getProperty("line.separator"));
                writerGrasp.write(System.getProperty("line.separator"));
                writerGrasp.write("Initial solution S0 : " + sequenceDisplay + System.getProperty("line.separator"));
                writerGrasp.write("Objective function value of the initial solution : " + fO(bestPosition, false) + System.getProperty("line.separator"));
                writerGrasp.write("Neighbourhood search method used: Type " + swap + ". Refer to the documentation to know the corresponding method." + System.getProperty("line.separator"));
                writerGrasp.write(System.getProperty("line.separator"));
            }

            //while i as not reached the number of iteration 
            while (i < nbIteration) { 
//Creation of a random new sequence  
                //The back up list is emptied
                backUp = new ArrayList();
                //The content of the current sequence is put into the back up list
                for (Patient p : scur) {
                    //Each patient is added one by one
                    backUp.add(p);
                }
                //The current sequence is emptied
                scur = new ArrayList();
                //The back up list is used to generate a randomized constructed sequence which is put inside the current list
                for (Patient p : randomizedConstruction(backUp)) {
                    //Each patient is added one by one
                    scur.add(p);
                }
// 
 
//Find the best sequence in the neighborhood of this sequence 
                //The back up list is emptied
                backUp = new ArrayList();
                //The current is used to find the best sequence in its neighborhood which is than put inside the current list
                for (Patient p : localSearch(scur, maxNonImprov, startRuntime, writerGrasp, swap)) {
                    //Each patient is added one by one
                    backUp.add(p);
                }
                //The current sequence is emptied
                scur = new ArrayList();
                //The best sequence is in the back up list is put in the current sequence
                for (Patient p : backUp) {
                    //Each patient is added one by one
                    scur.add(p);
                }
// 
                //if the value of the current sequence is smaller than the best sequence 
                if (fO(scur, false) < fO(bestPosition, false)) {
                    
//This sequence is set as best sequence
                    //The best sequence is emptied
                    bestPosition = new ArrayList();
                    //The content of the current sequence is put in the best sequence
                    for (Patient p : scur) {
                        //Each patient is added one by one
                        bestPosition.add(p);
                    }
// 
                    currentRuntime1 = (System.nanoTime() - startRuntime) / pow(10, 9);
                    totaliterBestGrasp = totaliterBest;
                    writerGrasp.write(System.getProperty("line.separator"));
                    writerGrasp.write("==> New update of the optimum : " + fO(bestPosition, false) + " Minimum number of generated sequences to get this optimum : " + totaliterBest + " Current runtime : " + currentRuntime1 + " s." + System.getProperty("line.separator"));
                    writerGrasp.write(System.getProperty("line.separator"));
                    writerGrasp.write(System.getProperty("line.separator"));
                } else {
                    currentRuntime2 = (System.nanoTime() - startRuntime) / pow(10, 9);
                    writerGrasp.write(System.getProperty("line.separator"));
                    writerGrasp.write("==> No update of the optimum." + " Current runtime : " + currentRuntime2 + " s." + System.getProperty("line.separator"));
                    writerGrasp.write(System.getProperty("line.separator"));
                    writerGrasp.write(System.getProperty("line.separator"));
                }
                i++;
            }
            totalRuntime = (System.nanoTime() - startRuntime) / pow(10, 9);
            sequenceDisplay = new ArrayList();
            for (Patient p : bestPosition) {
                sequenceDisplay.add(p.getPatientID());
            }
            writerGrasp.write(System.getProperty("line.separator"));
            writerGrasp.write("Best solution proposed by the algorithm : " + sequenceDisplay + System.getProperty("line.separator"));
            writerGrasp.write("Objective function value associed : " + fO(bestPosition, false) + System.getProperty("line.separator"));
            writerGrasp.write("Found in " + currentRuntime1 + " s. on a total runtime of " + totalRuntime + " s." + System.getProperty("line.separator"));
            writerGrasp.write("This solution was reached by generating " + totaliterBestGrasp + " sequences on a total of " + totaliterGrasp + " generated sequences." + System.getProperty("line.separator"));
            writerGrasp.write(System.getProperty("line.separator"));
            writerGrasp.close();

            writerGraspStatTime.write(currentRuntime1 + System.getProperty("line.separator"));
            writerGraspStatTime.close();
            writerGraspStatSequence.write(totaliterBestGrasp + System.getProperty("line.separator"));
            writerGraspStatSequence.close();
            writerGraspStatValue.write(fO(bestPosition, false) + System.getProperty("line.separator"));
            writerGraspStatValue.close();

        } catch (IOException e) {
            e.printStackTrace();

        }
        return bestPosition;
    }

    /** 
     * Local search of the random sequence generated find if there is a schedule with smaller value of objective function 
     * @param scur Initial sequence  
     * @param maxNonImprov Number of swap without improvement before the local 
     * search stops 
     * @param startRuntime  
     * @param writer 
     * @return the best sequence of the local Search 
     *  
     * @see Sequence#deterministicSwap(java.util.List, int, int)  
     * @see #fO(java.util.List, boolean)  
     * @see Math#random() 
     * @see BufferedWriter#BufferedWriter(java.io.Writer) 
     * @see Writer#Writer(java.lang.Object) 
     * @see OutputStreamWriter#OutputStreamWriter(java.io.OutputStream) 
     * @see FileOutputStream#FileOutputStream(java.lang.String, boolean)  
     * @see BufferedWriter#write(java.lang.String)  
     * @see BufferedWriter#close()  
     */ 
    public List<Patient> localSearch(List<Patient> scur, int maxNonImprov, double startRuntime, Writer writer, int swap) {
//Initialization of the initial sequence as the best sequence 
        //This variable is used to keep the sequence with the smallest value of the objective function 
        List<Patient> bestPosition = new ArrayList();
        //The initial sequence is considered as the best sequence
        for (Patient p : scur) {
            //Each patient of the initial sequence is added one by one in the best sequence
            bestPosition.add(p);
        }
//
        int numiter = 1;
        int numiterBest = 0;
        totaliterBest = totaliterGrasp;
        try {
            if (fO(bestPosition, false) == Double.MAX_VALUE) {
                writer.write("-------------------------------------------------------------------------------------------------------------------------------" + System.getProperty("line.separator"));
                writer.write(System.getProperty("line.separator"));
                writer.write("Value of the new random sequence : This random sequence is not schedulable" + System.getProperty("line.separator"));
                writer.write("--> Search for improvement in the neighbourhood" + System.getProperty("line.separator"));
                writer.write(System.getProperty("line.separator"));
            } else {
                writer.write("-------------------------------------------------------------------------------------------------------------------------------" + System.getProperty("line.separator"));
                writer.write(System.getProperty("line.separator"));
                writer.write("Value of the new random sequence : " + fO(bestPosition, false) + System.getProperty("line.separator"));
                writer.write("--> Search for improvement in the neighbourhood" + System.getProperty("line.separator"));
                writer.write(System.getProperty("line.separator"));
            }
            // while the number of iteration without improvement has not reached the maximum
            while (numiter <= maxNonImprov) {               
                switch (swap) {
                    case 1:
                        Sequence.simpleSwap(scur, (numiter - 1) % (scur.size()), numiter % (scur.size()));
                        break;
                    case 2:
                        Sequence.simpleSwap(scur, (numiter - 1) % (scur.size()), (numiter + 1) % (scur.size()));
                        break;
                    case 3:
                        Sequence.simpleSwap(scur, (numiter - 1) % (scur.size()), (numiter + 3) % (scur.size()));
                        break;
                    case 4:
                        Sequence.simpleSwap(scur, (numiter - 1) % (scur.size()), (numiter + 5) % (scur.size()));
                        break;
                    case 5:
                        Sequence.simpleSwap(scur, (numiter - 1) % (scur.size()), (numiter + 7) % (scur.size()));
                        break;
                    case 6:
                        Sequence.reverseSubsequence(scur, (numiter - 1) % (scur.size()), (numiter + 3) % (scur.size()));
                        break;
                    case 7:
                        Sequence.reverseSubsequence(scur, (numiter - 1) % (scur.size()), (numiter + 5) % (scur.size()));
                        break;
                    case 8:
                        Sequence.reverseSubsequence(scur, (numiter - 1) % (scur.size()), (numiter + 7) % (scur.size()));
                        break;
                }
                //if the value of the new sequence is smaller than the value of the best sequence 
                if (fO(scur, false) < fO(bestPosition, false)) {
                    totaliterGrasp++;
                    numiterBest += numiter;
                    //set number of iteration to one
                    numiter = 1;

//Set the new sequence as best sequence
                    //The best sequence is emptied
                    bestPosition = new ArrayList();
                    //The content of the current sequence is put in the best sequence
                    for (Patient p : scur) {
                        //Each patient is added one by one
                        bestPosition.add(p);
                    }
//
                    double currentRuntime = (System.nanoTime() - startRuntime) / pow(10, 9);
                    writer.write("Improved value found in the neighbourhood : " + fO(bestPosition, false) + " Current total of generated sequences in this try : " + numiterBest + " Current runtime : " + currentRuntime + " s." + System.getProperty("line.separator"));
                } else {
                    //value of number of iteration without improvement increase 
                    totaliterGrasp++;
                    numiter++;
                }
            }
            totaliterBest += numiterBest;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bestPosition;
    }

    /** 
     * Function generating a sequence Randomly 
     * @param list List of patient to put in the generated sequence 
     * @return Random sequence 
     *  
     * @see Patient 
     * @see Random 
     */ 
    public List<Patient> randomizedConstruction(List<Patient> list) {
        //this variable is the sequence that will returned at the end of the function
        List<Patient> sequence = new ArrayList();
//Create a list of possibilities 
        //this variable will be used to serve as way to pick patients during the generation of the sequence
        List<Patient> patientList = new ArrayList();
        //The content of the initial list is put inside the list of possibilities
        for (Patient p : list) {
            //Each patient is added one by one
            patientList.add(p);
        }
//
        //This variable will be used for the randomness of the process
        Random rand = new Random();
        //This variable will be used to have the attributes of the patient which was picked
        Patient randomElement; 
 
       //Fill the random sequence 
        while (sequence.size() < list.size()) {  
            //Select a random patient in the list of possibilities 
            randomElement = patientList.get(rand.nextInt(patientList.size())); 
            //Add it to the sequence 
            sequence.add(randomElement); 
            //Remove it frome the list of possibilities 
            patientList.remove(randomElement); 
        } 
        return sequence; 
    }

    /** 
    * GRASP algorithm used to find the minimum of the objective function 
     * with the use of a Restricted Choice List with the Cancellation likelihood 
    * @param greediness Tolerance of cost depending on the Cancellation likelihood 
    * @param nbIteration Number of pseudo randomly generated sequence 
    * @param maxNonImprov Number of swap without improvement before the local search stops 
    * @param scur Initial Sequence 
    * @return The best Sequence in term of value of the objective function 
    *  
    * @see System#nanoTime()  
    * @see Patient 
    * @see #localSearch(java.util.List, int, double, java.io.Writer)  
    * @see #greedyRandomizedConstruction(double, java.util.List)  
    * @see Sequence#weightedInitialSequence(java.util.List)  
    * @see BufferedWriter#BufferedWriter(java.io.Writer) 
     * @see Writer#Writer(java.lang.Object) 
     * @see OutputStreamWriter#OutputStreamWriter(java.io.OutputStream) 
     * @see FileOutputStream#FileOutputStream(java.lang.String, boolean)  
     * @see BufferedWriter#write(java.lang.String)  
     * @see BufferedWriter#close()  
    */ 
    public List<Patient> graspRCL(double greediness, int nbIteration, int maxNonImprov, List<Patient> scur, int swap) {      
        double startRuntime = System.nanoTime();
        double currentRuntime1 = System.nanoTime();
        double currentRuntime2 = System.nanoTime();
        double totalRuntime;
        int totaliterBestGrasp = 0;
        List<String> sequenceDisplay = new ArrayList();
        for (Patient p : scur) {
            sequenceDisplay.add(p.getPatientID());
        }
        //This variable is used to keep the sequence with the smallest value of the objective function 
        List<Patient> bestPosition = new ArrayList();
        //This variable will be used to keep values in case of swaps or random generation
        List<Patient> backUp = new ArrayList();
        
        //The initial sequence is considered as the best sequence
        for (Patient p : scur) {
            //Each patient is added one by one in best sequence
            bestPosition.add(p);
        }
        int i = 0;
        totaliterGrasp = 0;
        try (Writer writerGraspRCL = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("improvementGraspRCL.txt", true)));
                Writer writerGraspRCLStatTime = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("improvementGraspRCLStatTime.txt", true)));
                Writer writerGraspRCLStatSequence = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("improvementGraspRCLStatSequence.txt", true)));
                Writer writerGraspRCLStatValue = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("improvementGraspRCLStatValue.txt", true)))) {
            if (fO(bestPosition, false) == Double.MAX_VALUE) {
                writerGraspRCL.write("////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////" + System.getProperty("line.separator"));
                writerGraspRCL.write(System.getProperty("line.separator"));
                writerGraspRCL.write("NEW RUN" + System.getProperty("line.separator"));
                writerGraspRCL.write(System.getProperty("line.separator"));
                writerGraspRCL.write("Initial solution S0 : " + sequenceDisplay + System.getProperty("line.separator"));
                writerGraspRCL.write("Objective function value of the initial solution : The initial sequence is not schedulable" + System.getProperty("line.separator"));
                writerGraspRCL.write("Neighbourhood search method used: Type " + swap + ". Refer to the documentation to know the corresponding method." + System.getProperty("line.separator"));
                writerGraspRCL.write(System.getProperty("line.separator"));
            } else {
                writerGraspRCL.write("////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////" + System.getProperty("line.separator"));
                writerGraspRCL.write(System.getProperty("line.separator"));
                writerGraspRCL.write("NEW RUN" + System.getProperty("line.separator"));
                writerGraspRCL.write(System.getProperty("line.separator"));
                writerGraspRCL.write("Initial solution S0 : " + sequenceDisplay + System.getProperty("line.separator"));
                writerGraspRCL.write("Objective function value of the initial solution : " + fO(bestPosition, false) + System.getProperty("line.separator"));
                writerGraspRCL.write("Neighbourhood search method used: Type " + swap + ". Refer to the documentation to know the corresponding method." + System.getProperty("line.separator"));
                writerGraspRCL.write(System.getProperty("line.separator"));
            }

            // while the number of sequence generated has not reached the maximum number 
            while (i < nbIteration) {
//Create a gready random sequence 
                //The back up list is emptied
                backUp = new ArrayList();
                //The content of the current sequence is put into the back up list
                for (Patient p : scur) {
                    //Each patient is added one by one
                    backUp.add(p);
                }
                //The current sequence is emptied
                scur = new ArrayList();
                //The back up list is used to generate a new sequence by greedy randomize construction, the contest is put in the current sequence
                for (Patient p : greedyRandomizedConstruction(greediness, backUp)) {
                    //Each patient is added one by one
                    scur.add(p);
                }
// 
 
//Search in the neighbourhood the best sequence 
                //The back up list is emptied
                backUp = new ArrayList();
                //The current sequence is used to find the best sequence in its neighborhood which is than put inside the current list
                for (Patient p : localSearch(scur, maxNonImprov, startRuntime, writerGraspRCL, swap)) {
                    //Each patient is added one by one
                    backUp.add(p);
                }
                //The current sequence is emptied
                scur = new ArrayList();
                //The best sequence in the back up list is put in the current sequence
                for (Patient p : backUp) {
                    //Each patient is added one by one
                    scur.add(p);
                }
// 
 
                //if the value of the current sequence is smaller than the best sequence
                if (fO(scur, false) < fO(bestPosition, false)) {
//Replace the best sequence by this sequence 
                    //The best sequence is emptied
                    bestPosition = new ArrayList();
                    //The content of the current sequence is put in the best sequence
                    for (Patient p : scur) { 
                        //Each patient is added one by one
                        bestPosition.add(p); 
                    } 
// 
                    currentRuntime1 = (System.nanoTime() - startRuntime) / pow(10, 9);
                    totaliterBestGrasp = totaliterBest;
                    writerGraspRCL.write(System.getProperty("line.separator"));
                    writerGraspRCL.write("==> New update of the optimum : " + fO(bestPosition, false) + " Minimum number of generated sequences to get this optimum : " + totaliterBest + " Current runtime : " + currentRuntime1 + " s." + System.getProperty("line.separator"));
                    writerGraspRCL.write(System.getProperty("line.separator"));
                    writerGraspRCL.write(System.getProperty("line.separator"));
                } else {
                    currentRuntime2 = (System.nanoTime() - startRuntime) / pow(10, 9);
                    writerGraspRCL.write(System.getProperty("line.separator"));
                    writerGraspRCL.write("==> No update of the optimum." + " Current runtime : " + currentRuntime2 + " s." + System.getProperty("line.separator"));
                    writerGraspRCL.write(System.getProperty("line.separator"));
                    writerGraspRCL.write(System.getProperty("line.separator"));
                }
                i++;
            }
            totalRuntime = (System.nanoTime() - startRuntime) / pow(10, 9);
            sequenceDisplay = new ArrayList();
            for (Patient p : bestPosition) {
                sequenceDisplay.add(p.getPatientID());
            }
            writerGraspRCL.write(System.getProperty("line.separator"));
            writerGraspRCL.write("Best solution proposed by the algorithm : " + sequenceDisplay + System.getProperty("line.separator"));
            writerGraspRCL.write("Objective function value associed : " + fO(bestPosition, false) + System.getProperty("line.separator"));
            writerGraspRCL.write("Found in " + currentRuntime1 + " s. on a total runtime of " + totalRuntime + " s." + System.getProperty("line.separator"));
            writerGraspRCL.write("This solution was reached by generating " + totaliterBestGrasp + " sequences on a total of " + totaliterGrasp + " generated sequences." + System.getProperty("line.separator"));
            writerGraspRCL.write(System.getProperty("line.separator"));
            writerGraspRCL.close();

            writerGraspRCLStatTime.write(currentRuntime1 + System.getProperty("line.separator"));
            writerGraspRCLStatTime.close();
            writerGraspRCLStatSequence.write(totaliterBestGrasp + System.getProperty("line.separator"));
            writerGraspRCLStatSequence.close();
            writerGraspRCLStatValue.write(fO(bestPosition, false) + System.getProperty("line.separator"));
            writerGraspRCLStatValue.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return bestPosition;
    }

    /** 
     * Construction of new sequence according to the greediness decided 
     * @param greediness Tolerance of difference between the Cancellation likelihood of the last added patient ant the potiential others  
     * @param list List of patient to be put inside the sequence generated 
     * @return Greedy Randomly generated Sequence 
     *  
     * @see Sequence#sortByCancellationLikelihood(java.util.List)  
     * @see Random#Random()  
     * @see Collections#max(java.util.Collection)  
     * @see Collections#min(java.util.Collection)  
     *  
     */ 
    public List<Patient> greedyRandomizedConstruction(double greediness, List<Patient> list) {
        //This variable is the sequence that will be filled    
        List<Patient> sequence = new ArrayList();
        //This variable is a list of the patient sorted by cancellation likelihood
        List<List> sortedCancellationLikelihoods = Sequence.sortByCancellationLikelihood(list);
//Put the patients with a low cancellation likelihood in a list 
        //this variable will contain the list of patient with a low cancellation likelihood it will be emptied as the sequence will be filled
        List<Patient> possibilitiesL = sortedCancellationLikelihoods.get(0);
        //this variable will contain the list of patient with a low cancellation likelihood it will remain as a back up
        List<Patient> possibilitiesLbackup = new ArrayList();
        //The back up list is filled with the content of the low cancellation likelihood patients
        for (Patient p : possibilitiesL) {
            //Each patient is added one by one
            possibilitiesLbackup.add(p);
        }
// 
  
//Put the patients with a high cancellation likelihood in a list  
        //this variable will contain the list of patient with a high cancellation likelihood it will be emptied as the sequence will be filled
        List<Patient> possibilitiesH = sortedCancellationLikelihoods.get(1);
        //this variable will contain the list of patient with a high cancellation likelihood it will remain as a back up
        List<Patient> possibilitiesHbackup = new ArrayList();
        //The back up list is filled with the content of the low cancellation likelihood patients
        for (Patient p : possibilitiesH) {
            //Each patient is added one by one
            possibilitiesHbackup.add(p);
        }
//
        //This variable will be to pick up randomly a patient
        Patient randomElement;
        //this variable will be used for the randomness of the action
        Random rand = new Random();
        
//Select the first patient in the low cancellation likelihood list 
        //Select randomly a position in the list of the low cancellation likelihood patients
        int firstpo = rand.nextInt(possibilitiesL.size());
        //the patient at this position is added to the sequence
        sequence.add(possibilitiesL.get(firstpo));
        //the patient at this position is removed from list of the low cancellation likelihood patients
        possibilitiesL.remove(firstpo);
// 

//While the sequence is not complete 
        while (sequence.size() < list.size()) {
            //this variable will be used a Restricted Choice List
            List<Patient> rcl = new ArrayList();
            //this variable will be used to calculate the cost for the algorithm for each patient
            List<Double> cost = new ArrayList();
             
            //if the last patient of the sequence has a high cancellation likelihood 
            if (possibilitiesHbackup.contains(sequence.get(sequence.size() - 1))) {
                //Compare the difference of cancellation likelihood with the last patient and all the remaining patients with a low cancellation likehood to create a list of cost 
                for (int h = 0; h < possibilitiesL.size(); h++) {
                    //The cost of each patient is added in the list of cost
                    cost.add(Math.abs(sequence.get(sequence.size() - 1).getCancellationLikelihood() - possibilitiesL.get(h).getCancellationLikelihood()));

                }
                                 
                //Find the maximal difference 
                double maxcost = Collections.max(cost);
                //Find the minimal difference 
                double mincost = Collections.min(cost);

//Compare all the costs according to the greediness                 
                for (int k = 0; k < possibilitiesL.size(); k++) { 
                    //if the greediness allows it  
                    if (cost.get(k) <= (mincost + greediness * (maxcost - mincost))) { 
                        //the patient is added to the Restricted Choice list 
                        rcl.add(possibilitiesL.get(k)); 
                    } 
                }
// 
 
//Pick a random patient in the Restricted Choice list 
                //this variable is used to pick one of the element of the restricted choice list
                int limit = rcl.size() - 1;
                //if the size of the restricted choice list is of one 
                if (limit == 0) {
                   //select this patient
                   randomElement = rcl.get(limit);
                    //imediatly add this patient to the sequence
                    sequence.add(randomElement);
                    
                } else {
                    //select randomly a patient from all the element of the restricted choice list
                    randomElement = rcl.get(rand.nextInt(limit));
                    //add this patient to the sequence
                    sequence.add(randomElement);
                }
// 
 
                //Remove this patient from the list of possible patients with low cancellation likelihood
                possibilitiesL.remove(possibilitiesL.indexOf(randomElement));
                 
                //if the last patient of the sequence has a low cancellation likelihood 
            } else if ((possibilitiesLbackup.contains(sequence.get(sequence.size() - 1)))) {                 
                //Compare the difference of cancellation likelihood with the last patient and all the remaining patients with a hight cancellation likehood to create a list
                for (int h = 0; h < possibilitiesH.size(); h++) {
                    cost.add(Math.abs(sequence.get(sequence.size() - 1).getCancellationLikelihood() - possibilitiesH.get(h).getCancellationLikelihood()));
                }
                 
                //Find the maximal difference 
                double maxcost = Collections.max(cost); 
                //Find the minimal difference 
                double mincost = Collections.min(cost); 
 
//Compare all the cost according to the greediness 
                for (int k = 0; k < possibilitiesH.size(); k++) { 
                    //if the greediness allows it 
                    if (cost.get(k) <= (mincost + greediness * (maxcost - mincost))) { 
                        //the patient is added to the Restricted Choice list 
                        rcl.add(possibilitiesH.get(k)); 
                    } 
                }
// 
 
//Pick a random patient in the Restricted Choice list 
                //this variable is used to pick one of the element of the restricted choice list
                int limit = rcl.size() - 1;
                //if the size of the restricted choice list is of one 
                if (limit == 0) {
                    //select this patient
                    sequence.add(rcl.get(limit));
                    //imediatly add this patient to the sequence
                    randomElement = rcl.get(limit);
                } else {
                    //select randomly a patient from all the element of the restricted choice list
                    randomElement = rcl.get(rand.nextInt(limit));
                    //add this patient to the sequence
                    sequence.add(randomElement);
                }
// 
 
                //Remove this patient from the list of possible patients with high cancellation likelihood 
                possibilitiesH.remove(possibilitiesH.indexOf(randomElement));

            }
        }
        return sequence;
    }

    /**
     * Genetic Algorithm
     *
     * @param sizePopulation size of the population studied 
     * @param nbrGeneration number of generation done before finding the best 
     * sequence 
     * @param scur Initial sequence  
     * @param startPercentage 
     * @param endPercentage 
     * @param reverse 
     * @return the sequence with the best fitness 
     *  
     * @see Patient 
     * @see System#nanoTime() 
     * @see Sequence#makeACrossingOver(java.util.List, java.util.List, int, int, boolean)  
     */ 
    public List<Patient> genetic(int sizePopulation, int nbrGeneration, List<Patient> scur, int startPercentage, int endPercentage, boolean reverse) {
        double startRuntime = System.nanoTime();
        double currentRuntime = System.nanoTime();
        double totalRuntime;
        int numiterBest = 0;
        List<String> sequenceDisplay = new ArrayList();
        for (Patient p : scur) {
            sequenceDisplay.add(p.getPatientID());
        }
        // List of Sequences considered as a population
        List<List<Patient>> population = new ArrayList();
        // Declaration of the initial sequence 
        List<Patient> bestPositionImprovement = new ArrayList();
//        List<Patient> restrictedScur = Sequence.weightedInitialSequence(scur);
//        scur = new ArrayList();
//        for (Patient p : restrictedScur) {
//            scur.add(p);
//        }
        for (Patient p : scur) {
            bestPositionImprovement.add(p);
        }
        //Initialization of the two lists used for the parents
        //the best population 1 is the mother
        List<Patient> bestPopulation1;
        //the best population 2 is the father
        List<Patient> bestPopulation2;

        //Filling of the population by random sequences
        Random rd = new Random();
        List<Patient> randomPatients;
        List<Patient> possiblePatient = new ArrayList();
        Patient randomPatient;
        population.add(scur);

        try (Writer writerGenetic = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("improvementGenetic.txt", true)));
                Writer writerGeneticStatTime = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("improvementGeneticStatTime.txt", true)));
                Writer writerGeneticStatSequence = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("improvementGeneticStatSequence.txt", true)));
                Writer writerGeneticStatValue = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream("improvementGeneticStatValue.txt", true)))) {
            if (fO(bestPositionImprovement, false) == Double.MAX_VALUE) {
                writerGenetic.write("Initial solution S0 : " + sequenceDisplay + System.getProperty("line.separator"));
                writerGenetic.write("Objective function value of the initial solution : The initial sequence is not schedulable" + System.getProperty("line.separator"));
                writerGenetic.write(System.getProperty("line.separator"));
            } else {
                writerGenetic.write("Initial solution S0 : " + sequenceDisplay + System.getProperty("line.separator"));
                writerGenetic.write("Objective function value of the initial solution : " + fO(bestPositionImprovement, false) + System.getProperty("line.separator"));
                writerGenetic.write(System.getProperty("line.separator"));
            }
            while (population.size() < sizePopulation) {
                for (int i = 0; i < scur.size(); i++) {
                    possiblePatient.add(scur.get(i));
                }
                randomPatients = new ArrayList();
                while (randomPatients.size() < scur.size()) {
                    randomPatient = possiblePatient.get(rd.nextInt(possiblePatient.size()));
                    randomPatients.add(randomPatient);
                    possiblePatient.remove(randomPatient);

                }
                if (!population.contains(randomPatients)) {
                    population.add(randomPatients);
                }
            }


            /*Evolution of the population to find the sequence with the best fitness
        after a fixed number of iterations*/
            int n = 0;
            while (n < nbrGeneration) {

                //Comparison of fitness of the two first sequences of the population to set the first two parents
                //if the first sequence of the population has a smaller value of objective function than the second
                if (fO(population.get(0), false) < fO(population.get(1), false)) {
                    //the mother is initialized
                    bestPopulation1 = new ArrayList();
                    //the first sequence is considered as the mother
                    for (Patient p : population.get(0)) {
                        bestPopulation1.add(p);
                    }
                    //the father is initialized
                    bestPopulation2 = new ArrayList();
                    //the second sequence is considered as the father
                    for (Patient p : population.get(1)) {
                        bestPopulation2.add(p);
                    }
                } else {
                    //the mother is initialized
                    bestPopulation1 = new ArrayList();
                    //the second sequence is considered as the mother
                    for (Patient p : population.get(1)) {
                        bestPopulation1.add(p);
                    }
                    //the father is initialized
                    bestPopulation2 = new ArrayList();
                    //the first sequence is considered as the mother
                    for (Patient p : population.get(0)) {
                        bestPopulation2.add(p);
                    }
                }

                //Examination of the population to find the two fittest sequences
                for (int j = 0; j < sizePopulation; j++) {
                    //This variable is  going to select each sequence of the population to study them
                    List<Patient> read = new ArrayList();
                    //the variable takes the value of the sequence placed at the position j
                    for (Patient p : population.get(j)) {
                        read.add(p);
                    }
                    //if the value of the objective function of the variable read is smaller than the value of the father
                    if (fO(read, false) < fO(bestPopulation2, false)) {
                        //if the value of the objective function of the variable read is smaller than the value of the mother
                        if (fO(read, false) < fO(bestPopulation1, false)) {
                            //the father is emptied
                            bestPopulation2 = new ArrayList();
                            //the sequence "father" is replaced by the sequence "mother"
                            for (Patient p : bestPopulation1) {
                                //Each patient one by one
                                bestPopulation2.add(p);
                            }
                            //the mother is emptied
                            bestPopulation1 = new ArrayList();
                            //the sequence "mother" is replace by the sequence "read"
                            for (Patient p : read) {
                                bestPopulation1.add(p);
                            }
                        //else if the value of the objective function of the variable read is different from the value of the mother
                        } else if (fO(read, false) != fO(bestPopulation1, false)) {
                            //the father is emptied
                            bestPopulation2 = new ArrayList();
                            //the sequence "father" is replaced by the sequence "mother"
                            for (Patient p : read) {
                                bestPopulation2.add(p);
                            }
                        }
                    }
                }

                if (fO(bestPopulation1, false) < fO(bestPositionImprovement, false)) {
                    bestPositionImprovement = new ArrayList();
                    for (Patient p : bestPopulation1) {
                        bestPositionImprovement.add(p);
                    }
                    numiterBest = n;
                    currentRuntime = (System.nanoTime() - startRuntime) / pow(10, 9);
                    writerGenetic.write("==> New update of the optimum : " + fO(bestPositionImprovement, false) + System.getProperty("line.separator"));
                    writerGenetic.write("Minimum number of generated sequences to get this optimum : " + (sizePopulation + numiterBest) + " sequences (" + sizePopulation + " sequences from the initial pool + " + numiterBest + " children)" + " Current runtime : " + currentRuntime + " s." + System.getProperty("line.separator"));
                    writerGenetic.write(System.getProperty("line.separator"));
                }

                //Realisation of the crossing over to create an offspring supposedly better than its two parents
                //Empty the list "child"
                List<Patient> child = new ArrayList();
                //Fill the child with the crossing over of the best population 1 and 2
                for (Patient p : Sequence.makeACrossingOver(bestPopulation1, bestPopulation2, startPercentage, endPercentage, reverse)) {
                    child.add(p);
                }
                //This offspring is added in the population 
                population.add(child);
                //The "father" in taken out of the population 
                population.remove(population.indexOf(bestPopulation2));

                //A Generation pass
                n++;
            }
            totalRuntime = (System.nanoTime() - startRuntime) / pow(10, 9);
            sequenceDisplay = new ArrayList();
            for (Patient p : bestPositionImprovement) {
                sequenceDisplay.add(p.getPatientID());
            }
            writerGenetic.write("Best solution proposed by the algorithm : " + sequenceDisplay + System.getProperty("line.separator"));
            writerGenetic.write("Objective function value associed : " + fO(bestPositionImprovement, false) + System.getProperty("line.separator"));
            writerGenetic.write("Found in " + currentRuntime + " s. on a total runtime of " + totalRuntime + " s." + System.getProperty("line.separator"));
            writerGenetic.write("This solution was reached by generating " + (sizePopulation + numiterBest) + " sequences (" + sizePopulation + " sequences from the initial pool + " + numiterBest + " children)," + System.getProperty("line.separator"));
            writerGenetic.write("on a total of " + (sizePopulation + nbrGeneration) + " generated sequences (" + sizePopulation + " sequences from the initial pool + " + nbrGeneration + " children).");
            writerGenetic.close();

            writerGeneticStatTime.write(currentRuntime + System.getProperty("line.separator"));
            writerGeneticStatTime.close();
            writerGeneticStatSequence.write((sizePopulation + numiterBest) + System.getProperty("line.separator"));
            writerGeneticStatSequence.close();
            writerGeneticStatValue.write(fO(bestPositionImprovement, false) + System.getProperty("line.separator"));
            writerGeneticStatValue.close();

        } catch (IOException e) {
            e.printStackTrace();

        }
        return bestPositionImprovement;
    }
}
