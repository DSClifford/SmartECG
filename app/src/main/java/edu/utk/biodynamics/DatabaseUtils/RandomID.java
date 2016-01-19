package edu.utk.biodynamics.DatabaseUtils;

import java.util.Calendar;

/**
 * Created by DSClifford on 8/12/2015.
 */
public class RandomID {

    public static String generateString()
    {

        Calendar calendar = Calendar.getInstance();
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        String month = String.valueOf(calendar.get(Calendar.MONTH));
        String day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));

        int secInt = calendar.get(Calendar.SECOND);
        if(secInt >= 30){secInt = 30;}else{secInt=0;}
        String sec = String.format("%02d", secInt);
        String hour = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY));
        String minute = String.format("%02d", calendar.get(Calendar.MINUTE));
        String text = year+"_"+month+"_"+day+"_"+hour+"_"+minute+"_"+sec;
        return new String(text);
    }

}
