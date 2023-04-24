package org.cloudbus.cloudsim.examples.myscheduler;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.examples.C;
import org.cloudbus.cloudsim.examples.V;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

/**
 * 他遗传算法主类
 */
public class GA {
    static List<Cloudlet> clist = new ArrayList<>();
    static List<Vm> vlist = new ArrayList<>();
    private double[] ET; //虚拟机完成时间
    static private double aveWaitTime; //记录每轮迭代的平均等待时间
    private DatacenterBroker datacenterBroker = null;
    public GA(int popSize,int gmax,double crossoverProb,double mutationRate, List<Cloudlet> clist, List<Vm> vlist){
        this.clist = clist;
        this.vlist = vlist;
        this.aveWaitTime = 0;
        ArrayList<int[]> pop=initPopsRandomly(clist.size(),vlist.size(),popSize);
        pop = GA1(pop, gmax, crossoverProb, mutationRate);
        int []schedule= findBestSchedule(pop);
        assignResourcesWithSchedule(schedule);
    }
    public GA(int popSize, int gmax, double crossoverProb, double mutationRate, List<Cloudlet> clist, List<Vm> vlist, DatacenterBroker datacenterBroker){
        this.datacenterBroker = datacenterBroker;
        this.clist = clist;
        this.vlist = vlist;
        this.aveWaitTime = 0;
        ArrayList<int[]> pop=initPopsRandomly(clist.size(),vlist.size(),popSize);
        pop = GA1(pop, gmax, crossoverProb, mutationRate);
        int []schedule= findBestSchedule(pop);
        assignResourcesWithSchedule(schedule);
    }
    public ArrayList<int[]> GA1(ArrayList<int[]> pop,int gmax,double crossoverProb,double mutationRate){
        HashMap<Integer,double[]> segmentForEach=calcSelectionProbs(pop);
        ArrayList<int[]> children=new ArrayList<int[]>();
        ArrayList<int[]> tempParents=new ArrayList<int[]>();
        while(children.size()<pop.size())
        {
            //selection phase:select two parents each time.
            for(int i=0;i<2;i++)
            {
                double prob = new Random().nextDouble();
                for (int j = 0; j < pop.size(); j++)
                {
                    if (isBetween(prob, segmentForEach.get(j)))
                    {
                        tempParents.add(pop.get(j));
                        break;
                    }
                }
            }
            //cross-over phase.
            int[] p1,p2,p1temp,p2temp;
            p1= tempParents.get(tempParents.size() - 2).clone();
            p1temp= tempParents.get(tempParents.size() - 2).clone();
            p2 = tempParents.get(tempParents.size() -1).clone();
            p2temp = tempParents.get(tempParents.size() -1).clone();
            if(new Random().nextDouble()<crossoverProb)
            {
                int crossPosition = new Random().nextInt(clist.size() - 1);
                //cross-over operation
                for (int i = crossPosition + 1; i < clist.size(); i++)
                {
                    int temp = p1temp[i];
                    p1temp[i] = p2temp[i];
                    p2temp[i] = temp;
                }
            }
            //choose the children if they are better,else keep parents in next iteration.
            children.add(getFitness(p1temp) < getFitness(p1) ? p1temp : p1);
            children.add(getFitness(p2temp) < getFitness(p2) ? p2temp : p2);
            // mutation phase.
            if (new Random().nextDouble() < mutationRate)
            {
                // mutation operations bellow.
                int maxIndex = children.size() - 1;

                for (int i = maxIndex - 1; i <= maxIndex; i++)
                {
                    operateMutation(children.get(i));
                }
            }
        }

        gmax--;
        return gmax > 0 ? GA1(children, gmax, crossoverProb, mutationRate): children;
    }
    public void assignResourcesWithSchedule(int []schedule) {
        for(int i=0;i<schedule.length;i++)
        {
            getCloudletById(i).setVmId(schedule[i]);
        }
        double best =  getFitness(schedule);
        ET = new double[this.vlist.size()];
        for(int i = 0; i < schedule.length; i++){
            //分配任务到虚拟机
            datacenterBroker.bindCloudletToVm(i, schedule[i]);
            this.aveWaitTime += ET[schedule[i]];
            ET[schedule[i]] += this.clist.get(i).getCloudletLength()/this.vlist.get(schedule[i]).getMips();
        }
        this.aveWaitTime /= this.clist.size();
        try{
            BufferedWriter out = new BufferedWriter(new FileWriter("GA_result.txt",true));
            out.write(best+"\t"+aveWaitTime+"\n");
            out.close();
        }catch (Exception e){

        }
        System.out.println("GA finished !! 全局最佳完成时间 " + best + " 平均等待时间" + aveWaitTime);
    }
    private int[] findBestSchedule(ArrayList<int[]> pop){
        double bestFitness=1000000000;
        int bestIndex=0;
        for(int i=0;i<pop.size();i++)
        {
            int []schedule=pop.get(i);
            double fitness=getFitness(schedule);
            if(bestFitness>fitness)
            {
                bestFitness=fitness;
                bestIndex=i;
            }
        }
        return pop.get(bestIndex);
    }
    private double getFitness(int[] schedule){
        double fitness=0;

        HashMap<Integer,ArrayList<Integer>> vmTasks=new HashMap<Integer,ArrayList<Integer>>();
        int size=clist.size();

        for(int i=0;i<size;i++)
        {
            if(!vmTasks.keySet().contains(schedule[i]))
            {
                ArrayList<Integer> taskList=new ArrayList<Integer>();
                taskList.add(i);
                vmTasks.put(schedule[i],taskList);
            }
            else
            {
                vmTasks.get(schedule[i]).add(i);
            }
        }

        for(Map.Entry<Integer, ArrayList<Integer>> vmtask:vmTasks.entrySet()){
            int length=0;
            for(Integer taskid:vmtask.getValue())
            {
                length+=getCloudletById(taskid).getCloudletLength();
            }

            double runtime=length/getVmById(vmtask.getKey()).getMips();
            if (fitness<runtime)
            {
                fitness=runtime;
            }
        }

        return fitness;
    }
    public Cloudlet getCloudletById(int id){
        for(Cloudlet c:clist)
        {
            if(c.getCloudletId()==id)
                return c;
        }
        return null;
    }
    public Vm getVmById(int vmId){
        for(Vm v : vlist)
        {
            if(v.getId()==vmId)
                return v;
        }
        return null;
    }
    private ArrayList<int[]> initPopsRandomly(int taskNum,int vmNum,int popsize){
        ArrayList<int[]> schedules=new ArrayList<int[]>();
        for(int i=0;i<popsize;i++)
        {
            //data structure for saving a schedule：array,index of array are cloudlet id,content of array are vm id.
            int[] schedule=new int[taskNum];
            for(int j=0;j<taskNum;j++)
            {
                schedule[j]=new Random().nextInt(vmNum);
            }
            schedules.add(schedule);
        }
        return schedules;
    }

