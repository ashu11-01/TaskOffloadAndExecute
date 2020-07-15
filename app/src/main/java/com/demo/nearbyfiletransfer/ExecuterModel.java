package com.demo.nearbyfiletransfer;

import androidx.annotation.NonNull;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class ExecuterModel implements Comparable<ExecuterModel> {

    private String codename;
    private String rating;
    private int status;
    private String serviceType;
    private String timestamp;
    private String battery;
    private String RAM;
    private String Cpu;
    private String storage;
    private String endpointId;

    public double getUtility() {
        return utility;
    }

    public void setUtility(double utility) {
        this.utility = utility;
    }

    private double utility;
    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public ExecuterModel() {}
    public ExecuterModel(String codename, String rating, String serviceType, String timestamp, String battery, String RAM, String cpu, String storage) {
        this.codename = codename;
        this.rating = rating;
        this.serviceType = serviceType;
        this.timestamp = timestamp;
        this.battery = battery;
        this.RAM = RAM;
        Cpu = cpu;
        this.storage = storage;
    }

    public String getCodename() {
        return codename;
    }

    public void setCodename(String codename) {
        this.codename = codename;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    public String getRAM() {
        return RAM;
    }

    public void setRAM(String RAM) {
        this.RAM = RAM;
    }

    public String getCpu() {
        return Cpu;
    }

    public void setCpu(String cpu) {
        Cpu = cpu;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    @NonNull
    @Override
    public String toString() {
        super.toString();
        return this.codename;
    }

    @Override
    public int compareTo(ExecuterModel executerModel) {
        return Double.compare(this.getUtility(),executerModel.getUtility());
    }
    private class CompareByUtility implements Comparator<ExecuterModel>{

        @Override
        public int compare(ExecuterModel executerModel, ExecuterModel t1) {
            return Double.compare(executerModel.getUtility(),t1.getUtility());
        }

        @Override
        public Comparator<ExecuterModel> reversed() {
            return null;
        }

        @Override
        public Comparator<ExecuterModel> thenComparing(Comparator<? super ExecuterModel> other) {
            return null;
        }

        @Override
        public <U> Comparator<ExecuterModel> thenComparing(Function<? super ExecuterModel, ? extends U> keyExtractor, Comparator<? super U> keyComparator) {
            return null;
        }

        @Override
        public <U extends Comparable<? super U>> Comparator<ExecuterModel> thenComparing(Function<? super ExecuterModel, ? extends U> keyExtractor) {
            return null;
        }

        @Override
        public Comparator<ExecuterModel> thenComparingInt(ToIntFunction<? super ExecuterModel> keyExtractor) {
            return null;
        }

        @Override
        public Comparator<ExecuterModel> thenComparingLong(ToLongFunction<? super ExecuterModel> keyExtractor) {
            return null;
        }

        @Override
        public Comparator<ExecuterModel> thenComparingDouble(ToDoubleFunction<? super ExecuterModel> keyExtractor) {
            return null;
        }
    }
}
