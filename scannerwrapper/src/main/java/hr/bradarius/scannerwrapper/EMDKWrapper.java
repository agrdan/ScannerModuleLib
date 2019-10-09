package hr.bradarius.scannerwrapper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;

import java.util.ArrayList;

import hr.bradarius.scannerwrapper.callbacks.ScanResult;


public class EMDKWrapper implements EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener {

    private Context context;
    private static final String TAG = "wrapper";
    private boolean scannerInitialized = false;
    private boolean allowScanner = false;

    // Declare a variable to store EMDKManager object
    private EMDKManager emdkManager = null;

    // Declare a variable to store Barcode Manager object
    private BarcodeManager barcodeManager = null;

    // Declare a variable to hold scanner device to scan
    private Scanner scanner = null;

    ScanResult scannerCallback = null;

    public EMDKWrapper(Context context, ScanResult callback, boolean allowScanner) {
        this.context = context;
        scannerCallback = callback;
        this.allowScanner = allowScanner;
    }

    public void initialize() {
        // The EMDKManager object will be created and returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(context, this);
        // Check the return status of getEMDKManager and update the status Text
        // View accordingly
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            Toast.makeText(context, "EMDK failed!", Toast.LENGTH_SHORT).show();
        }



    }


    public boolean isScannerInitialized() {
        return scannerInitialized;
    }


    // Method to initialize and enable Scanner and its listeners
    private String initializeScanner() throws ScannerException {
        if(allowScanner) {
            if (scanner == null) {
                // Get the Barcode Manager object
                barcodeManager = (BarcodeManager) this.emdkManager
                        .getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
                // Get default scanner defined on the device
                scanner = barcodeManager.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT);
                // Add data and status listeners
                scanner.addDataListener(this);
                scanner.addStatusListener(this);
                // Hard trigger. When this mode is set, the user has to manually
                // press the trigger on the device after issuing the read call.
                scanner.triggerType = Scanner.TriggerType.HARD;
                // Enable the scanner
                scanner.enable();
                // Starts an asynchronous Scan. The method will not turn ON the
                // scanner. It will, however, put the scanner in a state in which
                // the scanner can be turned ON either by pressing a hardware
                // trigger or can be turned ON automatically.
                scanner.read();
                scannerInitialized = true;
                //Toast.makeText(context, "Scanner inizialized", Toast.LENGTH_SHORT).show();
                return "scanner initialized";
            }
            else {
                return null;
            }

        } else {
            //Toast.makeText(context, "Scanner not inizialized because normal phone is used!", Toast.LENGTH_LONG).show();
            return null;
        }
    }


    public void stopScanner() {
        try {
            if (scanner != null) {
                // releases the scanner hardware resources for other application
                // to use. You must call this as soon as you're done with the
                // scanning.
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);
                scanner.disable();
                scanner = null;
            }
        } catch (ScannerException e) {
            e.printStackTrace();
        }

        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }


    @Override
    public void onOpened(EMDKManager emdkManager) {
        Log.d(TAG, "Scanner opened!");
        this.emdkManager = emdkManager;

        try {
            // Call this method to enable Scanner and its listeners
            initializeScanner();
        } catch (ScannerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClosed() {
        Log.d(TAG, "Scanner closed!");
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        new AsyncDataUpdate().execute(scanDataCollection);
    }

    @Override
    public void onStatus(StatusData statusData) {
        new AsyncStatusUpdate().execute(statusData);
    }

    int dataLength = 0;

    public class AsyncDataUpdate extends AsyncTask<ScanDataCollection, Void, String> {
        @Override
        protected String doInBackground(ScanDataCollection... params) {
            // Status string that contains both barcode data and type of barcode
            // that is being scanned
            String statusStr = "";

            try {

                ScanDataCollection scanDataCollection = params[0];

                // The ScanDataCollection object gives scanning result and the
                // collection of ScanData. So check the data and its status
                if (scanDataCollection != null) {

                }
                else {

                }

                if (scanDataCollection.getResult() == ScannerResults.SUCCESS) {

                    ArrayList<ScanDataCollection.ScanData> scanData = scanDataCollection.getScanData();

                    // Iterate through scanned data and prepare the statusStr
                    for (ScanDataCollection.ScanData data : scanData) {
                        // Get the scanned data
                        String a = data.getData();

                        // Get the type of label being scanned
                        ScanDataCollection.LabelType labelType = data.getLabelType();
                        // Concatenate barcode data and label type

                        statusStr = a;// + " " + labelType;
                        Log.d(TAG, "get scanner result: " + a + ";" + statusStr);
                        //statusStr = a;
                    }
                }

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.d(TAG, "Error: " + e);
            }

            // Return result to populate on UI thread
            return statusStr;
        }

        @Override
        protected void onPostExecute(String result) {
            // Update the dataView EditText on UI thread with barcode data and
            // its label type
            boolean isOld = false;
            if (dataLength > 50) {

                dataLength = 0;
            }

            scannerCallback.onScanResult(result);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }

    }


    // AsyncTask that configures the current state of scanner on background
    // thread and updates the result on UI thread
    private class AsyncStatusUpdate extends AsyncTask<StatusData, Void, String> {

        @Override
        protected String doInBackground(StatusData... params) {
            String statusStr = "";
            // Get the current state of scanner in background
            StatusData statusData = params[0];
            StatusData.ScannerStates state = statusData.getState();
            // Different states of Scanner
            switch (state) {
                // Scanner is IDLE
                case IDLE:
                    statusStr = "The scanner enabled and its idle";
                    break;
                // Scanner is SCANNING
                case SCANNING:
                    statusStr = "Scanning..";
                    break;
                // Scanner is waiting for trigger press
                case WAITING:
                    statusStr = "Waiting for trigger press..";
                    break;
                // Scanner is not enabled
                case DISABLED:
                    statusStr = "Scanner is not enabled";
                    break;
                default:
                    break;
            }

            // Return result to populate on UI thread
            return statusStr;
        }

        @Override
        protected void onPostExecute(String result) {
            scannerCallback.onStatusChange(result);
            try {
                if (scanner != null) {
                    scanner.read();
                }
            } catch (ScannerException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }


    }
}
