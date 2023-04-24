package org.cloudbus.cloudsim.examples.myscheduler;

import java.util.ArrayList;
import java.util.Random;

/**
 * 蚂蚁类
 */
public class Ant {
    private ArrayList<Integer> tabu; // 分配表
    private ArrayList<Integer> task_list; // 任务顺序表
    private double[][] delta; // 信息素增量矩阵
    private double[][] distance; // 每只蚂蚁保存自己的一份距离矩阵，在每轮迭代过程中每只蚂蚁的距离矩阵会变化（受到分配方案的影响）
    private double[][] time; // 处理时间矩阵
    private double[][] eta; // 能见度矩阵
    private double[] ET; //虚拟机完成时间

    private double alpha; // 信息素重要程度系数
    private double beta; // 城市间距离重要程度系数

    private int cloudletNum; // 任务数量
    private int vmNum; // 虚拟机数量
    private double aveWaitTime;

    /**
     * 构造方法
     * @param cloudletNum
     *  @param vmNum
     */
    public Ant(int cloudletNum, int vmNum, double[][] time) {
        this.time = new double[cloudletNum][vmNum];
        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                this.time[i][j] = time[i][j];
            }
        }
        this.cloudletNum = cloudletNum;
        this.vmNum = vmNum;
    }

    /**
     * 初始化蚂蚁
     * @param distance
     * @param alpha
     * @param beta
     */
    public void init(double[][] distance, double alpha, double beta, int function_type) {
        this.alpha = alpha;
        this.beta = beta;
        this.aveWaitTime = 0;
        //每轮迭代开始时每只蚂蚁的距离矩阵都重新初始化
        this.distance = new double[cloudletNum][vmNum];
        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                this.distance[i][j] = distance[i][j];
            }
        }
        //本轮迭代的方案表，已完成分配的任务表，虚拟机已分配时间表也重新初始化
        if(function_type != 1){
            this.tabu = new ArrayList<>(cloudletNum);
            for(int i = 0; i < cloudletNum; i++){
                tabu.add(-1);
            }
        }
        this.task_list = new ArrayList<Integer>();
        this.ET = new double[vmNum];
        // 初始化信息素增量矩阵为0
        delta = new double[cloudletNum][vmNum];
        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                delta[i][j] = 0.0;
            }
        }

        // 根据距离矩阵计算能见度矩阵
        eta = new double[cloudletNum][vmNum];
        for (int i = 0; i < cloudletNum ; i++) {
            for (int j = 0; j < vmNum; j++) {
                eta[i][j] = 1.0 / this.distance[i][j];
            }
        }
    }

    /**
     * 蚁群ACO算法为任务分配虚拟机
     * @param pheromone
     */
    public void selectNextVM_ACO(int currentCloulet, double[][] pheromone) {
        this.task_list.add(currentCloulet); //将当前分配的任务加入已分配表
        double[] probability = new double[vmNum]; // 转移概率矩阵
        double sum = 0;
        for (int i = 0; i < cloudletNum ; i++) {
            for (int j = 0; j < vmNum; j++) {
                eta[i][j] = 1.0 / distance[i][j];
            }
        }
        // 计算分母
        for (int i = 0; i < vmNum; i++) {
            sum += Math.pow(pheromone[currentCloulet][i], alpha) * Math.pow(eta[currentCloulet][i], beta);
        }

        // 计算概率矩阵
        for (int i = 0; i < vmNum; i++) {
            probability[i] = (Math.pow(pheromone[currentCloulet][i], alpha) * Math.pow(eta[currentCloulet][i], beta)) / sum;
        }
        // 选择下一个VM(权重随机数算法/轮盘赌)
        int selectVm = 0;
        Random random = new Random();
        double rand = random.nextDouble();
        double sumPs = 0.0;
        for (int i = 0; i < vmNum; i++) {
            sumPs += probability[i];
            if (sumPs >= rand) {
                selectVm = i;
                break;
            }
        }

        //分配到任务的虚拟机的占用时间增加
        this.aveWaitTime += ET[selectVm];
        ET[selectVm] += time[currentCloulet][selectVm];
        for(int i = 0; i < cloudletNum; i++){
            if(!task_list.contains(i)){
                this.distance[i][selectVm] += ET[selectVm]; //将该虚拟机的占用时间加到距离矩阵
            }
        }
        tabu.set(currentCloulet,selectVm);

    }

    public ArrayList<Integer> getTabu() {
        return tabu;
    }


    public double[][] getDelta() {
        return delta;
    }

    public void setDelta(double[][] delta) {
        this.delta = delta;
    }

    public double getMakespan() {
        double max = Double.MIN_VALUE;
        for(int i = 0; i < vmNum; i++){
            if(ET[i] > max){
                max = ET[i];
            }
        }
        return max;
    }

    public ArrayList<Integer> getTask_list() {
        return task_list;
    }

    public double getAveWaitTime() {
        this.aveWaitTime /= this.cloudletNum;
        return aveWaitTime;
    }

    public void setTask_list(ArrayList<Integer> task_list) {
        this.task_list = task_list;
    }
}
