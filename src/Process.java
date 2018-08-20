/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.ArrayList;
import java.util.List;

/**
 * This class aims to describe a Process
 *
 * @author Hayat
 */
public class Process {

    String processID; // variable to identity a process
    String processType; // variable for type of a process, not used in code
    String ProcessName;  // variable for process name, not used in code
    private List<Task> listTask; // list of Task objects of the process

    /**
     * Constructor initilizes variables. Method called by Data Class
     * @param processID 
     */
    public Process(String processID) {
        this.processID = processID;
        this.listTask = new ArrayList();
    }

    /**
     * @return the processID
     */
    public String getID() {
        return processID;
    }

    /**
     * @return the listTask
     */
    public List<Task> getListTask() {
        return listTask;
    }

    /**
     * Method used in class Data when getting Tasks' information
     * @param t is as Task to add in list Task
     */
    public void addListTask(Task t) {
        listTask.add(t);
    }
}