    private HashMap<Integer,double[]> calcSelectionProbs(ArrayList<int[]> parents){
        int size=parents.size();
        double totalFitness=0;
        ArrayList<Double> fits=new ArrayList<Double>();
        HashMap<Integer,Double> probs=new HashMap<Integer,Double>();

        for(int i=0;i<size;i++)
        {
            double fitness=getFitness(parents.get(i));
            fits.add(fitness);
            totalFitness+=fitness;
        }
        for(int i=0;i<size;i++)
        {
            probs.put(i,fits.get(i)/totalFitness );
        }

        return getSegments(probs);
    }
    private HashMap<Integer,double[]> getSegments(HashMap<Integer,Double> probs) {
        HashMap<Integer,double[]> probSegments=new HashMap<Integer,double[]>();
        //probSegments保存每个个体的选择概率的起点、终点，以便选择作为交配元素。
        int size=probs.size();
        double start=0;
        double end=0;
        for(int i=0;i<size;i++)
        {
            end=start+probs.get(i);
            double[]segment=new double[2];
            segment[0]=start;
            segment[1]=end;
            probSegments.put(i, segment);
            start=end;
        }

        return probSegments;
    }
    private boolean isBetween(double prob,double[]segment){
        if(segment[0]<=prob&&prob<=segment[1])
            return true;
        return false;
    }
    public void operateMutation(int []child){
        int mutationIndex = new Random().nextInt(clist.size());
        int newVmId = new Random().nextInt(vlist.size());
        while (child[mutationIndex] == newVmId)
        {
            newVmId = new Random().nextInt(vlist.size());
        }

        child[mutationIndex] = newVmId;
    }
}
