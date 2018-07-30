
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import static java.lang.Math.pow;
import java.util.ArrayList;
import java.util.List;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author robin
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        List<Patient> arrivalSequence = new ArrayList();
        Patient p0 = new Patient("P0", "PR1", 85, 98);
        Patient p1 = new Patient("P1", "PR1", 22, 38);
        Patient p2 = new Patient("P2", "PR2", 102, 105);
        Patient p3 = new Patient("P3", "PR1", 68, 13);
        Patient p4 = new Patient("P4", "PR1", 19, 5);
        Patient p5 = new Patient("P5", "PR1", 29, 56);
        Patient p6 = new Patient("P6", "PR2", 23, 118);
        Patient p7 = new Patient("P7", "PR1", 86, 24);
        Patient p8 = new Patient("P8", "PR1", 27, 93);
        Patient p9 = new Patient("P9", "PR1", 76, 72);
        arrivalSequence.add(p6);
        arrivalSequence.add(p3);
        arrivalSequence.add(p9);
        arrivalSequence.add(p0);
        arrivalSequence.add(p4);
        arrivalSequence.add(p5);
        arrivalSequence.add(p7);
        arrivalSequence.add(p2);
        arrivalSequence.add(p1);
        arrivalSequence.add(p8);

        Schedule s = new Schedule(arrivalSequence);
        Functions f = new Functions(s);
        
        long t_0A = System.nanoTime();
        List<Patient> best = f.annealingMin(100, 20, 100, arrivalSequence);
        long t_1A = System.nanoTime();
        if (f.fO(best, false) != Double.MAX_VALUE) {
            System.out.println(best + " " + f.fO(best, false));
            System.out.println("Length of the algorithm annealing: " + (t_1A - t_0A) / pow(10, 9) + " s.");
        } else {
            System.out.println("You don't have enough resources to create a full schedule or you don't test enough sequences for this algorithm. Please add more resources, reduce the number of patients to add to the schedule or test more sequences.");
        }

        long t_0GR = System.nanoTime();
        List<Patient> grasp = f.grasp(20, 45, arrivalSequence);
        long t_1GR = System.nanoTime();
        if (f.fO(grasp, false) != Double.MAX_VALUE) {
            System.out.println(grasp + " " + f.fO(grasp, false));
            System.out.println("Length of the algorithm GRASP: " + (t_1GR - t_0GR) / pow(10, 9) + " s.");
        } else {
            System.out.println("You don't have enough resources to create a full schedule or you don't test enough sequences. Please add more resources, reduce the number of patients to add to the schedule or test more sequences.");
        }

        long t_0GRCL = System.nanoTime();
        List<Patient> graspRCL = f.graspRCL(0.4, 20, 45, arrivalSequence);
        long t_1GRCL = System.nanoTime();
        if (f.fO(graspRCL, false) != Double.MAX_VALUE) {
            System.out.println(graspRCL + " " + f.fO(graspRCL, false));
            System.out.println("Length of the algorithm GRASP RCL: " + (t_1GRCL - t_0GRCL) / pow(10, 9) + " s.");
        } else {
            System.out.println("You don't have enough resources to create a full schedule or you don't test enough sequences. Please add more resources, reduce the number of patients to add to the schedule or test more sequences.");
        }

        long t_0G = System.nanoTime();
        List<Patient> gene = f.genetic(50, 4, arrivalSequence, 20);
        long t_1G = System.nanoTime();
        if (f.fO(gene, false) != Double.MAX_VALUE) {
            System.out.println(gene + " " + f.fO(gene, false));
            System.out.println("Length of the algorithm Genetic: " + (t_1G - t_0G) / pow(10, 9) + " s.");
        } else {
            System.out.println("You don't have enough resources to create a full schedule or you don't test enough sequences. Please add more resources, reduce the number of patients to add to the schedule or test more sequences.");
        }
    }

}
