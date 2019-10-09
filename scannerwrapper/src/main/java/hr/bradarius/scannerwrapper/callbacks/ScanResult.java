package hr.bradarius.scannerwrapper.callbacks;

public interface ScanResult {

    void onScanResult(String result);
    void onStatusChange(String status);
}
