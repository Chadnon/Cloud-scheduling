package org.cloudbus.cloudsim.examples.myscheduler;

import java.util.*;

/**
 * 天牛个体
 */
public class Beetle {
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
    public Beetle(int cloudletNum, int vmNum, double[][] time) {
        this.time = new double[cloudletNum][vmNum];
        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                this.time[i][j] = time[i][j];
            }
        }
        this.tabu = new ArrayList<>(cloudletNum);
        for(int i = 0; i < cloudletNum; i++){
            tabu.add(-1);
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
    public void init(double[][] distance, double alpha, double beta) {
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
     * 初始化第一轮分配方案（随机分配） 使用蚁群算法信息素的方法
     * @param pheromone
     */
    public void initSolution(int currentCloulet, double[][] pheromone) {
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
        if(tabu.size() == currentCloulet){
            aveWaitTime /= cloudletNum;
        }
    }

    /**
     * DABS算法是一次性更新所有任务分配到的VM
     * @param d 两个天牛须距离
     * @param p1 更新概率
      @param p2 局部搜索概率
     */
    public void selectVMs_DABS(double d, double p1, double p2, List<Integer> best) {
        Random rand = new Random();
        int[] pre_solution = new int[cloudletNum];
        for(int i = 0; i < cloudletNum; i++){
            pre_solution[i] = best.get(i);
        }
        int[] xleft = new int[cloudletNum];
        int[] xright = new int[cloudletNum];
        int[] new_solution = new int[cloudletNum];
        System.arraycopy(pre_solution,0,xleft, 0, pre_solution.length);
        System.arraycopy(pre_solution,0,xright, 0, pre_solution.length);
        System.arraycopy(pre_solution,0,new_solution, 0, pre_solution.length);
        Set<Integer> indexL = new HashSet<>();
        Set<Integer> indexR = new HashSet<>();
        while(indexL.size() < d || indexR.size() < d){
            if(indexL.size() < d )
                indexL.add(rand.nextInt(cloudletNum));
            if(indexR.size() < d )
                indexR.add(rand.nextInt(cloudletNum));
        }
        for(Integer i : indexL){
            xleft[i] = rand.nextInt(vmNum);
        }
        for(Integer i : indexR){
            xright[i] = rand.nextInt(vmNum);
        }
        double left_makespan = calMakespan(xleft);
        double right_makespan = calMakespan(xright);
        int[] diff = new int[cloudletNum];
        if(left_makespan < right_makespan){
            for(int i = 0 ; i < cloudletNum; i++){
                diff[i] = pre_solution[i] - xleft[i];
            }
        }else{
            for(int i = 0 ; i < cloudletNum; i++){
                diff[i] = pre_solution[i] - xright[i];
            }
        }
        for(int i = 0 ; i < cloudletNum; i++){
            if(rand.nextDouble() < p1){
                new_solution[i] = pre_solution[i] - diff[i];
            }
        }
        double pre_solution_makespan = calMakespan(pre_solution);
        double new_solution_makespan = calMakespan(new_solution);
        if(pre_solution_makespan < new_solution_makespan){
            new_solution = pre_solution;
            System.arraycopy(pre_solution, 0, new_solution, 0, pre_solution.length);
        }
        int[] temp = new int[cloudletNum];
        System.arraycopy(new_solution, 0, temp, 0, new_solution.length);
        if(rand.nextDouble() < p2){
            int start = rand.nextInt(cloudletNum);
            int end = rand.nextInt(cloudletNum);
            if(start > end){
                int tempp = start;
                start = end;
                end = tempp;
            }
            for(int i = start; i <= ((end -start)/2 + start); i++){
                int tempp = new_solution[i];
                new_solution[i] = new_solution[end - i + start];
                new_solution[end - i + start] = tempp;
            }
        }
        if(calMakespan(new_solution) > new_solution_makespan){
            System.arraycopy(temp, 0, new_solution, 0, pre_solution.length);
        }
        tabu.clear();;
        for(int i = 0 ; i < cloudletNum; i++){
            tabu.add(new_solution[i]);
        }
        calMakespan(new_solution);
    }
    private double calMakespan(int[] xleft) {
        ET = new double[vmNum];
        for(int i = 0; i < xleft.length; i++){
            this.aveWaitTime += ET[xleft[i]];
            ET[xleft[i]] += time[i][xleft[i]];
        }
        this.aveWaitTime /= (double) this.cloudletNum;
        return getMakespan();
    }

    public double getAveWaitTime() {
        return aveWaitTime;
    }

    public ArrayList<Integer> getTabu() {
        return tabu;
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

    public void setTask_list(ArrayList<Integer> task_list) {
        this.task_list = task_list;
    }

}
