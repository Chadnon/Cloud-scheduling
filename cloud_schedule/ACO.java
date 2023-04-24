package org.cloudbus.cloudsim.examples.myscheduler;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.examples.myscheduler.Ant;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 蚁群算法主类 规定算法流程
 */
public class ACO {
    private Ant[] ants; // 蚂蚁
    private int cloudletNum; // 任务数量
    private int vmNum; // 虚拟机数量

    private double[][] distance; // 距离矩阵
    private double[][] pheromone; // 信息素矩阵


    private int antNum; // 蚂蚁数量
    private int generation; // 迭代次数
    private double alpha; // 信息素重要程度系数
    private double beta; // 城市间距离重要程度系数
    private double rho; // 信息素残留系数
    private int Q; // 蚂蚁循环一周在经过的路径上所释放的信息素总量
    private int deltaType; // 信息素更新方式模型，0: Ant-quantity; 1: Ant-density; 2: Ant-cycle

    private double[] makespan; //记录每轮迭代的最优完成时间
    private double[] aveWatitTime; //记录每轮迭代的平均等待时间
    private List<List<Integer>> solution; //记录每轮迭代的最佳方案
    private DatacenterBroker datacenterBroker = null;
    /**
     * 构造方法
     * @param antNum
     * @param generation
     * @param alpha
     * @param beta
     * @param rho
     * @param Q
     */
    public ACO(int antNum, int generation, double alpha, double beta, double rho, int Q, int deltaType, DatacenterBroker datacenterBroker) {
        this.datacenterBroker = datacenterBroker;
        this.antNum = antNum;
        this.generation = generation;
        this.alpha = alpha;
        this.beta = beta;
        this.rho = rho;
        this.Q = Q;
        this.deltaType = deltaType;
        this.makespan = new double[generation];
        this.aveWatitTime = new double[generation];
        this.ants = new Ant[antNum];
        this.solution = new ArrayList<>();
    }

    /**
     * 初始化
     * @param
     * @throws IOException
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

        // 初始化antNum个蚂蚁
        for (int i = 0; i < antNum; i++) {
            ants[i] = new Ant(cloudletNum, vmNum, this.distance);
            ants[i].init(distance, alpha, beta, 0);
        }
    }


    /**
     *
     */
    public void solve() {
        // 迭代generation次
        Random rand = new Random();
        for (int g = 0; g < generation; g++) {
            // 对antNum只蚂蚁分别进行操作
            double makespan_min = Double.MAX_VALUE;
            int best_ant = -1;
            for (int ant = 0; ant < antNum; ant++) {
                // 为每只蚂蚁分别选择一个分配方案
                while (ants[ant].getTask_list().size() < cloudletNum) {
                    int n =  rand.nextInt(cloudletNum);
                    while(ants[ant].getTask_list().contains(n)){
                        n =  rand.nextInt(cloudletNum);
                    }
                    ants[ant].selectNextVM_ACO(n, pheromone);
                }

                double makespan_ant = ants[ant].getMakespan();
                if(makespan_ant < makespan_min){
                    best_ant = ant;
                    makespan_min = makespan_ant;
                }
                // 更新这只蚂蚁信息素增量矩阵
                double[][] delta = ants[ant].getDelta();
                for (int i = 0; i < cloudletNum; i++) {
                    for (int j : ants[ant].getTabu()) {
                        if (deltaType == 0) {
                            delta[i][j] = Q; // Ant-quantity System
                        }
                        if (deltaType == 1) {
                            delta[i][j] = Q / distance[i][j]; // Ant-density System
                        }
                        if (deltaType == 2) {
                            delta[i][j] = Q / makespan_ant;
                        }
                    }
                }
                ants[ant].setDelta(delta);
            }
            this.aveWatitTime[g] = ants[best_ant].getAveWaitTime();
            this.makespan[g] = makespan_min;//保存本轮迭代最优值
            this.solution.add(new ArrayList<>(ants[best_ant].getTabu()));
            // 更新信息素
            updatePheromone(g);

            // 重新初始化蚂蚁
            for (int i = 0; i < antNum; i++) {
                ants[i].init(distance, alpha, beta, -1);
            }

            print(g);
        }
        System.out.print("ACO search finished!!!  ");
        // 打印最佳结果
        print(generation);
    }

    /**
     * 更新信息素
     */
    private void updatePheromone(int iter) {
        // 按照rho系数保留原有信息素
        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                pheromone[i][j] = pheromone[i][j] * rho;
            }
        }

        // 按照蚂蚁留下的信息素增量矩阵更新信息素
        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                for (int ant = 0; ant < antNum; ant++) {
                    pheromone[i][j] += ants[ant].getDelta()[i][j];
                }
            }
        }
        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                pheromone[i][j] += pheromone[i][j] + this.Q / this.makespan[iter];
            }
        }
    }

    /**
     * 在控制台中输出最佳长度及最佳路径
     */
    private void print(int iter) {
        //打印最终结果
        if(iter == generation){
            double min = Double.MAX_VALUE;
            int index = -1;
            for(int i = 0; i < generation; i++){
                if(min > makespan[i]){
                    min = makespan[i];
                    index = i;
                }
            }
            System.out.print("全局最佳完成时间: " + makespan[index]+ " 平均等待时间" + aveWatitTime[index]);
            try{
                BufferedWriter out = new BufferedWriter(new FileWriter("ACO_result.txt",true));
                out.write(makespan[index]+"\t"+aveWatitTime[index]+"\n");
                out.close();
            }catch (Exception e){

            }
           //System.out.print("全局最佳分配: ");
            for (int i = 0; i < solution.get(index).size(); i++) {
                //分配任务到虚拟机
                datacenterBroker.bindCloudletToVm(i,solution.get(index).get(i));
               // System.out.print(solution.get(index).get(i) + "-");
            }
            System.out.println();
            return;
        }
       // System.out.print(iter + " "+"最佳完成时间: " + makespan[iter]);
//        System.out.print("最佳分配: ");
//        for (int i = 0; i < solution.get(iter).size(); i++) {
//            System.out.print(solution.get(iter).get(i) + "-");
//        }
       // System.out.println();
    }


}
