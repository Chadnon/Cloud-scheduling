package org.cloudbus.cloudsim.examples.myscheduler;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.examples.myscheduler.ACO;
import org.cloudbus.cloudsim.examples.myscheduler.BCGO;
import org.cloudbus.cloudsim.examples.myscheduler.GA;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MyDatacenterBroker extends DatacenterBroker {
    /**
     * Created a new DatacenterBroker object.
     *
     * @param name name to be associated with this entity (as required by {@link } class)
     * @throws Exception the exception
     * @pre name != null
     * @post $none
     */
    public MyDatacenterBroker(String name) throws Exception {
        super(name);
    }
    //蚁群算法
    public void bindCloudletToVmACO(List<Cloudlet> clist, List<Vm> vlist){
        ACO(clist, vlist);
    }
    //天牛群遗传算法
    public void bindCloudletToVmBCGO(List<Cloudlet> clist, List<Vm> vlist){
        BCGO(clist, vlist);
    }
    //遗传算法
    public void bindCloudletToVmGA(List<Cloudlet> clist, List<Vm> vlist){
        GA(clist, vlist);
    }
    //轮询算法
    public void bindCloudletToVmPolling(List<Cloudlet> clist, List<Vm> vlist) {
        int cloudletNum = clist.size();
        int vmNum = vlist.size();

        double[][] time = new double[cloudletNum][vmNum];
        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                time[i][j] = (double) clist.get(i).getCloudletLength() / vlist.get(j).getMips();
            }
        }
        double[] vmLoad = new double[vmNum];
        int[] vmTasks = new int[vmNum];
        int vmIndex = 0;
        for(int i = 0; i < cloudletNum; i++){
            clist.get(i).setVmId(vlist.get(vmIndex % vmNum).getId());
            vmIndex++;
        }

//        for(C c : clist){
//            System.out.print(c.getVmId() + " ");
//        }
        double aveWaitTime = 0;
        double[] makespan = new double[vmNum];
        for(int i = 0; i < cloudletNum; i++){
            int j = clist.get(i).getVmId();
            aveWaitTime += makespan[j];
            makespan[j] += time[i][j];
        }
        double max = Double.MIN_VALUE;
        for(double t : makespan){
            if(t > max){
                max = t;
            }
        }
        try{
            BufferedWriter out = new BufferedWriter(new FileWriter("poll_result.txt",true));
            out.write(max+"\t"+aveWaitTime / cloudletNum+"\n");
            out.close();
        }catch (Exception e){

        }
        System.out.println("轮询算法 finished!! 全局最佳完成时间 " + max + " 平均等待时间" + aveWaitTime / cloudletNum);
    }
    //随机算法
    public void bindCloudletToVmRand(List<Cloudlet> clist, List<Vm> vlist) {
        Random random = new Random();
        int cloudletNum = clist.size();
        int vmNum = vlist.size();

        double[][] time = new double[cloudletNum][vmNum];
        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                time[i][j] = (double) clist.get(i).getCloudletLength() / vlist.get(j).getMips();
            }
        }
        double[] vmLoad = new double[vmNum];
        int[] vmTasks = new int[vmNum];
        for(int i = 0; i < cloudletNum; i++){
            clist.get(i).setVmId(vlist.get(random.nextInt(vmNum)).getId());
        }

        double aveWaitTime = 0;
        double[] makespan = new double[vmNum];
        for(int i = 0; i < cloudletNum; i++){
            int j = clist.get(i).getVmId();
            aveWaitTime += makespan[j];
            makespan[j] += time[i][j];
        }
        double max = Double.MIN_VALUE;
        for(double t : makespan){
            if(t > max){
                max = t;
            }
        }
        try{
            BufferedWriter out = new BufferedWriter(new FileWriter("rand_result.txt",true));
            out.write(max+"\t"+aveWaitTime / cloudletNum+"\n");
            out.close();
        }catch (Exception e){

        }
        System.out.println("随机算法 finished!! 全局最佳完成时间 " + max+ " 平均等待时间" + aveWaitTime / cloudletNum);
    }
    //MaxMin算法
    public void bindCloudletToVmMax2Min(List<Cloudlet> clist, List<Vm> vlist) {
        int cloudletNum = clist.size();
        int vmNum = vlist.size();

        double[][] time = new double[cloudletNum][vmNum];
        //重新排列任务和虚拟机
        Collections.sort(clist, new CloudletComparator());
        Collections.sort(vlist, new VmComparator());

        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                time[i][j] = (double) clist.get(i).getCloudletLength() / vlist.get(j).getMips();
            }
        }
        double[] vmLoad = new double[vmNum];
        int[] vmTasks = new int[vmNum];
        double minLoad = 0;
        int idx = 0;   //记录当前任务最合适的虚拟机号
        //将行号为0的任务直接分配给列号最大的虚拟机
        vmLoad[vmNum - 1] = time[0][vmNum - 1];
        vmTasks[vmNum - 1] = 1;
        clist.get(0).setVmId(vlist.get(vmNum - 1).getId());

        for (int i = 1; i < cloudletNum; i++) {
            minLoad = vmLoad[vmNum - 1] + time[i][vmNum - 1];
            idx = vmNum - 1;
            for (int j = vmNum - 2; j >= 0; j--) {
                if (vmLoad[j] == 0) {
                    if (minLoad >= time[i][j]) {
                        idx = j;
                        break;
                    }
                }
                if (minLoad > time[i][j] + vmLoad[j]) {
                    minLoad = time[i][j] + vmLoad[j];
                    idx = j;
                } else if (minLoad == time[i][j] + vmLoad[j] && vmTasks[j] < vmTasks[idx]) {
                    idx = j;
                }
            }
            vmLoad[idx] += time[i][idx];
            vmTasks[idx]++;
            clist.get(i).setVmId(vlist.get(idx).getId());
        }

