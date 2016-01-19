package edu.utk.biodynamics.DatabaseUtils;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Created by DSClifford on 10/1/2015.
 */
public class ECGRecord implements Serializable {
    private String id;
    private String upldDate;
    private String basePath;
    private double maxHR;

    public String getId() {
        return id;
    }
    public void setId(String string) {
        this.id = string;
    }
    public String getupDate() {
        // TODO Auto-generated method stub
        return upldDate;
    }
    public void setupDate(Timestamp tstamp) {
        // TODO Auto-generated method stub
        String upDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(tstamp);
        this.upldDate = upDate;
    }

    public double getMaxHR() {
        return maxHR;
    }

    public void setMaxHR(double maxHR) {
        this.maxHR = maxHR;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    }
