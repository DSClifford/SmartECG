package edu.utk.biodynamics;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

public class QrsDetectService extends Service {
    static String TAG = "QrsDetectService";
    static double meanY = 0;
    static double meanY2 = 0;
    double ECG_HighPass[];
    double QRSIndexes[];
    double numBeats = 0;
    double beatsPerMin;

    public QrsDetectService() {
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {

        double ECG_Data_Orig[] = intent.getDoubleArrayExtra("ECG_Data");
        ECG_HighPass = highPass(ECG_Data_Orig);
        QRSIndexes = QRS(ECG_HighPass);
        double totalTime = ECG_Data_Orig.length/200;
        beatsPerMin = (numBeats/totalTime)*60;
        numBeats=0;
        broadcastUpdate(TAG);

        return START_NOT_STICKY;
    }


    public double[] QRS(double[] filteredSig) {
        double[] QRS = new double[filteredSig.length];

        double treshold = 0;

        for(int i=0; i<200; i++) {
            if(filteredSig[i] > treshold) {
                treshold = filteredSig[i];
            }
        }

        int frame = 100;

        for(int i=0; i<filteredSig.length; i+=frame) {
            double max = 0;
            double index = 0;
            if (i + frame > filteredSig.length) {
                index = filteredSig.length;
            } else {
                index = i + frame;
            }
            for (int j = i; j < index; j++) {
                if (filteredSig[j] > max) max = filteredSig[j];
            }
            boolean added = false;
            for (int j = i; j < index; j++) {
                if (filteredSig[j] > 11 * meanY2 && !added) {
                    QRS[j] = 11*meanY2;
                    added = true;
                } else {
                    QRS[j] = 0;
                }
            }

            double gama = (Math.random() > 0.5) ? 0.15 : 0.20;
            double alpha = 0.01 + (Math.random() * ((0.1 - 0.01)));

            treshold = alpha * gama * max + (1 - alpha) * treshold;

        }
        int index=0;
        for (int i = 0; i < QRS.length ; i++) {
            if (QRS[i]>0){
                numBeats++;
                if (index==0) {
                    index = i;
                }else if(i-index<50){
                    QRS[i]=0;
                    numBeats--;
                }
            }
        }
        return QRS;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static double[] highPass(double[] x) {
        int IndexNeg2;
        int IndexNeg1;
        int Index;
        int IndexPos1;
        int IndexPos2;
        double highPass[] = new double[x.length];
        double y[] = new double[x.length];
        double sumY = 0;
        double sumY2 = 0;

        for (int i = 0; i < x.length ; i++) {
            IndexNeg2 = i-2;
            if (IndexNeg2 < 0) {
                IndexNeg2 = x.length + IndexNeg2;
            }

            IndexNeg1 = i - 1;
            if (IndexNeg1 < 0) {
                IndexNeg1 = x.length + IndexNeg1;
            }

            Index = i;

            IndexPos1 = i+1;
            if (IndexPos1 >=x.length){
                IndexPos1 = IndexPos1-x.length;
            }

            IndexPos2 = i+2;
            if (IndexPos2 >= x.length) {
                IndexPos2 = IndexPos2-x.length;
            }

            y[i] = (x[IndexNeg2]+x[IndexNeg1]+x[Index]+x[IndexPos1]+x[IndexPos2])/5;
        }
        for (int i = 0; i <y.length ; i++) {
            sumY += y[i];
        }
        meanY = sumY / y.length;

        for (int i = 0; i <y.length ; i++) {
            highPass[i] = (((y[i] - meanY)*(y[i] - meanY))*((y[i] - meanY)*(y[i] - meanY)))/10000;
        }

        for (int i = 0; i <y.length ; i++) {
            sumY2 += highPass[i];
        }
        meanY2 = sumY2 / y.length;

        return highPass;
    }


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        intent.putExtra("qrsindexes", QRSIndexes);
        intent.putExtra("heartRate", beatsPerMin);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
