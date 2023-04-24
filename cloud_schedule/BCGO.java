package org.cloudbus.cloudsim.examples.myscheduler;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.examples.C;
import org.cloudbus.cloudsim.examples.V;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 天牛群遗传算法主类规定算法流程
 */
public class BCGO {
    private Beetle[] beetles; // 天牛
    private int cloudletNum; // 任务数量
    private int vmNum; // 虚拟机数量

    private double[][] distance; // 距离矩阵
    private double[][] pheromone; // 信息素矩阵

    private double p1, p2, d;//更新概率，局部搜索概率 天牛须距离
    private int beetleNum; // 天牛数量
    private int generation; // 迭代次数
    private double alpha; // 信息素重要程度系数
    private double beta; // 路径间距离重要程度系数

    private double[] makespan; //记录每轮迭代的最优完成时间
    private double[] aveWatitTime; //记录每轮迭代的平均等待时间
    private List<List<Integer>> solution; //记录每轮迭代的最佳方案
    private DatacenterBroker datacenterBroker = null;
    /**
     * 构造方法
     * @param beetleNum
     * @param generation
     * @param alpha
     * @param beta
     * @param p1
     * @param p1
     */

    public BCGO(int beetleNum, int generation, double alpha, double beta, double p1, double p2, double d, DatacenterBroker datacenterBroker) {
        this.datacenterBroker =datacenterBroker;
        this.beetleNum = beetleNum;
        this.generation = generation;
        this.alpha = alpha;
        this.beta = beta;
        this.makespan = new double[generation];
        this.aveWatitTime = new double[generation];
        this.beetles = new Beetle[beetleNum];
        this.solution = new ArrayList<>();
        this.p1 = p1;
        this.p2 = p2;
        this.d = d;
    }
    /**
     * 初始化
     * @param
     * @throws
     */
    public void init(List<Cloudlet> cloudletList, List<Vm> vmList) {
        this.cloudletNum = cloudletList.size();
        this.vmNum = vmList.size();
        this.distance = new double[cloudletNum][vmNum];
        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                this.distance[i][j] = (double) cloudletList.get(i).getCloudletLength() / vmList.get(j).getMips();
            }
        }

        // 初始化信息素矩阵
        pheromone = new double[cloudletNum][vmNum];
        double sum_mips = 0;
        for (int j = 0; j < vmNum; j++) {
            sum_mips += vmList.get(j).getMips();
        }
        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                pheromone[i][j] = vmList.get(j).getMips()/sum_mips;
            }
        }

        // 初始化beetleNum个天牛
        for (int i = 0; i < beetleNum; i++) {
            beetles[i] = new Beetle(cloudletNum, vmNum, this.distance);
            beetles[i].init(distance, alpha, beta);
        }
    }
    
    
    public void solve() {
        // 迭代generation次
        Random rand = new Random();
        for (int g = 0; g < generation; g++) {
            // 对beetleNum只天牛分别进行操作
            double makespan_min = Double.MAX_VALUE;
            int best_ant = -1;
            for (int Beetle = 0; Beetle < beetleNum; Beetle++) {
                if(g == 0){
                    while (beetles[Beetle].getTask_list().size() < cloudletNum) {
                        int n =  rand.nextInt(cloudletNum);
                        while(beetles[Beetle].getTask_list().contains(n)){
                            n =  rand.nextInt(cloudletNum);
                        }
                        beetles[Beetle].initSolution(n, pheromone);
                    }
                }else{
                    beetles[Beetle].selectVMs_DABS(d, p1, p2,this.solution.get(this.solution.size()-1));
                }
                double makespan_ant = beetles[Beetle].getMakespan();
                if(makespan_ant < makespan_min){
                    best_ant = Beetle;
                    makespan_min = makespan_ant;
                }
            }
            this.aveWatitTime[g] = beetles[best_ant].getAveWaitTime();
            this.makespan[g] = makespan_min;//保存本轮迭代最优值
            this.solution.add(new ArrayList<>(beetles[best_ant].getTabu()));

            d = Math.floor(1+d * 0.996);
            print(g);
        }
        System.out.print("DABS search finished!!!   ");
        print(generation);
    }


    /**
     * 在控制台中输出最佳长度及最佳路径
     */
    private void print(int iter) {
        if(iter == generation){
            double min = Double.MAX_VALUE;
            int index = -1;
            for(int i = 0; i < generation; i++){
                if(min > makespan[i]){
                    min = makespan[i];
                    index = i;
                }
            }
            System.out.print("全局最佳完成时间: " + makespan[index] + " 平均等待时间" + aveWatitTime[index]);
            try{
                BufferedWriter out = new BufferedWriter(new FileWriter("BCGO_result.txt",true));
                out.write(makespan[index]+"\t"+aveWatitTime[index]+"\n");
                out.close();
            }catch (Exception e){

            }
//            System.out.print("全局最佳分配: ");
            for (int i = 0; i < solution.get(index).size(); i++) {
                //分配任务到虚拟机
                datacenterBroker.bindCloudletToVm(i,solution.get(index).get(i));
                //System.out.print(solution.get(index).get(i) + "-");
            }
            System.out.println();
            return;
        }
        //System.out.print(iter + " "+"最佳完成时间: " + makespan[iter]);
//        System.out.print("最佳分配: ");
//        for (int i = 0; i < solution.get(iter).size(); i++) {
//            System.out.print(solution.get(iter).get(i) + "-");
//        }
       // System.out.println();
    }


}