//        for(C c : clist){
//            System.out.print(c.getVmId() + " ");
//        }
        double aveWaitTime = 0;
        double[] makespan = new double[vmNum];
        for(int i = 0; i < cloudletNum; i++){
            int j = clist.get(i).getVmId();
            aveWaitTime += makespan[j];
            makespan[j] += time[i][j];
        }
        double max = Double.MIN_VALUE;
        for(double t : makespan){
            if(t > max){
                max = t;
            }
        }
        try{
            BufferedWriter out = new BufferedWriter(new FileWriter("maxmin_result.txt",true));
            out.write(max+"\t"+aveWaitTime / cloudletNum+"\n");
            out.close();
        }catch (Exception e){

        }
        System.out.println("Max-Min finished!! 全局最佳完成时间 " + max + " 平均等待时间" + aveWaitTime / cloudletNum);
    }
    //MinMin算法
    public void bindCloudletToVmMin2Min(List<Cloudlet> clist, List<Vm> vlist) {
        int cloudletNum = clist.size();
        int vmNum = vlist.size();

        double[][] time = new double[cloudletNum][vmNum];
        //重新排列任务和虚拟机
        Collections.sort(clist, new CloudletComparator2());
        Collections.sort(vlist, new VmComparator());

        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                time[i][j] = (double) clist.get(i).getCloudletLength() / vlist.get(j).getMips();
            }
        }
        double[] vmLoad = new double[vmNum];
        int[] vmTasks = new int[vmNum];
        double minLoad = 0;
        int idx = 0;   //记录当前任务最合适的虚拟机号
        //将行号为0的任务直接分配给列号最大的虚拟机
        vmLoad[vmNum - 1] = time[0][vmNum - 1];
        vmTasks[vmNum - 1] = 1;
        clist.get(0).setVmId(vlist.get(vmNum - 1).getId());

        for (int i = 1; i < cloudletNum; i++) {
            minLoad = vmLoad[vmNum - 1] + time[i][vmNum - 1];
            idx = vmNum - 1;
            for (int j = vmNum - 2; j >= 0; j--) {
                if (vmLoad[j] == 0) {
                    if (minLoad >= time[i][j]) {
                        idx = j;
                        break;
                    }
                }
                if (minLoad > time[i][j] + vmLoad[j]) {
                    minLoad = time[i][j] + vmLoad[j];
                    idx = j;
                } else if (minLoad == time[i][j] + vmLoad[j] && vmTasks[j] < vmTasks[idx]) {
                    idx = j;
                }
            }
            vmLoad[idx] += time[i][idx];
            vmTasks[idx]++;
            clist.get(i).setVmId(vlist.get(idx).getId());
        }

//        for(C c : clist){
//            System.out.print(c.getVmId() + " ");
//        }
        double aveWaitTime = 0;
        double[] makespan = new double[vmNum];
        for(int i = 0; i < cloudletNum; i++){
            int j = clist.get(i).getVmId();
            aveWaitTime += makespan[j];
            makespan[j] += time[i][j];
        }
        double max = Double.MIN_VALUE;
        for(double t : makespan){
            if(t > max){
                max = t;
            }
        }
        try{
            BufferedWriter out = new BufferedWriter(new FileWriter("minmin_result.txt",true));
            out.write(max+"\t"+aveWaitTime / cloudletNum+"\n");
            out.close();
        }catch (Exception e){

        }
        System.out.println("Min-Min finished!! 全局最佳完成时间 " + max + " 平均等待时间" + aveWaitTime / cloudletNum);
    }


    public void ACO(List<Cloudlet> clist, List<Vm> vlist) {
        ACO aco = new ACO(20, 30, 0.4, 1.5, 0.6, 20, 2, this);
        aco.init(clist, vlist);
        aco.solve();

    }
    public void BCGO(List<Cloudlet> clist, List<Vm> vlist) {

        BCGO dabs = new BCGO(20, 30, 0.4, 1.5, 0.5, 0.5, clist.size()/2, this);
        dabs.init(clist, vlist);
        dabs.solve();

    }

    private void GA(List<Cloudlet> clist, List<Vm> vlist){
        new GA(20 ,30,0.8,0.01, clist, vlist, this);
    }

    //根据指令长度降序排列任务
    private  class CloudletComparator implements Comparator<Cloudlet> {
        @Override
        public int compare(Cloudlet o1, Cloudlet o2) {
            return (int)(o2.getCloudletLength() - o1.getCloudletLength());
        }

    }
    //根据指令长度升序排列任务
    private  class CloudletComparator2 implements Comparator<Cloudlet> {
        @Override
        public int compare(Cloudlet o1, Cloudlet o2) {
            return (int)(o1.getCloudletLength() - o2.getCloudletLength());
        }
    }
    //根据MIPS升序排列VM
    private  class VmComparator implements Comparator<Vm>{

        @Override
        public int compare(Vm o1, Vm o2) {
            return (int)(o1.getMips() - o2.getMips());
        }
    }
}
